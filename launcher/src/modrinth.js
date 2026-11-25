'use strict';
/*
 * Modrinth browser: search Fabric mods for the game version, install them (with their
 * required dependencies) into gameDir/mods and remove them again.
 * Mods installed here are tracked in mods/.flower-modrinth.json so the launcher's own
 * sync (Fabric API, performance mods, Flower) never touches them.
 */
const fs = require('fs');
const path = require('path');
const { getJson, download, sha1File } = require('./net');

const API = 'https://api.modrinth.com/v2';
const SORTS = new Set(['relevance', 'downloads', 'follows', 'newest', 'updated']);
/** Handled elsewhere in the launcher: base requirement + the Performance toggles + Flower itself. */
const MANAGED = new Set(['fabric-api', 'P7dR8mSH', 'sodium', 'AANobbMI', 'lithium', 'gvQqBUqZ', 'ferrite-core', 'uXXizFIs', 'entityculling', 'NNAgCjsB']);

function manifestPath(gameDir) { return path.join(gameDir, 'mods', '.flower-modrinth.json'); }

function readManifest(gameDir) {
  try { return JSON.parse(fs.readFileSync(manifestPath(gameDir), 'utf8')); } catch { return {}; }
}

function writeManifest(gameDir, m) {
  fs.mkdirSync(path.dirname(manifestPath(gameDir)), { recursive: true });
  fs.writeFileSync(manifestPath(gameDir), JSON.stringify(m, null, 2));
}

/** Drop entries whose jar the user deleted by hand. */
function installed(gameDir) {
  const m = readManifest(gameDir);
  let changed = false;
  for (const [id, e] of Object.entries(m)) {
    if (!fs.existsSync(path.join(gameDir, 'mods', e.filename))) { delete m[id]; changed = true; }
  }
  if (changed) writeManifest(gameDir, m);
  return m;
}

async function search(gameDir, mcVersion, { query = '', sort = 'relevance', offset = 0, limit = 20 } = {}) {
  const facets = JSON.stringify([['project_type:mod'], ['categories:fabric'], [`versions:${mcVersion}`]]);
  const index = SORTS.has(sort) ? sort : 'relevance';
  const url = `${API}/search?query=${encodeURIComponent(query)}&facets=${encodeURIComponent(facets)}&index=${index}&offset=${Math.max(0, offset | 0)}&limit=${Math.min(50, limit | 0 || 20)}`;
  const res = await getJson(url);
  const have = installed(gameDir);
  return {
    total: res.total_hits || 0,
    offset: res.offset || 0,
    hits: (res.hits || []).map((h) => ({
      id: h.project_id,
      slug: h.slug,
      title: h.title,
      author: h.author,
      description: h.description,
      icon: h.icon_url || null,
      downloads: h.downloads || 0,
      follows: h.follows || 0,
      categories: (h.categories || []).filter((c) => !['fabric', 'forge', 'quilt', 'neoforge'].includes(c)).slice(0, 3),
      clientSide: h.client_side,
      installed: !!have[h.project_id],
      managed: MANAGED.has(h.project_id) || MANAGED.has(h.slug),
    })),
  };
}

async function pickVersion(projectId, mcVersion) {
  const q = `loaders=${encodeURIComponent('["fabric"]')}&game_versions=${encodeURIComponent(JSON.stringify([mcVersion]))}`;
  const versions = await getJson(`${API}/project/${encodeURIComponent(projectId)}/version?${q}`);
  const v = versions.find((x) => x.version_type === 'release') || versions.find((x) => x.version_type === 'beta') || versions[0];
  if (!v) throw new Error(`No Fabric build for Minecraft ${mcVersion}.`);
  return v;
}

/**
 * Installs a project and, recursively, its required dependencies.
 * Returns the titles installed in this call.
 */
async function install(gameDir, mcVersion, projectId, onProgress = () => {}) {
  const modsDir = path.join(gameDir, 'mods');
  await fs.promises.mkdir(modsDir, { recursive: true });
  const manifest = installed(gameDir);
  const done = [];
  const seen = new Set();

  async function one(id, isDependency) {
    if (seen.has(id) || MANAGED.has(id)) return;
    seen.add(id);
    if (manifest[id] && fs.existsSync(path.join(modsDir, manifest[id].filename))) return;

    const [project, v] = await Promise.all([getJson(`${API}/project/${encodeURIComponent(id)}`), pickVersion(id, mcVersion)]);
    if (MANAGED.has(project.slug)) return;
    if (project.client_side === 'unsupported') {
      if (isDependency) return;
      throw new Error(`${project.title} is server-only and does nothing in the client.`);
    }

    // dependencies first so a half-finished install never leaves a mod without what it needs
    for (const dep of v.dependencies || []) {
      if (dep.dependency_type !== 'required') continue;
      let depId = dep.project_id;
      if (!depId && dep.version_id) depId = (await getJson(`${API}/version/${dep.version_id}`)).project_id;
      if (depId) await one(depId, true);
    }

    const f = v.files.find((x) => x.primary) || v.files[0];
    const dest = path.join(modsDir, f.filename);
    if (!(fs.existsSync(dest) && (await sha1File(dest)) === f.hashes.sha1)) {
      onProgress({ id, title: project.title, pct: 0 });
      await download(f.url, dest, { sha1: f.hashes.sha1, onProgress: (p) => onProgress({ id, title: project.title, pct: p }) });
    }
    manifest[project.id] = {
      title: project.title, slug: project.slug, icon: project.icon_url || null,
      versionId: v.id, version: v.version_number, filename: f.filename, dependency: !!isDependency,
    };
    writeManifest(gameDir, manifest);
    done.push(project.title);
  }

  await one(projectId, false);
  return done;
}

async function uninstall(gameDir, projectId) {
  const m = installed(gameDir);
  const e = m[projectId];
  if (!e) return false;
  await fs.promises.rm(path.join(gameDir, 'mods', e.filename), { force: true });
  delete m[projectId];
  writeManifest(gameDir, m);
  return true;
}

module.exports = { search, install, uninstall, installed };
