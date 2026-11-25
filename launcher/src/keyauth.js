'use strict';
/*
 * KeyAuth license gate for Flower Client.
 *
 * Talks to the KeyAuth 1.3 REST API directly (no third-party SDK) so the exact traffic is
 * auditable here. A machine HWID is sent to KeyAuth to bind the key to this computer — this is
 * standard license HWID-locking, handled by the licensing service, not reported anywhere else.
 *
 * The validated key is cached (encrypted with the OS keychain) so you don't re-enter it every
 * launch; it's re-validated online when the network is reachable, and trusted until its expiry
 * when offline.
 */
const fs = require('fs');
const os = require('os');
const path = require('path');
const crypto = require('crypto');
const { app, safeStorage } = require('electron');

const APP = { name: 'Flower Client', ownerid: 'cwJqpOlZd0', version: '1.0' };
const API = 'https://keyauth.win/api/1.3/';

let sessionid = null;
let initialized = false;

const licenseFile = () => path.join(app.getPath('userData'), 'license.bin');
const hwidFile = () => path.join(app.getPath('userData'), 'hwid');

/** Stable per-machine id. Persisted so it survives small hardware reporting changes. */
function hwid() {
  try { return fs.readFileSync(hwidFile(), 'utf8').trim(); } catch { /* generate */ }
  const seed = [os.hostname(), os.platform(), os.arch(), (os.cpus()[0] || {}).model, os.totalmem()].join('|');
  let salt = '';
  try { salt = fs.readFileSync(hwidFile() + '.salt', 'utf8'); }
  catch { salt = crypto.randomUUID(); try { fs.writeFileSync(hwidFile() + '.salt', salt); } catch { /* ignore */ } }
  const id = crypto.createHash('sha256').update(seed + '|' + salt).digest('hex').slice(0, 32);
  try { fs.writeFileSync(hwidFile(), id); } catch { /* ignore */ }
  return id;
}

function saveLicense(obj) {
  const json = Buffer.from(JSON.stringify(obj));
  const data = safeStorage.isEncryptionAvailable()
    ? Buffer.concat([Buffer.from('E1'), safeStorage.encryptString(json.toString())])
    : Buffer.concat([Buffer.from('P1'), json]);
  fs.mkdirSync(path.dirname(licenseFile()), { recursive: true });
  fs.writeFileSync(licenseFile(), data, { mode: 0o600 });
}

function loadLicense() {
  try {
    const buf = fs.readFileSync(licenseFile());
    const tag = buf.subarray(0, 2).toString();
    const body = buf.subarray(2);
    return JSON.parse(tag === 'E1' ? safeStorage.decryptString(body) : body.toString());
  } catch { return null; }
}

function clearLicense() { fs.rmSync(licenseFile(), { force: true }); }

async function post(params) {
  const body = new URLSearchParams({ ...params, name: APP.name, ownerid: APP.ownerid }).toString();
  const res = await fetch(API, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'User-Agent': 'KeyAuth' },
    body,
  });
  const text = await res.text();
  try { return JSON.parse(text); }
  catch { throw new Error('Unexpected response from the license server.'); }
}

async function init() {
  if (initialized && sessionid) return;
  const res = await post({ type: 'init', ver: APP.version });
  if (!res.success) {
    if (res.message && /version/i.test(res.message)) throw new Error('This version of Flower is out of date. ' + res.message);
    throw new Error(res.message || 'Could not reach the license server.');
  }
  sessionid = res.sessionid;
  initialized = true;
}

function expiryOf(res) {
  const sub = res && res.info && Array.isArray(res.info.subscriptions) ? res.info.subscriptions[0] : null;
  const exp = sub && sub.expiry ? Number(sub.expiry) * 1000 : 0;
  return Number.isFinite(exp) ? exp : 0;
}

/** Redeem a key the user typed. Returns { ok, message, expiry }. */
async function redeem(key) {
  key = String(key || '').trim();
  if (!key) return { ok: false, message: 'Enter a license key.' };
  try {
    await init();
    const res = await post({ type: 'license', key, hwid: hwid(), sessionid });
    if (res.success) {
      const expiry = expiryOf(res);
      saveLicense({ key, hwid: hwid(), expiry, savedAt: Date.now() });
      return { ok: true, message: 'Welcome to Flower.', expiry };
    }
    return { ok: false, message: friendly(res.message) };
  } catch (e) {
    return { ok: false, message: e.message || 'Could not reach the license server.' };
  }
}

/** Check for an already-validated key on launch. Re-validates online, trusts cache offline. */
async function status() {
  const saved = loadLicense();
  if (!saved || !saved.key) return { valid: false };
  try {
    await init();
    const res = await post({ type: 'license', key: saved.key, hwid: hwid(), sessionid });
    if (res.success) {
      const expiry = expiryOf(res);
      saveLicense({ ...saved, expiry, savedAt: Date.now() });
      return { valid: true, expiry };
    }
    // Server rejected it (expired, banned, reset) — require a new key.
    clearLicense();
    return { valid: false, message: friendly(res.message) };
  } catch {
    // Offline: honour the cached key until its known expiry.
    if (!saved.expiry || saved.expiry > Date.now()) return { valid: true, offline: true, expiry: saved.expiry };
    return { valid: false, message: 'Your license needs re-checking and the license server is unreachable.' };
  }
}

function friendly(message) {
  const m = String(message || '').toLowerCase();
  if (m.includes('hwid')) return 'This key is locked to a different computer. Reset its HWID or use the right key.';
  if (m.includes('expired')) return 'This key has expired.';
  if (m.includes('used') || m.includes('taken')) return 'This key is already in use on another machine.';
  if (m.includes('banned')) return 'This key has been banned.';
  if (m.includes('invalid') || m.includes('not found') || m.includes('exist')) return 'That key isn\'t valid.';
  return message || 'That key was rejected.';
}

function signOut() { clearLicense(); sessionid = null; initialized = false; }

module.exports = { status, redeem, signOut, APP };
