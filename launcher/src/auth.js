'use strict';
const { Auth } = require('msmc');
const store = require('./store');

let session = null; // { profile, mclc, expires }

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
  const auth = new Auth('select_account');
  const xbox = await auth.launch('electron', { width: 520, height: 680, resizable: false, title: 'Sign in to Flower Client' });
  return finish(xbox);
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
    throw new Error('Your sign-in expired. Please sign in again. (' + (e && e.message ? e.message : e) + ')');
  }
}

function logout() { session = null; store.clearAccount(); }

module.exports = { login, logout, authorization, publicAccount };
