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

async function finish(xbox) {
  const mc = await xbox.getMinecraft();
  if (!mc.profile) throw new Error('This Microsoft account does not own Minecraft: Java Edition.');
  session = { profile: { id: mc.profile.id, name: mc.profile.name }, mclc: mc.mclc(), expires: Date.now() + 20 * 60 * 1000 };
  store.saveAccount({ profile: session.profile, refresh: xbox.save() });
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
  const saved = store.loadAccount();
  if (!saved || !saved.refresh) throw new Error('Sign in with Microsoft first.');
  try {
    const xbox = await new Auth('select_account').refresh(saved.refresh);
    await finish(xbox);
    return session.mclc;
  } catch (e) {
    const err = await toError(e);
    throw new Error('Your sign-in expired. Please sign in again. (' + err.message + ')');
  }
}

function logout() { session = null; store.clearAccount(); }

module.exports = { login, logout, authorization, publicAccount };
