'use strict';
const path = require('path');
const { app, BrowserWindow, ipcMain, dialog, shell } = require('electron');
const store = require('./src/store');
const auth = require('./src/auth');
const game = require('./src/game');
const mods = require('./src/mods');
const modrinth = require('./src/modrinth');
const stats = require('./src/stats');
const keyauth = require('./src/keyauth');

let win;

const AUTH_ERRORS = {
  'error.gui.closed': 'Sign-in window was closed.',
  'error.auth.xsts.userNotFound': 'This Microsoft account has no Xbox profile. Sign in at minecraft.net once, then try again.',
  'error.auth.xsts.child': 'Child accounts need to be added to a family by an adult first.',
  'error.auth.minecraft.profile': 'This account does not own Minecraft: Java Edition.',
};
const friendly = (e) => {
  let msg;
  if (typeof e === 'string') msg = e;
  else if (e && e.message) msg = e.message;
  else if (e && e.ts) msg = e.ts;
  else { try { msg = JSON.stringify(e); } catch { msg = String(e); } }
  console.error('[flower]', e);
  return AUTH_ERRORS[msg] || msg;
};

function createWindow() {
  win = new BrowserWindow({
    width: 1120,
    height: 700,
    minWidth: 940,
    minHeight: 600,
    frame: false,
    backgroundColor: '#0c0a0c',
    show: false,
    title: 'Flower Client',
    icon: path.join(__dirname, 'build', 'icon.png'),
    webPreferences: { preload: path.join(__dirname, 'preload.js'), contextIsolation: true, nodeIntegration: false, sandbox: true },
  });
  win.loadFile(path.join(__dirname, 'renderer', 'index.html'));
  win.webContents.once('did-finish-load', () => stats.send('launch'));
  win.once('ready-to-show', () => win.show());
  win.webContents.setWindowOpenHandler(({ url }) => {
    if (/^https:\/\//.test(url)) shell.openExternal(url);
    return { action: 'deny' };
  });
  win.on('maximize', () => send('window:maximized', true));
  win.on('unmaximize', () => send('window:maximized', false));
}

auth.setLogger((line) => send('game:log', line));

function send(channel, payload) { if (win && !win.isDestroyed()) win.webContents.send(channel, payload); }

const handle = (channel, fn) => ipcMain.handle(channel, async (_e, ...args) => {
  try { return { ok: true, data: await fn(...args) }; } catch (e) { return { ok: false, error: friendly(e) }; }
});

handle('settings:get', () => store.load());
handle('settings:set', (patch) => store.save(patch || {}));
handle('settings:pickFolder', async () => {
  const r = await dialog.showOpenDialog(win, { properties: ['openDirectory', 'createDirectory'] });
  return r.canceled ? null : r.filePaths[0];
});
handle('settings:pickJava', async () => {
  const r = await dialog.showOpenDialog(win, { properties: ['openFile'], filters: process.platform === 'win32' ? [{ name: 'Java', extensions: ['exe'] }] : [] });
  return r.canceled ? null : r.filePaths[0];
});
handle('settings:openGameDir', () => shell.openPath(store.load().gameDir));

handle('account:get', () => auth.publicAccount());
handle('account:login', () => auth.login());
handle('account:logout', () => auth.logout());

handle('modules:get', () => mods.readModules(store.load().gameDir));
handle('modules:set', (name, enabled) => mods.setModule(store.load().gameDir, name, enabled));
handle('mods:performance', () => mods.PERFORMANCE);
handle('modrinth:search', (opts) => modrinth.search(store.load().gameDir, game.MC_VERSION, opts || {}));
handle('modrinth:installed', () => modrinth.installed(store.load().gameDir));
handle('modrinth:install', (id) => modrinth.install(store.load().gameDir, game.MC_VERSION, id, (p) => send('modrinth:progress', p)));
handle('modrinth:uninstall', (id) => modrinth.uninstall(store.load().gameDir, id));
handle('keyauth:status', () => keyauth.status());
handle('keyauth:redeem', (key) => keyauth.redeem(key));
handle('keyauth:signout', () => { keyauth.signOut(); return true; });
handle('analytics:get', () => { const s = store.load(); return { analytics: s.analytics, asked: s.analyticsAsked, available: stats.hasEndpoint() }; });
handle('analytics:set', (on) => { store.save({ analytics: !!on, analyticsAsked: true }); if (on) stats.send('opt_in'); return store.load().analytics; });
handle('discord:getAppId', () => mods.getDiscordAppId(store.load().gameDir));
handle('discord:setAppId', (id) => mods.setDiscordAppId(store.load().gameDir, id));
handle('app:info', () => ({ version: app.getVersion(), mc: game.MC_VERSION, flowerBundled: !!mods.findBundledFlowerJar(), platform: process.platform }));

handle('game:launch', () => game.launch((type, payload) => {
  if (type === 'state' && payload === 'running') stats.send('play');
  if (type === 'state' && payload === 'running' && store.load().closeOnLaunch) setTimeout(() => app.quit(), 1500);
  send('game:' + type, payload);
}));

ipcMain.on('window:minimize', () => win && win.minimize());
ipcMain.on('window:maximize', () => win && (win.isMaximized() ? win.unmaximize() : win.maximize()));
ipcMain.on('window:close', () => win && win.close());

if (!app.requestSingleInstanceLock()) app.quit();
else {
  app.on('second-instance', () => { if (win) { if (win.isMinimized()) win.restore(); win.focus(); } });
  app.whenReady().then(createWindow);
  app.on('window-all-closed', () => { if (!game.isRunning() || process.platform !== 'darwin') app.quit(); });
}
