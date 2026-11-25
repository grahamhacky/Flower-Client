'use strict';
const fs = require('fs');
const path = require('path');
const { app } = require('electron');
const { getJson, download, sha1File } = require('./net');

/** Optional performance mods (Modrinth slugs) the user can toggle in the launcher. */
const PERFORMANCE = {
  sodium: { name: 'Sodium', desc: 'Modern renderer — big FPS gains' },
  lithium: { name: 'Lithium', desc: 'Faster game logic and physics' },
  'ferrite-core': { name: 'FerriteCore', desc: 'Lower memory usage' },
  entityculling: { name: 'Entity Culling', desc: "Skips rendering entities you can't see" },
};
const REQUIRED = ['fabric-api'];

/** In-game Flower modules. Names must match the mod's module names. */
const MODULES = [
  ['FPS', 'Frames per second', true, 'HUD'],
  ['CPS', 'Clicks per second (left | right)', true, 'HUD'],
  ['Keystrokes', 'WASD, mouse and space display', true, 'HUD'],
  ['Coordinates', 'XYZ position and biome', true, 'HUD'],
  ['Armor Status', 'Armor and held item durability', true, 'HUD'],
  ['Potion Effects', 'Active effects with time left', true, 'HUD'],
  ['Ping', 'Latency to the server', true, 'HUD'],
  ['Direction', 'Compass heading', false, 'HUD'],
  ['Clock', 'Real-world time', false, 'HUD'],
  ['Speed', 'Horizontal blocks per second', false, 'HUD'],
  ['Zoom', 'Hold C to zoom, scroll to adjust', true, 'Utility'],
  ['Toggle Sprint', 'Always sprint when walking forward', false, 'Utility'],
  ['Biome Tint', "HUD accent follows the biome you're in", true, 'Utility'],
  ['Discord Presence', 'Show Flower Client on your Discord profile', false, 'Utility'],
].map(([name, desc, def, category]) => ({ name, desc, def, category }));

function bundledModDirs() {
  return [
    path.join(process.resourcesPath || '', 'mods'),
    path.join(app.getAppPath(), 'resources', 'mods'),
    path.join(app.getAppPath(), '..', 'mod', 'build', 'libs'),
  ];
}

function findBundledFlowerJar() {
  for (const dir of bundledModDirs()) {
    try {
      const jar = fs.readdirSync(dir).find((f) => /^flowerclient-.*\.jar$/.test(f) && !f.includes('-sources'));
      if (jar) return path.join(dir, jar);
    } catch { /* not there */ }
  }
  return null;
}

async function modrinthFile(slug, mcVersion) {
  const q = `loaders=${encodeURIComponent('["fabric"]')}&game_versions=${encodeURIComponent(JSON.stringify([mcVersion]))}`;
  const versions = await getJson(`https://api.modrinth.com/v2/project/${slug}/version?${q}`);
  const v = versions.find((x) => x.version_type === 'release') || versions[0];
  if (!v) throw new Error(`${slug} has no build for Minecraft ${mcVersion}.`);
  const f = v.files.find((x) => x.primary) || v.files[0];
  return { url: f.url, filename: f.filename, sha1: f.hashes.sha1 };
}

/**
 * Makes gameDir/mods match what the launcher manages: required + enabled performance mods + Flower.
 * Mods the user dropped in by hand are never touched.
 */
async function syncMods(gameDir, mcVersion, perf, progress) {
  const modsDir = path.join(gameDir, 'mods');
  await fs.promises.mkdir(modsDir, { recursive: true });
  const manifestPath = path.join(modsDir, '.flower-managed.json');
  let managed = {};
  try { managed = JSON.parse(await fs.promises.readFile(manifestPath, 'utf8')); } catch { /* first run */ }

  const wanted = [...REQUIRED, ...Object.keys(PERFORMANCE).filter((s) => perf[s])];
  const warnings = [];

  for (const [slug, file] of Object.entries(managed)) {
    if (slug !== 'flowerclient' && !wanted.includes(slug)) {
      await fs.promises.rm(path.join(modsDir, file), { force: true });
      delete managed[slug];
    }
  }

  for (let i = 0; i < wanted.length; i++) {
    const slug = wanted[i];
    const label = slug === 'fabric-api' ? 'Fabric API' : PERFORMANCE[slug].name;
    progress('mods', i / (wanted.length + 1), `Checking ${label}`);
    try {
      const f = await modrinthFile(slug, mcVersion);
      const dest = path.join(modsDir, f.filename);
      const ok = fs.existsSync(dest) && (await sha1File(dest)) === f.sha1;
      if (!ok) {
        await download(f.url, dest, { sha1: f.sha1, onProgress: (p) => progress('mods', (i + p) / (wanted.length + 1), `Downloading ${label}`) });
      }
      if (managed[slug] && managed[slug] !== f.filename) await fs.promises.rm(path.join(modsDir, managed[slug]), { force: true });
      managed[slug] = f.filename;
    } catch (e) {
      if (managed[slug] && fs.existsSync(path.join(modsDir, managed[slug]))) warnings.push(`${label}: offline, using cached copy`);
      else if (slug === 'fabric-api') throw new Error(`Could not get Fabric API: ${e.message}`);
      else warnings.push(`${label} skipped: ${e.message}`);
    }
  }

  const jar = findBundledFlowerJar();
  if (jar) {
    const name = path.basename(jar);
    if (managed.flowerclient && managed.flowerclient !== name) await fs.promises.rm(path.join(modsDir, managed.flowerclient), { force: true });
    await fs.promises.copyFile(jar, path.join(modsDir, name));
    managed.flowerclient = name;
  } else if (!managed.flowerclient) {
    warnings.push('Flower Client mod jar is not bundled with this launcher build — the game will start without HUD mods.');
  }

  await fs.promises.writeFile(manifestPath, JSON.stringify(managed, null, 2));
  progress('mods', 1, 'Mods ready');
  return warnings;
}

function configPath(gameDir) { return path.join(gameDir, 'config', 'flowerclient.json'); }

function readModules(gameDir) {
  let cfg = {};
  try { cfg = JSON.parse(fs.readFileSync(configPath(gameDir), 'utf8')); } catch { /* defaults */ }
  const mods = cfg.modules || {};
  return MODULES.map((m) => ({ ...m, enabled: mods[m.name] && typeof mods[m.name].enabled === 'boolean' ? mods[m.name].enabled : m.def }));
}

/** Only touches "enabled" so HUD positions set in-game survive. */
function setModule(gameDir, name, enabled) {
  const file = configPath(gameDir);
  let cfg = {};
  try { cfg = JSON.parse(fs.readFileSync(file, 'utf8')); } catch { /* new */ }
  cfg.modules = cfg.modules || {};
  cfg.modules[name] = { ...(cfg.modules[name] || {}), enabled: !!enabled };
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, JSON.stringify(cfg, null, 2));
  return readModules(gameDir);
}

function readRawConfig(gameDir) {
  try { return JSON.parse(fs.readFileSync(configPath(gameDir), 'utf8')); } catch { return {}; }
}
function getDiscordAppId(gameDir) { return String(readRawConfig(gameDir).discordAppId || ''); }
function setDiscordAppId(gameDir, id) {
  const file = configPath(gameDir);
  const cfg = readRawConfig(gameDir);
  cfg.discordAppId = String(id || '').trim().replace(/[^0-9]/g, '');
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, JSON.stringify(cfg, null, 2));
  return cfg.discordAppId;
}

function writeRawConfig(gameDir, cfg) {
  const file = configPath(gameDir);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, JSON.stringify(cfg, null, 2));
}

const CAPE_CODES = { RELEASE2027: 'flower' };

/** Returns { unlocked, cape }. */
function capeStatus(gameDir) {
  const c = readRawConfig(gameDir);
  return { unlocked: !!c.capeUnlocked, cape: c.cape || 'flower' };
}

/** Redeem a code. Returns { ok, cape?, message }. */
function redeemCape(gameDir, code) {
  const key = String(code || '').trim().toUpperCase();
  const cape = CAPE_CODES[key];
  if (!cape) return { ok: false, message: "That code isn't valid." };
  const cfg = readRawConfig(gameDir);
  cfg.capeUnlocked = true;
  cfg.cape = cape;
  writeRawConfig(gameDir, cfg);
  return { ok: true, cape, message: 'Flower cape unlocked!' };
}

/** Equip or unequip an unlocked cape. */
function setCape(gameDir, wear) {
  const cfg = readRawConfig(gameDir);
  if (!cfg.capeUnlocked) return { unlocked: false, cape: 'none' };
  cfg.cape = wear ? 'flower' : 'none';
  writeRawConfig(gameDir, cfg);
  return { unlocked: true, cape: cfg.cape };
}

function backgroundPath(gameDir) { return path.join(gameDir, 'config', 'flowerclient', 'background.png'); }
function hasBackground(gameDir) { try { return fs.existsSync(backgroundPath(gameDir)); } catch { return false; } }
function setBackground(gameDir, srcPath) {
  const dest = backgroundPath(gameDir);
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  fs.copyFileSync(srcPath, dest);
  return true;
}
function clearBackground(gameDir) { fs.rmSync(backgroundPath(gameDir), { force: true }); return true; }

module.exports = { syncMods, readModules, setModule, getDiscordAppId, setDiscordAppId, capeStatus, redeemCape, setCape, hasBackground, setBackground, clearBackground, PERFORMANCE, findBundledFlowerJar };
