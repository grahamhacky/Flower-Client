'use strict';
const fs = require('fs');
const path = require('path');
const os = require('os');
const { app, safeStorage } = require('electron');
const versions = require('./versions');

function defaultGameDir() {
  if (process.platform === 'win32') return path.join(process.env.APPDATA || os.homedir(), '.flowerclient');
  if (process.platform === 'darwin') return path.join(os.homedir(), 'Library', 'Application Support', 'flowerclient');
  return path.join(os.homedir(), '.flowerclient');
}

const DEFAULTS = {
  mcVersion: versions.DEFAULT,
  ramMb: Math.min(4096, Math.max(2048, Math.floor(os.totalmem() / 1024 / 1024 / 4 / 512) * 512)),
  javaPath: '',
  gameDir: defaultGameDir(),
  width: 1280,
  height: 720,
  fullscreen: false,
  closeOnLaunch: false,
  performanceMods: { sodium: true, lithium: true, 'ferrite-core': true, entityculling: false },
  bloomTheme: 'auto',
  jvmArgs: '-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200',
};

const file = () => path.join(app.getPath('userData'), 'settings.json');
const tokenFile = () => path.join(app.getPath('userData'), 'account.bin');

let cache = null;

function load() {
  if (cache) return cache;
  try {
    const raw = JSON.parse(fs.readFileSync(file(), 'utf8'));
    cache = { ...DEFAULTS, ...raw, performanceMods: { ...DEFAULTS.performanceMods, ...(raw.performanceMods || {}) } };
  } catch {
    cache = { ...DEFAULTS };
  }
  cache.mcVersion = versions.resolve(cache.mcVersion);
  return cache;
}

function save(patch) {
  const next = { ...load(), ...patch };
  if (patch.performanceMods) next.performanceMods = { ...load().performanceMods, ...patch.performanceMods };
  next.mcVersion = versions.resolve(next.mcVersion);
  next.ramMb = Math.max(1024, Math.min(32768, Number(next.ramMb) || DEFAULTS.ramMb));
  next.width = Math.max(640, Number(next.width) || 1280);
  next.height = Math.max(480, Number(next.height) || 720);
  if (!next.gameDir) next.gameDir = DEFAULTS.gameDir;
  cache = next;
  fs.mkdirSync(path.dirname(file()), { recursive: true });
  fs.writeFileSync(file(), JSON.stringify(next, null, 2));
  return next;
}

/** Account = { profile: {id,name}, refresh } — encrypted with the OS keychain when available. */
function saveAccount(acc) {
  const json = Buffer.from(JSON.stringify(acc));
  const data = safeStorage.isEncryptionAvailable() ? Buffer.concat([Buffer.from('E1'), safeStorage.encryptString(json.toString())]) : Buffer.concat([Buffer.from('P1'), json]);
  fs.mkdirSync(path.dirname(tokenFile()), { recursive: true });
  fs.writeFileSync(tokenFile(), data, { mode: 0o600 });
}

function loadAccount() {
  try {
    const buf = fs.readFileSync(tokenFile());
    const tag = buf.subarray(0, 2).toString();
    const body = buf.subarray(2);
    const text = tag === 'E1' ? safeStorage.decryptString(body) : body.toString();
    return JSON.parse(text);
  } catch {
    return null;
  }
}

function clearAccount() { fs.rmSync(tokenFile(), { force: true }); }

module.exports = { load, save, saveAccount, loadAccount, clearAccount, DEFAULTS };
