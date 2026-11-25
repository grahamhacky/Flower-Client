'use strict';
const fs = require('fs');
const path = require('path');
const { execFile } = require('child_process');
const { app } = require('electron');
const { getJson, download } = require('./net');

const REQUIRED = 21;

function javaVersion(bin) {
  return new Promise((resolve) => {
    execFile(bin, ['-version'], { timeout: 10000, windowsHide: true }, (err, stdout, stderr) => {
      if (err) return resolve(0);
      const m = /version "(\d+)(?:\.(\d+))?/.exec(stderr + stdout);
      if (!m) return resolve(0);
      const major = Number(m[1]) === 1 ? Number(m[2]) : Number(m[1]);
      resolve(major);
    });
  });
}

function runtimeDir() { return path.join(app.getPath('userData'), 'runtime', 'java' + REQUIRED); }

function findBin(dir) {
  const exe = process.platform === 'win32' ? 'javaw.exe' : 'java';
  const stack = [dir];
  while (stack.length) {
    const d = stack.pop();
    let entries = [];
    try { entries = fs.readdirSync(d, { withFileTypes: true }); } catch { continue; }
    for (const e of entries) {
      const full = path.join(d, e.name);
      if (e.isDirectory()) stack.push(full);
      else if (e.name === exe && path.basename(d) === 'bin') return full;
    }
  }
  return null;
}

/** Returns a path to a Java 21+ binary, downloading Eclipse Temurin JRE 21 if nothing suitable exists. */
async function ensureJava(customPath, progress) {
  if (customPath) {
    const v = await javaVersion(customPath);
    if (v >= REQUIRED) return customPath;
    throw new Error(`The Java at "${customPath}" is version ${v || 'unknown'}; Minecraft 1.21.1 needs Java ${REQUIRED}+. Clear the Java path in Settings to use the built-in runtime.`);
  }
  const existing = findBin(runtimeDir());
  if (existing && (await javaVersion(existing)) >= REQUIRED) return existing;

  const os = { win32: 'windows', darwin: 'mac', linux: 'linux' }[process.platform];
  const arch = { x64: 'x64', arm64: 'aarch64' }[process.arch];
  if (!os || !arch) throw new Error(`No automatic Java download for ${process.platform}/${process.arch}. Set a Java ${REQUIRED} path in Settings.`);

  progress('java', 0, 'Finding Java 21 runtime');
  const assets = await getJson(`https://api.adoptium.net/v3/assets/latest/${REQUIRED}/hotspot?architecture=${arch}&image_type=jre&os=${os}&vendor=eclipse`);
  const pkg = assets && assets[0] && assets[0].binary && assets[0].binary.package;
  if (!pkg) throw new Error('Could not find a Java 21 download from Adoptium.');

  const tmpDir = path.join(app.getPath('userData'), 'runtime', 'tmp');
  const archive = path.join(tmpDir, pkg.name);
  await download(pkg.link, archive, { sha256: pkg.checksum, onProgress: (p) => progress('java', p * 0.9, 'Downloading Java 21') });

  progress('java', 0.92, 'Extracting Java 21');
  const out = runtimeDir();
  await fs.promises.rm(out, { recursive: true, force: true });
  await fs.promises.mkdir(out, { recursive: true });
  if (pkg.name.endsWith('.zip')) {
    await require('extract-zip')(archive, { dir: out });
  } else {
    await require('tar').x({ file: archive, cwd: out });
  }
  await fs.promises.rm(tmpDir, { recursive: true, force: true });
  const bin = findBin(out);
  if (!bin) throw new Error('Java was downloaded but its executable was not found.');
  if (process.platform !== 'win32') await fs.promises.chmod(bin, 0o755);
  progress('java', 1, 'Java 21 ready');
  return bin;
}

module.exports = { ensureJava, javaVersion };
