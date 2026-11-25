'use strict';
const fs = require('fs');
const path = require('path');
const { getJson } = require('./net');

const META = 'https://meta.fabricmc.net/v2';

/** Installs the newest stable Fabric loader profile for the given MC version. Returns the version id. */
async function ensureFabric(root, mcVersion, progress) {
  progress('fabric', 0, 'Checking Fabric loader');
  let loaders;
  try {
    loaders = await getJson(`${META}/versions/loader/${mcVersion}`);
  } catch (e) {
    const cached = findInstalled(root, mcVersion);
    if (cached) return cached; // offline but already installed
    throw e;
  }
  const pick = loaders.find((l) => l.loader && l.loader.stable) || loaders[0];
  if (!pick) throw new Error(`Fabric has no loader for Minecraft ${mcVersion}.`);
  const loader = pick.loader.version;
  const id = `fabric-loader-${loader}-${mcVersion}`;
  const dir = path.join(root, 'versions', id);
  const json = path.join(dir, `${id}.json`);
  if (!fs.existsSync(json)) {
    progress('fabric', 0.3, `Installing Fabric ${loader}`);
    const profile = await getJson(`${META}/versions/loader/${mcVersion}/${loader}/profile/json`);
    await fs.promises.mkdir(dir, { recursive: true });
    await fs.promises.writeFile(json, JSON.stringify(profile, null, 2));
  }
  progress('fabric', 1, `Fabric ${loader}`);
  return id;
}

function findInstalled(root, mcVersion) {
  try {
    const ids = fs.readdirSync(path.join(root, 'versions')).filter((d) => d.startsWith('fabric-loader-') && d.endsWith('-' + mcVersion));
    return ids.sort().pop() || null;
  } catch { return null; }
}

module.exports = { ensureFabric };
