'use strict';

const api = window.flower;
const $ = (s) => document.querySelector(s);
const $$ = (s) => [...document.querySelectorAll(s)];
const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

const state = { settings: null, account: null, modules: [], perf: {}, info: null, busy: false, page: 'home' };
const bloom = new window.FlowerBloom($('#bloom'));

/* ---------------- placement of the flower per page ---------------- */
const ANCHORS = { browse: [0.66, 0.4, 0.26], home: [0.7, 0.42, 0.22], mods: [0.66, 0.4, 0.26], settings: [0.66, 0.4, 0.26], log: [0.7, 0.4, 0.24] };
function placeFlower(page, dur) {
  const [x, y, s] = ANCHORS[page] || ANCHORS.home;
  bloom.moveTo(x, y, s, dur);
}

/* ---------------- toasts ---------------- */
function toast(msg, kind = 'info', ms = 5200) {
  const el = document.createElement('div');
  el.className = 'toast ' + kind;
  el.textContent = msg;
  $('#toasts').appendChild(el);
  setTimeout(() => { el.classList.add('out'); setTimeout(() => el.remove(), 300); }, ms);
}

/* ---------------- navigation ---------------- */
function go(page) {
  state.page = page;
  if (page === 'browse' && !mr.loaded && !mr.reqId) mrLoad(true);
  $$('.nav').forEach((b) => b.classList.toggle('active', b.dataset.page === page));
  $$('.page').forEach((p) => p.classList.toggle('active', p.id === 'page-' + page));
  placeFlower(page, 900);
}
$$('.nav').forEach((b) => b.addEventListener('click', () => go(b.dataset.page)));

/* ---------------- window controls ---------------- */
$$('[data-win]').forEach((b) => b.addEventListener('click', () => api.window[b.dataset.win]()));

/* ---------------- account ---------------- */
function renderAccount() {
  const box = $('#account');
  const a = state.account;
  if (a) {
    box.innerHTML = `<div class="acct-chip"><img alt="" src="https://mc-heads.net/avatar/${esc(a.id)}/52" /><b>${esc(a.name)}</b><button id="logout">Sign out</button></div>`;
    const img = box.querySelector('img');
    img.addEventListener('error', () => { img.removeAttribute('src'); });
    $('#logout').addEventListener('click', async () => {
      await api.account.logout();
      state.account = null;
      renderAccount();
      toast('Signed out.');
    });
  } else {
    box.innerHTML = `<button class="ms-btn" id="login"><span class="ms-logo"><i style="background:#f25022"></i><i style="background:#7fba00"></i><i style="background:#00a4ef"></i><i style="background:#ffb900"></i></span>Sign in with Microsoft</button>`;
    $('#login').addEventListener('click', login);
  }
  renderPlay();
}

async function login() {
  try {
    setStatus('Waiting for Microsoft sign-in…');
    state.account = await api.account.login();
    renderAccount();
    setStatus('Ready');
    toast(`Signed in as ${state.account.name}.`);
  } catch (e) {
    setStatus('Ready');
    toast(e.message, 'error');
  }
}

/* ---------------- play ---------------- */
function renderPlay() {
  const btn = $('#play');
  btn.classList.toggle('signin', !state.account && !state.busy);
  btn.disabled = state.busy;
  btn.querySelector('.play-label').textContent = state.busy ? 'Launching' : state.account ? 'Play' : 'Sign in';
  btn.querySelector('.play-sub').textContent = state.account || state.busy ? `${state.info ? state.info.mc : '1.21.1'} · Fabric` : 'Microsoft';
}

function setStatus(text, pct) {
  $('#status-text').textContent = text;
  if (typeof pct === 'number') $('#bar').style.width = Math.round(pct * 100) + '%';
}

function setBusy(b) {
  state.busy = b;
  document.body.classList.toggle('busy', b);
  renderPlay();
}

$('#play').addEventListener('click', async () => {
  if (state.busy) return;
  if (!state.account) return login();
  setBusy(true);
  setStatus('Starting', 0.01);
  bloom.scatter();
  appendLog('— Launch ' + new Date().toLocaleTimeString() + ' —');
  try {
    await api.game.launch();
  } catch (e) {
    setBusy(false);
    setStatus('Ready', 0);
    toast(e.message, 'error', 8000);
    appendLog('[error] ' + e.message);
    regrow();
    if (/sign in/i.test(e.message)) { state.account = await api.account.get().catch(() => null); renderAccount(); }
  }
});

function regrow() {
  if (bloom.state === 'gone' || bloom.state === 'scattering') {
    const wait = bloom.state === 'scattering' ? 2600 : 200;
    bloom.bloomDur = 3200; // regrowth is quicker than the first bloom
    setTimeout(() => bloom.bloom(), wait);
  }
}

api.game.onProgress(({ pct, text }) => setStatus(text, pct));
api.game.onState((s) => {
  if (s === 'starting') setStatus('Starting Minecraft…', 0.97);
  if (s === 'running') { setStatus('Playing', 1); $('.play-label').textContent = 'Playing'; }
  if (s === 'closed') {
    setBusy(false);
    setStatus('Ready', 0);
    regrow();
  }
});
api.game.onError((m) => toast(m, 'error', 9000));

/* ---------------- log ---------------- */
const logEl = $('#log');
let logLines = 0;
function appendLog(line) {
  const atBottom = logEl.scrollTop + logEl.clientHeight >= logEl.scrollHeight - 30;
  const span = document.createElement('span');
  if (/\b(WARN|warn)\b|\[warn\]/.test(line)) span.className = 'warn';
  if (/\b(ERROR|FATAL|Exception)\b|\[error\]/.test(line)) span.className = 'err';
  span.textContent = line + '\n';
  logEl.appendChild(span);
  if (++logLines > 4000) { logEl.firstChild.remove(); logLines--; }
  if (atBottom) logEl.scrollTop = logEl.scrollHeight;
}
api.game.onLog(appendLog);
$('#copy-log').addEventListener('click', async () => { await navigator.clipboard.writeText(logEl.textContent); toast('Log copied.'); });
$('#clear-log').addEventListener('click', () => { logEl.textContent = ''; logLines = 0; });

/* ---------------- mods ---------------- */
function rowItem({ name, tag, desc, checked, onChange }) {
  const row = document.createElement('label');
  row.className = 'row-item';
  row.innerHTML = `<div class="ri-text"><div class="ri-name">${esc(name)}${tag ? `<span class="ri-tag">${esc(tag)}</span>` : ''}</div><div class="ri-desc">${esc(desc)}</div></div><span class="switch"><input type="checkbox" ${checked ? 'checked' : ''}/><span></span></span>`;
  row.querySelector('input').addEventListener('change', (e) => onChange(e.target.checked, e.target));
  return row;
}

function renderMods() {
  const list = $('#module-list');
  list.innerHTML = '';
  state.modules.forEach((m) => {
    list.appendChild(rowItem({
      name: m.name, tag: m.category, desc: m.desc, checked: m.enabled,
      onChange: async (on, input) => {
        try { state.modules = await api.modules.set(m.name, on); renderFacts(); }
        catch (e) { input.checked = !on; toast(e.message, 'error'); }
      },
    }));
  });
  $('#mod-count').textContent = `${state.modules.filter((m) => m.enabled).length} of ${state.modules.length} on`;

  const perf = $('#perf-list');
  perf.innerHTML = '';
  Object.entries(state.perf).forEach(([slug, p]) => {
    perf.appendChild(rowItem({
      name: p.name, desc: p.desc, checked: !!state.settings.performanceMods[slug],
      onChange: async (on) => {
        state.settings = await api.settings.set({ performanceMods: { [slug]: on } });
        renderFacts();
      },
    }));
  });
}

/* ---------------- modrinth ---------------- */
const mr = { view: 'search', sort: 'relevance', q: '', offset: 0, total: 0, hits: [], busy: new Map(), installed: {}, loaded: false, reqId: 0 };

function fmtNum(n) {
  if (n >= 1e6) return (n / 1e6).toFixed(n >= 1e7 ? 0 : 1) + 'M';
  if (n >= 1e3) return (n / 1e3).toFixed(n >= 1e4 ? 0 : 1) + 'K';
  return String(n);
}

function mrIcon(url, title) {
  return url
    ? `<img class="mr-icon" alt="" loading="lazy" src="${esc(url)}" />`
    : `<div class="mr-icon ph">${esc((title || '?').trim().charAt(0).toUpperCase())}</div>`;
}

function mrButton(id, installedNow, managed) {
  if (managed) return `<span class="mr-tag">Managed in Mods</span>`;
  const b = mr.busy.get(id);
  if (b) return `<button class="mr-btn busy" disabled><i style="--p:${Math.round((b.pct || 0) * 100)}%"></i>${b.label}</button>`;
  return installedNow
    ? `<button class="mr-btn done" data-remove="${esc(id)}"><span class="a">Installed</span><span class="b">Remove</span></button>`
    : `<button class="mr-btn" data-install="${esc(id)}">Install</button>`;
}

function renderModrinth() {
  const list = $('#mr-list');
  $('#mr-count').textContent = Object.keys(mr.installed).length || '';
  $('#mr-bar').hidden = mr.view !== 'search';
  $$('#mr-view button').forEach((b) => b.setAttribute('aria-checked', String(b.dataset.view === mr.view)));
  $$('#mr-sort button').forEach((b) => b.setAttribute('aria-checked', String(b.dataset.sort === mr.sort)));

  if (mr.view === 'installed') {
    const entries = Object.entries(mr.installed);
    $('#mr-more').hidden = true;
    $('#mr-status').textContent = entries.length ? 'Changes apply next time you press Play.' : '';
    list.innerHTML = entries.length
      ? entries.map(([id, e]) => `<div class="mr-item">${mrIcon(e.icon, e.title)}<div class="mr-text"><div class="mr-title">${esc(e.title)}<span class="mr-by mono">${esc(e.version || '')}</span></div><div class="mr-desc">${e.dependency ? 'Installed as a dependency' : esc(e.filename)}</div></div>${mrButton(id, true, false)}</div>`).join('')
      : `<div class="mr-empty"><b>Nothing installed from Modrinth yet.</b><span>Find something in Discover.</span></div>`;
    return;
  }
  list.innerHTML = mr.hits.length
    ? mr.hits.map((h) => `<div class="mr-item">${mrIcon(h.icon, h.title)}<div class="mr-text"><div class="mr-title">${esc(h.title)}<span class="mr-by">by ${esc(h.author)}</span></div><div class="mr-desc">${esc(h.description)}</div><div class="mr-meta mono"><span>↓ ${fmtNum(h.downloads)}</span><span>♥ ${fmtNum(h.follows)}</span>${h.categories.map((c) => `<span class="mr-cat">${esc(c)}</span>`).join('')}</div></div>${mrButton(h.id, !!mr.installed[h.id], h.managed)}</div>`).join('')
    : mr.loaded ? `<div class="mr-empty"><b>No mods found.</b><span>Try a different search.</span></div>` : skeletonRows();
  $('#mr-more').hidden = mr.hits.length >= mr.total || !mr.hits.length;
  $('#mr-status').textContent = mr.loaded && mr.hits.length ? `${mr.hits.length} of ${fmtNum(mr.total)}` : '';
}

function skeletonRows() {
  return Array.from({ length: 6 }, () => `<div class="mr-item sk"><div class="mr-icon"></div><div class="mr-text"><i></i><i></i></div></div>`).join('');
}

async function mrLoad(reset) {
  const id = ++mr.reqId;
  if (reset) { mr.offset = 0; mr.hits = []; mr.loaded = false; renderModrinth(); }
  try {
    const [res, inst] = await Promise.all([
      api.modrinth.search({ query: mr.q, sort: mr.sort, offset: mr.offset, limit: 20 }),
      api.modrinth.installed(),
    ]);
    if (id !== mr.reqId) return; // a newer search superseded this one
    mr.installed = inst;
    mr.total = res.total;
    mr.hits = reset ? res.hits : mr.hits.concat(res.hits);
    mr.offset = mr.hits.length;
    mr.loaded = true;
    renderModrinth();
  } catch (e) {
    if (id !== mr.reqId) return;
    mr.loaded = true;
    renderModrinth();
    $('#mr-status').textContent = 'Could not reach Modrinth. Check your connection.';
    toast(e.message, 'error');
  }
}

async function mrRefreshInstalled() {
  try { mr.installed = await api.modrinth.installed(); } catch { /* ignore */ }
  renderModrinth();
}

let mrTimer;
$('#mr-q').addEventListener('input', (e) => {
  clearTimeout(mrTimer);
  mrTimer = setTimeout(() => { mr.q = e.target.value.trim(); mrLoad(true); }, 320);
});
$('#mr-sort').addEventListener('click', (e) => {
  const b = e.target.closest('button'); if (!b) return;
  mr.sort = b.dataset.sort; mrLoad(true);
});
$('#mr-view').addEventListener('click', (e) => {
  const b = e.target.closest('button'); if (!b) return;
  mr.view = b.dataset.view;
  if (mr.view === 'installed') mrRefreshInstalled(); else renderModrinth();
});
$('#mr-more').addEventListener('click', () => mrLoad(false));
$('#mr-list').addEventListener('error', (e) => {
  if (e.target.tagName === 'IMG') e.target.replaceWith(Object.assign(document.createElement('div'), { className: 'mr-icon ph', textContent: '•' }));
}, true);
$('#mr-list').addEventListener('click', async (e) => {
  const ins = e.target.closest('[data-install]');
  const rem = e.target.closest('[data-remove]');
  if (ins) {
    const id = ins.dataset.install;
    mr.busy.set(id, { label: 'Installing', pct: 0 });
    renderModrinth();
    try {
      const added = await api.modrinth.install(id);
      toast(added.length > 1 ? `Installed ${added[added.length - 1]} + ${added.length - 1} dependenc${added.length === 2 ? 'y' : 'ies'}.` : `Installed ${added[0] || 'mod'}.`);
    } catch (err) { toast(err.message, 'error', 7000); }
    mr.busy.delete(id);
    mrRefreshInstalled();
  } else if (rem) {
    const id = rem.dataset.remove;
    mr.busy.set(id, { label: 'Removing', pct: 1 });
    renderModrinth();
    try { await api.modrinth.uninstall(id); } catch (err) { toast(err.message, 'error'); }
    mr.busy.delete(id);
    mrRefreshInstalled();
  }
});
api.modrinth.onProgress(({ pct }) => {
  for (const b of mr.busy.values()) if (b.label === 'Installing') b.pct = pct;
  $$('#mr-list .mr-btn.busy i').forEach((el) => el.style.setProperty('--p', Math.round(pct * 100) + '%'));
});

/* ---------------- settings ---------------- */
function fmtGb(mb) { return (mb / 1024).toFixed(mb % 1024 ? 1 : 0) + ' GB'; }

function renderSettings() {
  const s = state.settings;
  const ram = $('#ram');
  ram.value = s.ramMb;
  const fill = () => {
    ram.style.setProperty('--fill', ((ram.value - ram.min) / (ram.max - ram.min)) * 100 + '%');
    $('#ram-out').textContent = fmtGb(Number(ram.value));
  };
  fill();
  ram.oninput = fill;
  ram.onchange = () => save({ ramMb: Number(ram.value) });

  $('#width').value = s.width;
  $('#height').value = s.height;
  $('#fullscreen').checked = !!s.fullscreen;
  $('#gameDir').value = s.gameDir;
  $('#javaPath').value = s.javaPath || '';
  $('#jvmArgs').value = s.jvmArgs || '';
  $('#closeOnLaunch').checked = !!s.closeOnLaunch;
  renderThemes();
}

function renderThemes() {
  const box = $('#bloom-themes');
  const current = state.settings.bloomTheme || 'auto';
  const autoKey = window.FlowerBloom.seasonFor();
  const opts = [['auto', `Auto · ${window.FlowerBloom.THEMES[autoKey]}`], ...Object.entries(window.FlowerBloom.THEMES)];
  box.innerHTML = opts.map(([k, label]) => `<button type="button" role="radio" aria-checked="${k === current}" data-theme="${k}">${esc(label)}</button>`).join('');
  box.querySelectorAll('button').forEach((b) => b.addEventListener('click', async () => {
    await save({ bloomTheme: b.dataset.theme });
    renderThemes();
    bloom.setTheme(b.dataset.theme);
    bloom.bloomDur = 3200;
    bloom.bloom();
  }));
}

async function save(patch, quiet) {
  try {
    state.settings = await api.settings.set(patch);
    renderFacts();
  } catch (e) {
    if (!quiet) toast(e.message, 'error');
  }
}

$('#width').addEventListener('change', (e) => save({ width: Number(e.target.value) }));
$('#height').addEventListener('change', (e) => save({ height: Number(e.target.value) }));
$('#fullscreen').addEventListener('change', (e) => save({ fullscreen: e.target.checked }));
$('#jvmArgs').addEventListener('change', (e) => save({ jvmArgs: e.target.value.trim() }));
$('#closeOnLaunch').addEventListener('change', (e) => save({ closeOnLaunch: e.target.checked }));
$('#browse-dir').addEventListener('click', async () => {
  const dir = await api.settings.pickFolder();
  if (dir) { await save({ gameDir: dir }); renderSettings(); state.modules = await api.modules.get(); renderMods(); }
});
$('#open-dir').addEventListener('click', () => api.settings.openGameDir().catch((e) => toast(e.message, 'error')));
$('#browse-java').addEventListener('click', async () => {
  const p = await api.settings.pickJava();
  if (p) { await save({ javaPath: p }); renderSettings(); }
});
$('#clear-java').addEventListener('click', async () => { await save({ javaPath: '' }); renderSettings(); });

/* ---------------- home facts ---------------- */
function renderFacts() {
  if (!state.settings) return;
  const on = state.modules.filter((m) => m.enabled).length;
  const perf = Object.entries(state.perf).filter(([k]) => state.settings.performanceMods[k]).map(([, p]) => p.name);
  const items = [
    ['Modules', `${on} active`],
    ['Performance', perf.length ? perf.slice(0, 2).join(', ') + (perf.length > 2 ? ` +${perf.length - 2}` : '') : 'Off'],
    ['Memory', fmtGb(state.settings.ramMb)],
  ];
  $('#facts').innerHTML = items.map(([k, v]) => `<div><dt>${esc(k)}</dt><dd>${esc(v)}</dd></div>`).join('');
}

/* ---------------- boot ---------------- */
async function boot() {
  try {
    const [info, settings, account, modules, perf] = await Promise.all([
      api.app.info(), api.settings.get(), api.account.get(), api.modules.get(), api.modules.performance(),
    ]);
    Object.assign(state, { info, settings, account, modules, perf });
    bloom.setTheme(settings.bloomTheme || 'auto');
    $('#build-tag').textContent = 'v' + info.version;
    $('#mc-version').textContent = info.mc;
    $('#mr-version').textContent = info.mc;
    renderAccount();
    renderMods();
    renderSettings();
    renderFacts();
    if (!info.flowerBundled) appendLog('[warn] Flower Client mod jar not bundled in this build. See README → Building.');
  } catch (e) {
    toast('Could not load settings: ' + e.message, 'error');
  }
}

/* intro: seed → bloom in the centre → glide to its spot as the UI fades in */
bloom.anchor = bloom.anchorFrom = bloom.anchorTo = { x: 0.5, y: 0.4, size: 0.25 };
let revealed = false;
function reveal() {
  if (revealed) return;
  revealed = true;
  placeFlower(state.page, 1900);
  setTimeout(() => document.body.classList.remove('intro'), 500);
}
bloom.onBloomed = reveal;
setTimeout(() => bloom.bloom(), 1100);
setTimeout(reveal, 8500); // safety net
document.addEventListener('keydown', (e) => { if (e.key === 'Escape' || e.key === 'Enter') reveal(); });

boot();
$('#settings').addEventListener('submit', (e) => e.preventDefault());
