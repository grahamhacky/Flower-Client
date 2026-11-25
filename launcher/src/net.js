'use strict';
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { Readable } = require('stream');
const { pipeline } = require('stream/promises');

const UA = 'RAMMENU/flower-client/1.0.0 (launcher)';

async function getJson(url) {
  const res = await fetch(url, { headers: { 'User-Agent': UA, Accept: 'application/json' } });
  if (!res.ok) throw new Error(`GET ${url} failed: ${res.status}`);
  return res.json();
}

/** Streams a file to disk with progress; verifies sha1 when given. Writes to .part then renames. */
async function download(url, dest, { sha1, sha256, onProgress } = {}) {
  await fs.promises.mkdir(path.dirname(dest), { recursive: true });
  const res = await fetch(url, { headers: { 'User-Agent': UA }, redirect: 'follow' });
  if (!res.ok || !res.body) throw new Error(`Download failed (${res.status}): ${url}`);
  const total = Number(res.headers.get('content-length')) || 0;
  let done = 0;
  const expected = sha256 || sha1;
  const hash = crypto.createHash(sha256 ? 'sha256' : 'sha1');
  const tmp = dest + '.part';
  const body = Readable.fromWeb(res.body);
  body.on('data', (chunk) => {
    done += chunk.length;
    hash.update(chunk);
    if (onProgress && total) onProgress(done / total);
  });
  await pipeline(body, fs.createWriteStream(tmp));
  if (expected && hash.digest('hex') !== expected.toLowerCase()) {
    await fs.promises.rm(tmp, { force: true });
    throw new Error(`Checksum mismatch for ${path.basename(dest)}`);
  }
  await fs.promises.rename(tmp, dest);
  return dest;
}

async function sha1File(file) {
  const hash = crypto.createHash('sha1');
  await pipeline(fs.createReadStream(file), hash);
  return hash.digest('hex');
}

module.exports = { getJson, download, sha1File, UA };
