'use strict';
const fs = require('fs');
const { Client } = require('minecraft-launcher-core');
const store = require('./store');
const auth = require('./auth');
const { ensureJava } = require('./java');
const { ensureFabric } = require('./fabric');
const { syncMods } = require('./mods');

const MC_VERSION = '1.21.1';
let running = null;

/** Weighted stages so the progress bar moves smoothly across the whole launch. */
const STAGES = { auth: [0, 0.04], java: [0.04, 0.2], fabric: [0.2, 0.24], mods: [0.24, 0.36], game: [0.36, 1] };

async function launch(emit) {
  if (running) throw new Error('Minecraft is already running.');
  const s = store.load();
  const progress = (stage, p, text) => {
    const [a, b] = STAGES[stage];
    emit('progress', { pct: a + (b - a) * Math.max(0, Math.min(1, p)), text });
  };

  progress('auth', 0, 'Signing in');
  const authorization = await auth.authorization();
  progress('auth', 1, 'Signed in');

  await fs.promises.mkdir(s.gameDir, { recursive: true });
  const javaPath = await ensureJava(s.javaPath, progress);
  const custom = await ensureFabric(s.gameDir, MC_VERSION, progress);
  const warnings = await syncMods(s.gameDir, MC_VERSION, s.performanceMods, progress);
  warnings.forEach((w) => emit('log', '[warn] ' + w));

  const launcher = new Client();
  running = launcher;
  let lastPct = 0;

  launcher.on('debug', (m) => emit('log', String(m)));
  launcher.on('data', (m) => emit('log', String(m).trimEnd()));
  launcher.on('progress', (e) => {
    if (!e || !e.total) return;
    const p = e.task / e.total;
    const kind = { assets: 'Downloading assets', natives: 'Preparing natives', classes: 'Downloading libraries', 'assets-copy': 'Copying assets' }[e.type] || 'Downloading game files';
    lastPct = Math.max(lastPct, p * 0.95);
    progress('game', lastPct, `${kind}  ${e.task}/${e.total}`);
  });
  launcher.on('download-status', () => {});

  let started = false;
  launcher.on('data', () => {
    if (!started) { started = true; progress('game', 1, 'Game running'); emit('state', 'running'); }
  });
  launcher.on('close', (code) => {
    running = null;
    emit('state', 'closed');
    if (code && code !== 0 && !started) emit('error', `Minecraft exited with code ${code}. Check the log for details.`);
  });

  progress('game', 0, 'Preparing Minecraft ' + MC_VERSION);
  const proc = await launcher.launch({
    authorization,
    root: s.gameDir,
    version: { number: MC_VERSION, type: 'release', custom },
    memory: { max: `${s.ramMb}M`, min: `${Math.min(1024, s.ramMb)}M` },
    javaPath,
    customArgs: String(s.jvmArgs || '').split(/\s+/).filter(Boolean),
    window: s.fullscreen ? { fullscreen: true } : { width: s.width, height: s.height },
    overrides: { maxSockets: 8, detached: false },
  });
  if (!proc) { running = null; throw new Error('Minecraft failed to start. Check the log for details.'); }
  emit('state', 'starting');
  return true;
}

module.exports = { launch, isRunning: () => !!running, MC_VERSION };
