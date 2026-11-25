/*
 * Flower Client — ASCII bloom.
 * A procedurally built 3D flower (layered, cupped petals around a pollen dome) projected
 * into a character grid with a z-buffer, shaded by luminance. It blooms from a seed,
 * slowly turns, and can scatter into drifting particles and grow back.
 */
'use strict';

(function () {
  const RAMP = ' .,:-=+*#%@';
  // Seasonal looks. Layers run outside-in: count, length, width, open angle (rad from vertical),
  // closed angle, tip curl, twist. Motes are the little particles drifting around the open flower.
  const L = (n, len, w, open, closed, curl, twist) => ({ n, len, w, open, closed, curl, twist });
  const ROSE_LEAF = ['#16301f', '#24503a', '#3a7a55', '#5fae80', '#9fd8b4', '#d4f5df'];
  const THEMES = {
    rose: {
      label: 'Rose', petal: ['#5a2236', '#8a2f52', '#b8436f', '#e0608f', '#f08cb0', '#f7b6cc', '#fde0ea'],
      pollen: ['#6b4a16', '#b07d22', '#f0b545', '#ffd678', '#fff0c2'], leaf: ROSE_LEAF, center: 0.17,
      layers: [L(6, 1.0, 0.33, 1.22, 0.22, 0.5, 0), L(6, 0.8, 0.29, 0.9, 0.16, 0.35, Math.PI / 6), L(5, 0.6, 0.25, 0.58, 0.1, 0.22, Math.PI / 5), L(4, 0.42, 0.21, 0.3, 0.05, 0.1, Math.PI / 4)],
      glow: [232, 93, 138], glow2: [159, 216, 180],
      motes: { count: 46, dir: 1, speed: 0.07, chars: '.·', colors: ['#ffd678', '#f7b6cc'] },
    },
    sakura: {
      label: 'Sakura', petal: ['#6b3a4a', '#a05d72', '#d58da3', '#f2b5c6', '#fbd3de', '#ffe7ee', '#fff6f9'],
      pollen: ['#7a3d4f', '#c0607e', '#e98aa7', '#ffd0dc', '#fff0f5'], leaf: ['#1e2e1c', '#33502c', '#4f7a3f', '#78a861', '#a9d68f', '#dcf3cf'], center: 0.11,
      layers: [L(5, 0.95, 0.44, 1.32, 0.25, 0.45, 0), L(5, 0.62, 0.34, 1.0, 0.18, 0.3, Math.PI / 5)],
      glow: [251, 196, 214], glow2: [255, 240, 245],
      motes: { count: 34, dir: -1, speed: 0.11, chars: '✿*,', colors: ['#fbd3de', '#f2b5c6', '#ffe7ee'], drift: 0.22 },
    },
    sunflower: {
      label: 'Sunflower', petal: ['#5a3d0a', '#8f6210', '#c98d14', '#f0b52a', '#ffd04d', '#ffe38a', '#fff3c4'],
      pollen: ['#2a1608', '#4a2a10', '#6e4219', '#94602a', '#c08a45'], leaf: ROSE_LEAF, center: 0.3,
      layers: [L(18, 0.98, 0.12, 1.42, 0.3, 0.35, 0), L(16, 0.84, 0.11, 1.25, 0.22, 0.3, Math.PI / 16)],
      glow: [255, 196, 70], glow2: [120, 190, 120],
      motes: { count: 40, dir: 1, speed: 0.06, chars: '.·', colors: ['#ffe38a', '#fff3c4'] },
    },
    marigold: {
      label: 'Marigold', petal: ['#4f1a06', '#7e2c0a', '#b3470f', '#e06a17', '#f58f2a', '#ffb454', '#ffd79a'],
      pollen: ['#4a1d05', '#7a3208', '#a8500f', '#d27a22', '#f0a957'], leaf: ['#1d2a14', '#304420', '#4a6630', '#6e8f45', '#9bb86a', '#cfe2a8'], center: 0.12,
      layers: [L(9, 0.95, 0.24, 1.3, 0.2, 0.7, 0), L(8, 0.8, 0.23, 1.05, 0.16, 0.6, Math.PI / 8), L(8, 0.64, 0.21, 0.78, 0.12, 0.45, Math.PI / 5), L(7, 0.48, 0.19, 0.5, 0.08, 0.3, Math.PI / 3), L(5, 0.32, 0.16, 0.25, 0.04, 0.15, 0.4)],
      glow: [240, 120, 40], glow2: [200, 150, 60],
      motes: { count: 30, dir: -1, speed: 0.09, chars: '❦,\'', colors: ['#e06a17', '#b3470f', '#f0a957'], drift: 0.3 },
    },
    pumpkin: {
      label: 'Pumpkin', petal: ['#3a1305', '#6b2408', '#a33c0c', '#d9601a', '#f27e28', '#ffa04a', '#ffc98a'],
      pollen: ['#120818', '#24102e', '#3c1a4d', '#5c2a73', '#8a49a8'], leaf: ['#13200f', '#22361a', '#34502a', '#4d6e3b', '#6f9152', '#9fba7c'], center: 0.2,
      layers: [L(8, 0.92, 0.3, 1.18, 0.18, 0.25, 0), L(8, 0.7, 0.27, 0.85, 0.12, 0.2, Math.PI / 8), L(6, 0.48, 0.22, 0.5, 0.08, 0.12, 0.3)],
      glow: [242, 126, 40], glow2: [138, 73, 168],
      motes: { count: 26, dir: 1, speed: 0.05, chars: '·.*', colors: ['#8a49a8', '#f27e28'] },
    },
    frost: {
      label: 'Frost', petal: ['#1b2e45', '#2d4d73', '#4a78a6', '#7aa8d4', '#a9cdef', '#d3e9fb', '#f3faff'],
      pollen: ['#5d6f80', '#8fa3b5', '#c2d3e0', '#e6f1f8', '#ffffff'], leaf: ['#16262a', '#24413f', '#3c6660', '#5e938a', '#8fc2b8', '#c9ece5'], center: 0.15,
      layers: [L(6, 1.0, 0.22, 1.25, 0.2, 0.15, 0), L(6, 0.8, 0.2, 0.95, 0.15, 0.1, Math.PI / 6), L(6, 0.58, 0.17, 0.62, 0.1, 0.05, 0), L(6, 0.38, 0.14, 0.32, 0.05, 0.02, Math.PI / 6)],
      glow: [140, 190, 240], glow2: [230, 245, 255],
      motes: { count: 70, dir: -1, speed: 0.13, chars: '*·.❄', colors: ['#ffffff', '#d3e9fb', '#a9cdef'], drift: 0.12 },
    },
  };

  /** Which look fits today's date. */
  function seasonFor(d = new Date()) {
    const m = d.getMonth() + 1, day = d.getDate();
    if (m === 10 && day >= 15) return 'pumpkin';
    if (m === 12 || m === 1) return 'frost';
    if (m === 2) return 'rose';
    if (m >= 3 && m <= 5) return 'sakura';
    if (m >= 6 && m <= 8) return 'sunflower';
    return 'marigold';
  }

  let T = THEMES.rose;
  let PETAL = T.petal, POLLEN = T.pollen, LEAF = T.leaf, LAYERS = T.layers;
  const NU = 34, NV = 15;

  const easeOutCubic = (t) => 1 - Math.pow(1 - t, 3);
  const easeInOutCubic = (t) => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);
  const clamp01 = (t) => Math.max(0, Math.min(1, t));
  const lerp = (a, b, t) => a + (b - a) * t;

  /** Petal samples in petal-local terms; the 3D shape is rebuilt each frame from openness. */
  function buildSamples() {
    const petals = [];
    LAYERS.forEach((Ly, li) => {
      for (let k = 0; k < Ly.n; k++) {
        const phi = (k / Ly.n) * Math.PI * 2 + Ly.twist + li * 0.13;
        const jitter = 0.92 + ((Math.sin(k * 12.9898 + li * 78.233) * 43758.5453) % 1 + 1) % 1 * 0.16;
        petals.push({ L: Ly, li, phi, jitter });
      }
    });
    const pollen = [];
    const N = 1400;
    for (let i = 0; i < N; i++) {
      // Fibonacci dome
      const y = 1 - (i / (N - 1)) * 0.75;
      const r = Math.sqrt(1 - y * y);
      const th = i * 2.399963;
      pollen.push([Math.cos(th) * r, y, Math.sin(th) * r]);
    }
    return { petals, pollen };
  }

  class FlowerBloom {
    constructor(canvas) {
      this.canvas = canvas;
      this.ctx = canvas.getContext('2d');
      this.samples = buildSamples();
      this.state = 'seed'; // seed | blooming | open | scattering | gone
      this.t0 = performance.now();
      this.bloomDur = 5600;
      this.scatterDur = 2400;
      this.spin = 0;
      this.anchor = { x: 0.5, y: 0.52, size: 0.36 };
      this.anchorFrom = { ...this.anchor };
      this.anchorTo = { ...this.anchor };
      this.anchorT0 = 0;
      this.anchorDur = 1;
      this.particles = [];
      this.onBloomed = null;
      this.moteDraw = [];
      this.setTheme('rose');
      this.pointer = { x: 0, y: 0, tx: 0, ty: 0 };
      this.resize();
      window.addEventListener('resize', () => this.resize());
      window.addEventListener('pointermove', (e) => {
        this.pointer.tx = (e.clientX / window.innerWidth - 0.5) * 2;
        this.pointer.ty = (e.clientY / window.innerHeight - 0.5) * 2;
      });
      this.loop = this.loop.bind(this);
      requestAnimationFrame(this.loop);
    }

    resize() {
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      const r = this.canvas.getBoundingClientRect();
      this.w = r.width; this.h = r.height;
      this.canvas.width = Math.round(r.width * dpr);
      this.canvas.height = Math.round(r.height * dpr);
      this.ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      this.fontPx = r.width < 1000 ? 10 : 11;
      this.ctx.font = `500 ${this.fontPx}px "JetBrains Mono", ui-monospace, monospace`;
      this.cw = this.ctx.measureText('M').width;
      this.ch = Math.round(this.fontPx * 1.12);
      this.cols = Math.ceil(this.w / this.cw);
      this.rows = Math.ceil(this.h / this.ch);
      const n = this.cols * this.rows;
      this.zbuf = new Float32Array(n);
      this.lum = new Float32Array(n);
      this.col = new Int16Array(n);
    }

    /** Animate the flower's centre and size (fractions of the canvas). */
    moveTo(x, y, size, dur = 1100) {
      this.anchorFrom = { ...this.currentAnchor() };
      this.anchorTo = { x, y, size };
      this.anchorT0 = performance.now();
      this.anchorDur = dur;
    }

    currentAnchor() {
      const t = easeInOutCubic(clamp01((performance.now() - this.anchorT0) / this.anchorDur));
      return {
        x: lerp(this.anchorFrom.x, this.anchorTo.x, t),
        y: lerp(this.anchorFrom.y, this.anchorTo.y, t),
        size: lerp(this.anchorFrom.size, this.anchorTo.size, t),
      };
    }

    /** Switch seasonal look: 'auto' or a theme key. Rebuilds petal geometry and motes. */
    setTheme(name) {
      const key = name === 'auto' || !THEMES[name] ? seasonFor() : name;
      T = THEMES[key];
      PETAL = T.petal; POLLEN = T.pollen; LEAF = T.leaf; LAYERS = T.layers;
      this.theme = key;
      this.samples = buildSamples();
      this.motes = Array.from({ length: T.motes.count }, () => this.newMote(true));
      return key;
    }

    newMote(anywhere) {
      const M = T.motes;
      const a = Math.random() * Math.PI * 2, r = 0.25 + Math.random() * 1.1;
      const start = M.dir > 0 ? -0.9 : 1.1;
      return {
        x: Math.cos(a) * r, z: Math.sin(a) * r, y: anywhere ? -0.9 + Math.random() * 2.0 : start,
        sp: M.speed * (0.6 + Math.random() * 0.8), ph: Math.random() * 6.28,
        ch: M.chars[(Math.random() * M.chars.length) | 0], color: M.colors[(Math.random() * M.colors.length) | 0],
      };
    }

    bloom() {
      this.state = 'blooming';
      this.t0 = performance.now();
    }

    scatter() {
      if (this.state === 'scattering' || this.state === 'gone') return;
      this.particles = [];
      const pts = this.lastPoints || [];
      for (let i = 0; i < pts.length; i += 2) {
        const p = pts[i];
        this.particles.push({
          x: p[0], y: p[1], z: p[2], c: p[3],
          vx: 0.5 + Math.random() * 0.9, vy: 0.15 + (Math.random() - 0.3) * 0.5, vz: (Math.random() - 0.5) * 0.6,
          delay: Math.max(0, (p[0] + 1.3) * 0.22 + Math.random() * 0.25),
          life: 1.1 + Math.random() * 1.1, seed: Math.random() * 100,
        });
      }
      this.state = 'scattering';
      this.t0 = performance.now();
    }

    /** 0..1 progress of open-ness and scale, given the state. */
    shape(now) {
      if (this.state === 'seed' || this.state === 'gone') return { scale: 0, open: 0, stem: 0 };
      if (this.state === 'blooming') {
        const t = clamp01((now - this.t0) / this.bloomDur);
        const stem = easeOutCubic(clamp01(t / 0.38));
        const scale = easeOutCubic(clamp01((t - 0.12) / 0.5));
        const open = easeInOutCubic(clamp01((t - 0.32) / 0.68));
        if (t >= 1) { this.state = 'open'; if (this.onBloomed) this.onBloomed(); }
        return { scale: 0.04 + scale * 0.96, open, stem };
      }
      return { scale: 1, open: 1, stem: 1 };
    }

    loop(now) {
      requestAnimationFrame(this.loop);
      if (document.hidden) return;
      const dt = Math.min(0.05, (now - (this.last || now)) / 1000);
      this.last = now;
      this.spin += dt * (this.state === 'blooming' ? 0.55 : 0.22);
      this.pointer.x += (this.pointer.tx - this.pointer.x) * 0.04;
      this.pointer.y += (this.pointer.ty - this.pointer.y) * 0.04;

      const { zbuf, lum, col } = this;
      zbuf.fill(0); lum.fill(0); col.fill(-1);

      const a = this.currentAnchor();
      const cx = a.x * this.w, cy = a.y * this.h;
      const K = Math.min(this.w, this.h * 1.6) * a.size * 3.0;
      const tilt = -0.74 + this.pointer.y * 0.08;
      const yaw = this.spin + this.pointer.x * 0.25;
      const cyw = Math.cos(yaw), syw = Math.sin(yaw), ct = Math.cos(tilt), st = Math.sin(tilt);
      const D = 3.4;
      const cols = this.cols, rows = this.rows, cw = this.cw, ch = this.ch;
      // light (in view space)
      const lx = -0.45, ly = 0.75, lz = -0.5, ll = Math.hypot(lx, ly, lz);
      const Lx = lx / ll, Ly = ly / ll, Lz = lz / ll;

      const plot = (x, y, z, nx, ny, nz, cidx, base) => {
        // rotate Y then X
        const x1 = x * cyw + z * syw, z1 = -x * syw + z * cyw;
        const y2 = y * ct - z1 * st, z2 = y * st + z1 * ct;
        const ooz = 1 / (z2 + D);
        const sx = cx + K * x1 * ooz, sy = cy - K * y2 * ooz;
        const c = (sx / cw) | 0, r = (sy / ch) | 0;
        if (c < 0 || r < 0 || c >= cols || r >= rows) return;
        const i = r * cols + c;
        if (ooz <= zbuf[i]) return;
        zbuf[i] = ooz;
        let L = base;
        if (nx !== undefined) {
          const nx1 = nx * cyw + nz * syw, nz1 = -nx * syw + nz * cyw;
          const ny2 = ny * ct - nz1 * st, nz2 = ny * st + nz1 * ct;
          const dd = nx1 * Lx + ny2 * Ly + nz2 * Lz;
          const d = dd >= 0 ? dd : -dd * 0.55;
          L = 0.06 + 0.94 * Math.pow(d, 1.35);
        }
        lum[i] = L; col[i] = cidx;
      };

      const { scale, open, stem } = this.shape(now);
      const capture = this.state === 'open' || this.state === 'blooming';
      const pts = capture ? [] : null;
      const yOff = -0.32;

      if (scale > 0) {
        for (const P of this.samples.petals) {
          const L = P.L;
          const len = L.len * P.jitter * scale;
          const breathe = this.state === 'open' ? Math.sin(now / 1400 + P.phi) * 0.025 : 0;
          const a0 = lerp(L.closed, L.open, open) + breathe;
          const curl = L.curl * open;
          const Rx = Math.cos(P.phi), Rz = Math.sin(P.phi);
          const Tx = -Rz, Tz = Rx;
          let rad = 0.035 * scale + 0.05 * P.li * 0.0, up = 0;
          const du = 1 / NU;
          for (let iu = 0; iu <= NU; iu++) {
            const u = iu * du;
            const ang = a0 + curl * u * u;
            if (iu > 0) { rad += Math.sin(ang) * len * du; up += Math.cos(ang) * len * du; }
            const wu = L.w * scale * Math.pow(Math.sin(Math.PI * Math.pow(Math.max(u, 0.02), 0.62)), 1.15);
            const inR = -Math.cos(ang), inY = Math.sin(ang); // inside normal in radial/up plane
            const cup = 0.55 * (1 - 0.4 * open);
            const shade = Math.min(PETAL.length - 1, Math.max(0, Math.round(1 + u * 4.2 - P.li * 0.35)));
            for (let iv = 0; iv <= NV; iv++) {
              const v = (iv / NV) * 2 - 1;
              const lat = v * wu;
              const lift = v * v * cup * wu;
              const r = rad + inR * lift, yy = up + inY * lift;
              const x = Rx * r + Tx * lat, z = Rz * r + Tz * lat, y = yy + yOff;
              // approximate normal: inside normal tilted by lateral slope
              const s = -2 * v * cup;
              let nx = Rx * inR + Tx * s, ny = inY, nz = Rz * inR + Tz * s;
              const nl = Math.hypot(nx, ny, nz); nx /= nl; ny /= nl; nz /= nl;
              const edge = Math.abs(v) > 0.86 ? Math.max(0, shade - 2) : shade;
              plot(x, y, z, nx, ny, nz, edge);
              if (pts && (iv & 1) === 0 && (iu & 1) === 0) pts.push([x, y, z, shade]);
            }
          }
        }
        // pollen dome
        const pr = T.center * scale, py = yOff + 0.04 * scale;
        for (const p of this.samples.pollen) {
          const x = p[0] * pr, y = py + p[1] * pr * 0.8, z = p[2] * pr;
          const grain = (Math.sin(p[0] * 40 + p[2] * 37) + 1) * 0.5;
          plot(x, y, z, p[0], p[1], p[2], 100 + Math.min(POLLEN.length - 1, Math.round(1 + grain * 3)));
          if (pts) pts.push([x, y, z, 100 + 3]);
        }
      }
      // stem + leaves
      if (stem > 0) {
        const SL = 1.35 * stem, NS = 60, SEG = 12, R = 0.042;
        const stemAt = (s) => [0.08 * Math.sin(s * 2.3), yOff - 0.03 - s * SL, 0.04 * s];
        for (let i = 0; i <= NS; i++) {
          const sv = i / NS, [sx, sy, sz] = stemAt(sv);
          for (let j = 0; j < SEG; j++) {
            const a = (j / SEG) * Math.PI * 2, nx = Math.cos(a), nz = Math.sin(a);
            plot(sx + nx * R, sy, sz + nz * R, nx, 0.1, nz, 203);
            if (pts && (i & 1) === 0 && j % 3 === 0) pts.push([sx + nx * R, sy, sz + nz * R, 202]);
          }
        }
        const leaves = [[0.42, 0.7], [0.66, 0.7 + Math.PI]];
        for (const [at, phi] of leaves) {
          const g = clamp01((stem - at) / (1 - at) * 1.6);
          if (g <= 0) continue;
          const [bx, by, bz] = stemAt(at);
          const Rx = Math.cos(phi), Rz = Math.sin(phi), Tx = -Rz, Tz = Rx;
          const len = 0.5 * g;
          let rad = 0, up = 0;
          for (let iu = 0; iu <= 24; iu++) {
            const u = iu / 24, ang = 0.95 + 0.9 * u * u;
            if (iu > 0) { rad += Math.sin(ang) * len / 24; up += Math.cos(ang) * len / 24; }
            const wu = 0.15 * g * Math.pow(Math.sin(Math.PI * Math.pow(Math.max(u, 0.02), 0.8)), 1.1);
            const inR = -Math.cos(ang), inY = Math.sin(ang);
            for (let iv = 0; iv <= 10; iv++) {
              const v = (iv / 10) * 2 - 1, lift = v * v * 0.35 * wu, lat = v * wu;
              const r = rad + inR * lift;
              const x = bx + Rx * r + Tx * lat, y = by + up + inY * lift, z = bz + Rz * r + Tz * lat;
              const shade = Math.abs(v) < 0.12 ? 201 : Math.min(205, 202 + Math.round(u * 2));
              plot(x, y, z, Rx * inR - Tx * v * 0.7, inY, Rz * inR - Tz * v * 0.7, shade);
              if (pts && (iu & 1) === 0 && (iv & 1) === 0) pts.push([x, y, z, shade]);
            }
          }
        }
      }

      // seasonal motes: pollen rising, petals / leaves / snow falling
      this.moteDraw.length = 0;
      const moteVis = this.state === 'open' ? 1 : this.state === 'blooming' ? clamp01(((now - this.t0) / this.bloomDur - 0.6) / 0.4) : 0;
      if (moteVis > 0) {
        const M = T.motes, drift = M.drift || 0;
        for (let i = 0; i < this.motes.length; i++) {
          const m = this.motes[i];
          m.y += m.sp * dt * M.dir;
          m.x += drift * dt * 0.25;
          if (m.y > 1.1 || m.y < -0.95 || m.x > 1.8) { this.motes[i] = this.newMote(false); continue; }
          const sway = Math.sin(now / 900 + m.ph) * (0.06 + drift * 0.2);
          const fade = Math.min(1, (m.y + 0.95) * 2.5) * Math.min(1, (1.1 - m.y) * 2.5);
          const x = m.x + sway, y = m.y, z = m.z;
          const x1 = x * cyw + z * syw, z1 = -x * syw + z * cyw;
          const y2 = y * ct - z1 * st, z2 = y * st + z1 * ct;
          const ooz = 1 / (z2 + D);
          this.moteDraw.push([cx + K * x1 * ooz, cy - K * y2 * ooz, m.ch, m.color, fade * moteVis * (0.35 + 0.5 * Math.min(1, ooz * 2.6))]);
        }
      }
      if (pts) this.lastPoints = pts;

      if (this.state === 'scattering') {
        const t = (now - this.t0) / 1000;
        let alive = 0;
        for (const q of this.particles) {
          const tt = t - q.delay;
          if (tt < 0) { plot(q.x, q.y, q.z, undefined, 0, 0, q.c, 0.75); alive++; continue; }
          const k = tt / q.life;
          if (k >= 1) continue;
          alive++;
          const sw = Math.sin(tt * 3 + q.seed) * 0.12;
          const x = q.x + q.vx * tt * 1.4 + tt * tt * 0.5;
          const y = q.y + q.vy * tt + sw;
          const z = q.z + q.vz * tt;
          plot(x, y, z, undefined, 0, 0, q.c, 0.95 * (1 - k));
        }
        if (alive === 0 || t * 1000 > this.scatterDur + 1200) { this.state = 'gone'; this.particles = []; }
      }

      this.draw(now);
    }

    draw(now) {
      const g = this.ctx;
      g.clearRect(0, 0, this.w, this.h);
      g.textBaseline = 'top';
      const an = this.currentAnchor();
      const shapeNow = this.state === 'open' ? 1 : this.state === 'blooming' ? clamp01((now - this.t0) / this.bloomDur) : 0;
      if (shapeNow > 0) {
        const gx = an.x * this.w, gy = an.y * this.h, gr = Math.min(this.w, this.h) * an.size * 2.2;
        const grad = g.createRadialGradient(gx, gy, 0, gx, gy, gr);
        grad.addColorStop(0, `rgba(${T.glow.join(',')}, ${0.13 * shapeNow})`);
        grad.addColorStop(0.5, `rgba(${T.glow2.join(',')}, ${0.04 * shapeNow})`);
        grad.addColorStop(1, `rgba(${T.glow2.join(',')}, 0)`);
        g.fillStyle = grad;
        g.beginPath(); g.arc(gx, gy, gr, 0, Math.PI * 2); g.fill();
      }
      const { cols, rows, cw, ch, lum, col } = this;
      const n = cols * rows;

      // bucket by colour so fillStyle changes stay few
      const buckets = new Map();
      for (let i = 0; i < n; i++) {
        const c = col[i];
        if (c < 0 || lum[i] <= 0.02) continue;
        let b = buckets.get(c);
        if (!b) { b = []; buckets.set(c, b); }
        b.push(i);
      }
      for (const [c, list] of buckets) {
        g.fillStyle = c >= 200 ? LEAF[c - 200] : c >= 100 ? POLLEN[c - 100] : PETAL[c];
        for (const i of list) {
          const ri = Math.min(RAMP.length - 1, Math.max(1, Math.round(lum[i] * (RAMP.length - 1))));
          g.fillText(RAMP[ri], (i % cols) * cw, ((i / cols) | 0) * ch);
        }
      }

      for (const [mx, my, ch, color, alpha] of this.moteDraw) {
        if (alpha <= 0.02) continue;
        g.globalAlpha = alpha;
        g.fillStyle = color;
        g.fillText(ch, mx, my);
      }
      g.globalAlpha = 1;

      if (this.state === 'seed' || this.state === 'gone') {
        const a = this.currentAnchor();
        const pulse = 0.55 + Math.sin(now / 420) * 0.35;
        g.globalAlpha = pulse;
        g.fillStyle = PETAL[4];
        const sx = Math.round(a.x * this.w / cw) * cw, sy = Math.round((a.y * this.h) / ch) * ch;
        g.fillText('•', sx, sy);
        g.globalAlpha = pulse * 0.4;
        g.fillText('.', sx - cw, sy);
        g.fillText('.', sx + cw, sy);
        g.globalAlpha = 1;
      }
    }
  }

  FlowerBloom.THEMES = Object.fromEntries(Object.entries(THEMES).map(([k, v]) => [k, v.label]));
  FlowerBloom.seasonFor = seasonFor;
  window.FlowerBloom = FlowerBloom;
})();
