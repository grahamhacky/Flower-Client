'use strict';
const { contextBridge, ipcRenderer } = require('electron');

const call = async (channel, ...args) => {
  const r = await ipcRenderer.invoke(channel, ...args);
  if (!r.ok) throw new Error(r.error);
  return r.data;
};
const on = (channel, cb) => {
  const fn = (_e, payload) => cb(payload);
  ipcRenderer.on(channel, fn);
  return () => ipcRenderer.removeListener(channel, fn);
};

contextBridge.exposeInMainWorld('flower', {
  settings: { get: () => call('settings:get'), set: (p) => call('settings:set', p), pickFolder: () => call('settings:pickFolder'), pickJava: () => call('settings:pickJava'), openGameDir: () => call('settings:openGameDir') },
  account: { get: () => call('account:get'), login: () => call('account:login'), logout: () => call('account:logout') },
  modules: { get: () => call('modules:get'), set: (n, e) => call('modules:set', n, e), performance: () => call('mods:performance') },
  app: { info: () => call('app:info') },
  analytics: { get: () => call('analytics:get'), set: (on) => call('analytics:set', on) },
  discord: { getAppId: () => call('discord:getAppId'), setAppId: (id) => call('discord:setAppId', id) },
  modrinth: {
    search: (opts) => call('modrinth:search', opts),
    installed: () => call('modrinth:installed'),
    install: (id) => call('modrinth:install', id),
    uninstall: (id) => call('modrinth:uninstall', id),
    onProgress: (cb) => on('modrinth:progress', cb),
  },
  game: {
    launch: () => call('game:launch'),
    onProgress: (cb) => on('game:progress', cb),
    onState: (cb) => on('game:state', cb),
    onLog: (cb) => on('game:log', cb),
    onError: (cb) => on('game:error', cb),
  },
  window: {
    minimize: () => ipcRenderer.send('window:minimize'),
    maximize: () => ipcRenderer.send('window:maximize'),
    close: () => ipcRenderer.send('window:close'),
    onMaximized: (cb) => on('window:maximized', cb),
  },
});
