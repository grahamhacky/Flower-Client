/*
 * Flower Client — ASCII bloom.
 * A procedurally built 3D flower (layered, cupped petals around a pollen dome) projected
 * into a character grid with a z-buffer, shaded by luminance. It blooms from a seed,
 * slowly turns, and can scatter into drifting particles and grow back.
 */
'use strict';

(function () {
  const RAMP = ' .,:-=+*#%@';
  const PETAL = ['#5a2236', '#8a2f52', '#b8436f', '#e0608f', '#f08cb0', '#f7b6cc', '#fde0ea'];
  const POLLEN = ['#6b4a16', '#b07d22', '#f0b545', '#ffd678', '#fff0c2'];
  const SEED = '#f4a6c0';

  // Layers from outside in: count, length, width, open angle (rad from vertical), closed angle, tip curl, twist
  const LAYERS = [
    { n: 6, len: 1.0, w: 0.33, open: 1.22, closed: 0.22, curl: 0.5, twist: 0 },
    { n: 6, len: 0.8, w: 0.29, open: 0.9, closed: 0.16, curl: 0.35, twist: Math.PI / 6 },
    { n: 5, len: 0.6, w: 0.25, open: 0.58, closed: 0.1, curl: 0.22, twist: Math.PI / 5 },
    { n: 4, len: 0.42, w: 0.21, open: 0.3, closed: 0.05, curl: 0.1, twist: Math.PI / 4 },
  ];
  const NU = 34, NV = 15;

  const easeOutCubic = (t) => 1 - Math.pow(1 - t, 3);
  const easeInOutCubic = (t) => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);
  const clamp01 = (t) => Math.max(0, Math.min(1, t));
  const lerp = (a, b, t) => a + (b - a) * t;

  /** Petal samples in petal-local terms; the 3D shape is rebuilt each frame from openness. */
  function buildSamples() {
    const petals = [];
    LAYERS.forEach((L, li) => {
      for (let k = 0; k < L.n; k++) {
        const phi = (k / L.n) * Math.PI * 2 + L.twist + li * 0.13;
        const jitter = 0.92 + ((Math.sin(k * 12.9898 + li * 78.233) * 43758.5453) % 1 + 1) % 1 * 0.16;
        petals.push({ L, li, phi, jitter });
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
      this.bloomDur = 2600;
      this.scatterDur = 2400;
      this.spin = 0;
      this.anchor = { x: 0.5, y: 0.52, size: 0.36 };
      this.anchorFrom = { ...this.anchor };
      this.anchorTo = { ...this.anchor };
      this.anchorT0 = 0;
      this.anchorDur = 1;
      this.particles = [];
      this.onBloomed = null;
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
      if (this.state === 'seed' || this.state === 'gone') return { scale: 0, open: 0 };
      if (this.state === 'blooming') {
        const t = clamp01((now - this.t0) / this.bloomDur);
        const scale = easeOutCubic(clamp01(t / 0.55));
        const open = easeInOutCubic(clamp01((t - 0.18) / 0.82));
        if (t >= 1) { this.state = 'open'; if (this.onBloomed) this.onBloomed(); }
        return { scale: 0.04 + scale * 0.96, open };
      }
      return { scale: 1, open: 1 };
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
      const tilt = -0.98 + this.pointer.y * 0.1;
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

      const { scale, open } = this.shape(now);
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
        const pr = 0.17 * scale, py = yOff + 0.04 * scale;
        for (const p of this.samples.pollen) {
          const x = p[0] * pr, y = py + p[1] * pr * 0.8, z = p[2] * pr;
          const grain = (Math.sin(p[0] * 40 + p[2] * 37) + 1) * 0.5;
          plot(x, y, z, p[0], p[1], p[2], 100 + Math.min(POLLEN.length - 1, Math.round(1 + grain * 3)));
          if (pts) pts.push([x, y, z, 100 + 3]);
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
        g.fillStyle = c >= 100 ? POLLEN[c - 100] : PETAL[c];
        for (const i of list) {
          const ri = Math.min(RAMP.length - 1, Math.max(1, Math.round(lum[i] * (RAMP.length - 1))));
          g.fillText(RAMP[ri], (i % cols) * cw, ((i / cols) | 0) * ch);
        }
      }

      if (this.state === 'seed' || this.state === 'gone') {
        const a = this.currentAnchor();
        const pulse = 0.55 + Math.sin(now / 420) * 0.35;
        g.globalAlpha = pulse;
        g.fillStyle = SEED;
        const sx = Math.round(a.x * this.w / cw) * cw, sy = Math.round((a.y * this.h) / ch) * ch;
        g.fillText('•', sx, sy);
        g.globalAlpha = pulse * 0.4;
        g.fillText('.', sx - cw, sy);
        g.fillText('.', sx + cw, sy);
        g.globalAlpha = 1;
      }
    }
  }

  window.FlowerBloom = FlowerBloom;
})();
