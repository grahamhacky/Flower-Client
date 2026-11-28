'use strict';
/*
 * Minecraft versions the launcher can install and run.
 *
 * Every id here has a stable Fabric loader plus Sodium / Lithium / FerriteCore
 * builds on Modrinth, so the whole performance stack works on all of them.
 * `flower: true` marks the versions the bundled Flower HUD mod is built against;
 * on the others the launcher still sets up Fabric and the performance mods, and
 * skips the Flower jar (so an incompatible build never crashes the game).
 */
const VERSIONS = [
  { id: '1.21.4', flower: false },
  { id: '1.21.3', flower: false },
  { id: '1.21.1', flower: true, recommended: true },
  { id: '1.21', flower: true },
  { id: '1.20.6', flower: false },
  { id: '1.20.4', flower: false },
  { id: '1.20.1', flower: false },
  { id: '1.19.4', flower: false },
  { id: '1.19.2', flower: false },
  { id: '1.18.2', flower: false },
  { id: '1.17.1', flower: false },
  { id: '1.16.5', flower: false },
];

const DEFAULT = '1.21.1';

const ids = () => VERSIONS.map((v) => v.id);
const has = (id) => VERSIONS.some((v) => v.id === id);
const resolve = (id) => (has(id) ? id : DEFAULT);
const isFlowerNative = (id) => {
  const v = VERSIONS.find((x) => x.id === id);
  return !!(v && v.flower);
};
/** Public list for the renderer, newest first, each { id, flower, recommended? }. */
const list = () => VERSIONS.map((v) => ({ id: v.id, flower: !!v.flower, recommended: !!v.recommended }));

module.exports = { VERSIONS, DEFAULT, ids, has, resolve, isFlowerNative, list };
