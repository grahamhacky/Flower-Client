'use strict';
/*
 * Opt-in, anonymous usage stats. Sends NOTHING unless the user explicitly turned it on.
 * No account name, no IP, no hardware id, no file paths — only a random install id,
 * the launcher/MC/OS versions and which performance mods are enabled.
 *
 * The destination URL is injected at build time from the STATS_WEBHOOK repo secret into
 * stats-endpoint.json, so it never lives in source. If it's empty, this is a no-op.
 */
const fs = require('fs');
const path = require('path');
const { app } = require('electron');
const store = require('./store');

function endpoint() {
  try {
    const raw = fs.readFileSync(path.join(app.getAppPath(), 'stats-endpoint.json'), 'utf8');
    const url = JSON.parse(raw).url;
    return typeof url === 'string' && /^https:\/\//.test(url) ? url : '';
  } catch {
    return '';
  }
}

function discordEmbed(p) {
  const fields = [
    { name: 'Event', value: '`' + p.event + '`', inline: true },
    { name: 'Launcher', value: 'v' + p.launcher, inline: true },
    { name: 'Minecraft', value: p.mc, inline: true },
    { name: 'OS', value: `${p.os} ${p.arch}`, inline: true },
    { name: 'Performance mods', value: p.perf.length ? p.perf.join(', ') : 'none', inline: true },
    { name: 'Install', value: '`' + p.installId.slice(0, 8) + '`', inline: true },
  ];
  return {
    username: 'Flower Stats',
    embeds: [{
      title: '✿ Flower Client',
      description: 'Anonymous usage ping',
      color: 0xe85d8a,
      fields,
      timestamp: p.ts,
      footer: { text: 'Opt-in · no account, IP or hardware info' },
    }],
  };
}

/** Fire-and-forget; never throws, never blocks a launch. */
async function send(event, extra = {}) {
  const s = store.load();
  if (!s.analytics) return;
  const url = endpoint();
  if (!url) return;
  const perf = s.performanceMods || {};
  const payload = {
    event,
    installId: store.installId(),
    launcher: app.getVersion(),
    mc: '1.21.1',
    os: process.platform,
    arch: process.arch,
    perf: Object.keys(perf).filter((k) => perf[k]),
    ...extra,
    ts: new Date().toISOString(),
  };
  const isDiscord = /discord(app)?\.com\/api\/webhooks\//.test(url);
  try {
    await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(isDiscord ? discordEmbed(payload) : payload),
    });
  } catch {
    /* best effort */
  }
}

module.exports = { send, hasEndpoint: () => !!endpoint() };
