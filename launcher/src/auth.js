'use strict';
const { Auth } = require('msmc');
const store = require('./store');

let session = null; // { profile, mclc, expires }

const MESSAGES = {
  'error.gui.closed': 'Sign-in window was closed.',
  'error.auth.microsoft': 'Microsoft sign-in failed. Try again.',
  'error.auth.xboxLive': 'Could not sign in to Xbox Live with this account.',
  'error.auth.xsts': 'Xbox Live rejected the sign-in.',
  'error.auth.xsts.userNotFound': 'This Microsoft account has no Xbox profile. Sign in once at xbox.com to create one, then try again.',
  'error.auth.xsts.bannedCountry': 'Xbox Live is not available in this account\'s country.',
  'error.auth.xsts.child': 'This is a child account. An adult must add it to a Microsoft family group first.',
  'error.auth.xsts.child.SK': 'This account needs parental consent on the Xbox site before it can sign in.',
  'error.auth.minecraft': 'Minecraft sign-in failed.',
  'error.auth.minecraft.login': 'Minecraft\'s servers rejected the Xbox sign-in.',
  'error.auth.minecraft.profile': 'This account does not own Minecraft: Java Edition (or has never created a Java profile — launch the official launcher once).',
  'error.auth.minecraft.entitlements': 'Could not check which Minecraft games this account owns.',
};

/** msmc throws plain strings or { ts, response } objects, not Errors. Turn them into readable Errors. */
async function toError(e) {
  if (e instanceof Error) return e;
  const code = typeof e === 'string' ? e : e && e.ts ? e.ts : null;
  let msg = code ? MESSAGES[code] || MESSAGES[code.split('.').slice(0, -1).join('.')] || code : 'Unknown sign-in error';
  if (e && e.response) {
    let body = '';
    try { body = (await e.response.text()).slice(0, 300); } catch { /* ignore */ }
    msg += ` (HTTP ${e.response.status}${body ? ': ' + body : ''})`;
  }
  const err = new Error(msg);
  err.code = code;
  return err;
}

function publicAccount() {
  const acc = session ? { profile: session.profile } : store.loadAccount();
  return acc && acc.profile ? { id: acc.profile.id, name: acc.profile.name } : null;
}

const crypto = require('crypto');
const MCS = 'https://api.minecraftservices.com';

async function gamertag(xbox) {
  try { return (await (await xbox.getSocial()).getProfile()).gamerTag; } catch { return null; }
}

async function readBody(res) {
  try { return (await res.text()).slice(0, 240); } catch { return ''; }
}

/**
 * Minecraft sign-in, done directly instead of msmc.getMinecraft(): msmc reports *any*
 * profile failure as "doesn't own Minecraft". This tells the real reason apart.
 */
async function minecraftLogin(xbox) {
  const identityToken = await xbox.xAuth('rp://api.minecraftservices.com/');
  let loginRes;
  for (let attempt = 0; attempt < 3; attempt++) {
    loginRes = await fetch(`${MCS}/authentication/login_with_xbox`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ identityToken }),
    });
    if (loginRes.ok || (loginRes.status !== 429 && loginRes.status < 500)) break;
    const after = Number(loginRes.headers && loginRes.headers.get && loginRes.headers.get('retry-after'));
    await new Promise((r) => setTimeout(r, Math.min(15000, (after > 0 ? after * 1000 : 3000 * (attempt + 1)))));
  }
  if (loginRes.status === 429) {
    throw new Error("Minecraft is limiting sign-ins right now because there were too many in a short time. Wait about 10–15 minutes, then try again. (You'll stay signed in after that, so this shouldn't come back.)");
  }
  if (!loginRes.ok) {
    const body = await readBody(loginRes);
    throw new Error(`Minecraft's login server refused the Xbox sign-in (HTTP ${loginRes.status}${body ? ': ' + body : ''}).`);
  }
  const login = await loginRes.json();
  const auth = { Authorization: `Bearer ${login.access_token}`, Accept: 'application/json' };

  let profileRes;
  for (let attempt = 0; attempt < 3; attempt++) {
    profileRes = await fetch(`${MCS}/minecraft/profile`, { headers: auth });
    if (profileRes.ok || (profileRes.status !== 429 && profileRes.status < 500)) break;
    await new Promise((r) => setTimeout(r, 1200 * (attempt + 1)));
  }
  if (profileRes.ok) {
    const profile = await profileRes.json();
    if (profile && profile.id && profile.name) {
      return {
        profile: { id: profile.id, name: profile.name },
        mclc: {
          access_token: login.access_token,
          client_token: crypto.randomUUID(),
          uuid: profile.id,
          name: profile.name,
          user_properties: '{}',
          meta: { type: 'msa', demo: false },
        },
        expiresIn: (login.expires_in || 86400) * 1000,
      };
    }
  }

  // No profile. Work out why.
  const status = profileRes.status;
  const tag = await gamertag(xbox);
  const who = tag ? `You signed in as Xbox account "${tag}". ` : '';
  let items = [];
  try {
    const ent = await fetch(`${MCS}/entitlements/mcstore`, { headers: auth });
    if (ent.ok) items = ((await ent.json()).items || []).map((i) => i.name);
  } catch { /* ignore */ }
  const ownsJava = items.some((n) => n === 'game_minecraft' || n === 'product_minecraft');
  const gamePass = items.some((n) => /game_pass/i.test(n));

  if (status === 404 && (ownsJava || gamePass)) {
    throw new Error(`${who}This account has Minecraft but no Java username yet. Open the official Minecraft Launcher once, pick your Java Edition name, then sign in here again.`);
  }
  if (status === 404) {
    throw new Error(`${who}Minecraft's servers found no Java Edition profile on this account. If you own the game on a different Microsoft account, click Sign in again and choose that one ("Use another account").`);
  }
  const body = await readBody(profileRes);
  throw new Error(`${who}Could not load your Minecraft profile (HTTP ${status}${body ? ': ' + body : ''}).`);
}

async function finish(xbox) {
  const mc = await minecraftLogin(xbox);
  // Minecraft tokens last ~24h; keep a 30 min safety margin and remember it across restarts.
  session = { profile: mc.profile, mclc: mc.mclc, expires: Date.now() + Math.max(5 * 60 * 1000, mc.expiresIn - 30 * 60 * 1000) };
  store.saveAccount({ profile: session.profile, refresh: xbox.save(), mc: { mclc: session.mclc, expires: session.expires } });
  return publicAccount();
}

async function login() {
  try {
    const auth = new Auth('select_account');
    const xbox = await auth.launch('electron', { width: 520, height: 680, resizable: false, title: 'Sign in to Flower Client' });
    return await finish(xbox);
  } catch (e) {
    throw await toError(e);
  }
}

/** Returns MCLC authorization, refreshing the saved Microsoft token if needed. */
async function authorization() {
  if (session && session.expires > Date.now()) return session.mclc;
  const cached = store.loadAccount();
  if (cached && cached.mc && cached.mc.expires > Date.now() && cached.profile) {
    session = { profile: cached.profile, mclc: cached.mc.mclc, expires: cached.mc.expires };
    return session.mclc;
  }
  const saved = store.loadAccount();
  if (!saved || !saved.refresh) throw new Error('Sign in with Microsoft first.');
  try {
    const xbox = await new Auth('select_account').refresh(saved.refresh);
    await finish(xbox);
    return session.mclc;
  } catch (e) {
    const err = await toError(e);
    if (/limiting sign-ins/.test(err.message)) throw err;
    throw new Error('Your sign-in expired. Please sign in again. (' + err.message + ')');
  }
}

function logout() { session = null; store.clearAccount(); }

module.exports = { login, logout, authorization, publicAccount };
