package dev.flowerclient.render;

import net.minecraft.client.gui.DrawContext;

/**
 * The launcher's ASCII bloom, redrawn in-game as a "pixel bloom": a small 3D flower
 * (layered cupped petals, pollen dome, stem and two leaves) projected onto a grid of
 * GUI pixels with a depth buffer and simple lighting. Pure fills, so it works even
 * before fonts and resource packs have loaded.
 */
public final class FlowerArt {
    // outside-in: count, length, width, open angle, closed angle, tip curl, twist
    private static final float[][] LAYERS = {
            {6, 1.0f, 0.33f, 1.22f, 0.22f, 0.5f, 0f},
            {6, 0.8f, 0.29f, 0.9f, 0.16f, 0.35f, (float) (Math.PI / 6)},
            {5, 0.6f, 0.25f, 0.58f, 0.1f, 0.22f, (float) (Math.PI / 5)},
            {4, 0.42f, 0.21f, 0.3f, 0.05f, 0.1f, (float) (Math.PI / 4)},
    };
    private static final int[] PETAL = {0x5a2236, 0x8a2f52, 0xb8436f, 0xe0608f, 0xf08cb0, 0xf7b6cc, 0xfde0ea};
    private static final int[] POLLEN = {0x6b4a16, 0xb07d22, 0xf0b545, 0xffd678, 0xfff0c2};
    private static final int[] LEAF = {0x16301f, 0x24503a, 0x3a7a55, 0x5fae80, 0x9fd8b4, 0xd4f5df};
    private static final int NU = 22, NV = 10;

    private static float[] zbuf = new float[0];
    private static int[] colour = new int[0];

    private FlowerArt() {}

    // projection state for the current frame
    private static float cyw, syw, ct, st, K, ox, oy, lx, ly, lz;
    private static int gx0, gy0, gw, gh, cell;
    private static final float D = 3.4f;

    /**
     * @param cx,cy  centre of the flower head in GUI pixels
     * @param radius rough radius of the open flower in GUI pixels
     * @param cellPx size of one "pixel" of the bloom in GUI pixels (1 or 2)
     * @param timeSec seconds since the animation started (drives spin)
     * @param grow   0..1 how far the bloom has grown (stem → bud → open)
     * @param alpha  0..1 overall opacity
     */
    public static void draw(DrawContext ctx, float cx, float cy, float radius, int cellPx, float timeSec, float grow, float alpha) {
        if (alpha <= 0.01f) return;
        cell = Math.max(1, cellPx);
        gx0 = (int) (cx - radius * 1.7f); gy0 = (int) (cy - radius * 1.5f);
        gw = (int) Math.ceil(radius * 3.4f / cell) + 1; gh = (int) Math.ceil(radius * 3.7f / cell) + 1;
        int n = gw * gh;
        if (zbuf.length < n) { zbuf = new float[n]; colour = new int[n]; }
        java.util.Arrays.fill(zbuf, 0, n, 0f);
        java.util.Arrays.fill(colour, 0, n, 0);

        float yaw = timeSec * 0.3f, tilt = -0.74f;
        cyw = (float) Math.cos(yaw); syw = (float) Math.sin(yaw);
        ct = (float) Math.cos(tilt); st = (float) Math.sin(tilt);
        K = radius * 3.1f; ox = cx; oy = cy;
        float ll = (float) Math.sqrt(0.45 * 0.45 + 0.75 * 0.75 + 0.5 * 0.5);
        lx = -0.45f / ll; ly = 0.75f / ll; lz = -0.5f / ll;

        float stem = ease(clamp(grow / 0.38f));
        float scale = 0.04f + 0.96f * ease(clamp((grow - 0.12f) / 0.5f));
        float open = easeInOut(clamp((grow - 0.32f) / 0.68f));
        float yOff = -0.32f;

        // petals
        for (int li = 0; li < LAYERS.length; li++) {
            float[] L = LAYERS[li];
            int count = (int) L[0];
            for (int k = 0; k < count; k++) {
                float phi = (float) (k / (double) count * Math.PI * 2) + L[6] + li * 0.13f;
                float jitter = 0.92f + frac((float) Math.sin(k * 12.9898f + li * 78.233f) * 43758.5453f) * 0.16f;
                float len = L[1] * jitter * scale;
                float breathe = grow >= 1 ? (float) Math.sin(timeSec * 0.7f + phi) * 0.025f : 0f;
                float a0 = L[4] + (L[3] - L[4]) * open + breathe;
                float curl = L[5] * open;
                float Rx = (float) Math.cos(phi), Rz = (float) Math.sin(phi), Tx = -Rz, Tz = Rx;
                float rad = 0.035f * scale, up = 0;
                for (int iu = 0; iu <= NU; iu++) {
                    float u = iu / (float) NU;
                    float ang = a0 + curl * u * u;
                    if (iu > 0) { rad += (float) Math.sin(ang) * len / NU; up += (float) Math.cos(ang) * len / NU; }
                    float wu = L[2] * scale * (float) Math.pow(Math.sin(Math.PI * Math.pow(Math.max(u, 0.02f), 0.62f)), 1.15f);
                    float inR = (float) -Math.cos(ang), inY = (float) Math.sin(ang);
                    float cup = 0.55f * (1 - 0.4f * open);
                    int shade = Math.max(0, Math.min(PETAL.length - 1, Math.round(1 + u * 4.2f - li * 0.35f)));
                    for (int iv = 0; iv <= NV; iv++) {
                        float v = iv / (float) NV * 2 - 1;
                        float lat = v * wu, lift = v * v * cup * wu;
                        float r = rad + inR * lift, yy = up + inY * lift;
                        float s = -2 * v * cup;
                        float nx = Rx * inR + Tx * s, ny = inY, nz = Rz * inR + Tz * s;
                        int base = PETAL[Math.abs(v) > 0.86f ? Math.max(0, shade - 2) : shade];
                        plot(Rx * r + Tx * lat, yy + yOff, Rz * r + Tz * lat, nx, ny, nz, base);
                    }
                }
            }
        }
        // pollen dome
        float pr = 0.17f * scale, py = yOff + 0.04f * scale;
        for (int i = 0; i < 520; i++) {
            float y = 1 - i / 519f * 0.75f, r = (float) Math.sqrt(1 - y * y), th = i * 2.399963f;
            float x = (float) Math.cos(th) * r, z = (float) Math.sin(th) * r;
            float grain = ((float) Math.sin(x * 40 + z * 37) + 1) * 0.5f;
            plot(x * pr, py + y * pr * 0.8f, z * pr, x, y, z, POLLEN[Math.min(POLLEN.length - 1, Math.round(1 + grain * 3))]);
        }
        // stem
        if (stem > 0) {
            float SL = 1.35f * stem, R = 0.042f;
            for (int i = 0; i <= 40; i++) {
                float sv = i / 40f;
                float sx = 0.08f * (float) Math.sin(sv * 2.3f), sy = yOff - 0.03f - sv * SL, sz = 0.04f * sv;
                for (int j = 0; j < 10; j++) {
                    float a = (float) (j / 10.0 * Math.PI * 2), nx = (float) Math.cos(a), nz = (float) Math.sin(a);
                    plot(sx + nx * R, sy, sz + nz * R, nx, 0.1f, nz, LEAF[3]);
                }
            }
            float[][] leaves = {{0.42f, 0.7f}, {0.66f, 0.7f + (float) Math.PI}};
            for (float[] lf : leaves) {
                float g = clamp((stem - lf[0]) / (1 - lf[0]) * 1.6f);
                if (g <= 0) continue;
                float at = lf[0];
                float bx = 0.08f * (float) Math.sin(at * 2.3f), by = yOff - 0.03f - at * SL, bz = 0.04f * at;
                float Rx = (float) Math.cos(lf[1]), Rz = (float) Math.sin(lf[1]), Tx = -Rz, Tz = Rx;
                float len = 0.5f * g, rad = 0, up = 0;
                for (int iu = 0; iu <= 18; iu++) {
                    float u = iu / 18f, ang = 0.95f + 0.9f * u * u;
                    if (iu > 0) { rad += (float) Math.sin(ang) * len / 18; up += (float) Math.cos(ang) * len / 18; }
                    float wu = 0.15f * g * (float) Math.pow(Math.sin(Math.PI * Math.pow(Math.max(u, 0.02f), 0.8f)), 1.1f);
                    float inR = (float) -Math.cos(ang), inY = (float) Math.sin(ang);
                    for (int iv = 0; iv <= 8; iv++) {
                        float v = iv / 8f * 2 - 1, lift = v * v * 0.35f * wu, lat = v * wu, r = rad + inR * lift;
                        int base = Math.abs(v) < 0.15f ? LEAF[2] : LEAF[Math.min(5, 3 + Math.round(u * 1.5f))];
                        plot(bx + Rx * r + Tx * lat, by + up + inY * lift, bz + Rz * r + Tz * lat,
                                Rx * inR - Tx * v * 0.7f, inY, Rz * inR - Tz * v * 0.7f, base);
                    }
                }
            }
        }

        // emit: merge horizontal runs of identical colour into single fills
        int a = Math.round(alpha * 255) << 24;
        for (int r = 0; r < gh; r++) {
            int row = r * gw, c = 0;
            while (c < gw) {
                int col = colour[row + c];
                if (col == 0) { c++; continue; }
                int start = c;
                while (c < gw && colour[row + c] == col) c++;
                int x1 = gx0 + start * cell, y1 = gy0 + r * cell;
                ctx.fill(x1, y1, gx0 + c * cell, y1 + cell, a | (col & 0xFFFFFF));
            }
        }
    }

    private static void plot(float x, float y, float z, float nx, float ny, float nz, int base) {
        float x1 = x * cyw + z * syw, z1 = -x * syw + z * cyw;
        float y2 = y * ct - z1 * st, z2 = y * st + z1 * ct;
        float ooz = 1 / (z2 + D);
        float sx = ox + K * x1 * ooz, sy = oy - K * y2 * ooz;
        int c = (int) Math.floor((sx - gx0) / cell), r = (int) Math.floor((sy - gy0) / cell);
        if (c < 0 || r < 0 || c >= gw || r >= gh) return;
        int i = r * gw + c;
        if (ooz <= zbuf[i]) return;
        zbuf[i] = ooz;
        float nx1 = nx * cyw + nz * syw, nz1 = -nx * syw + nz * cyw;
        float ny2 = ny * ct - nz1 * st, nz2 = ny * st + nz1 * ct;
        float d = nx1 * lx + ny2 * ly + nz2 * lz;
        d = d >= 0 ? d : -d * 0.55f;
        float lum = 0.3f + 0.7f * (float) Math.pow(Math.min(1, d), 1.2);
        lum = Math.round(lum * 6) / 6f; // quantise so runs merge (and it reads as pixel art)
        int rr = Math.min(255, Math.round(((base >> 16) & 255) * lum));
        int gg = Math.min(255, Math.round(((base >> 8) & 255) * lum));
        int bb = Math.min(255, Math.round((base & 255) * lum));
        colour[i] = 0x01000000 | rr << 16 | gg << 8 | bb; // non-zero marker even for black
    }

    private static float clamp(float t) { return Math.max(0, Math.min(1, t)); }
    private static float ease(float t) { return 1 - (1 - t) * (1 - t) * (1 - t); }
    private static float easeInOut(float t) { return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2; }
    private static float frac(float v) { return v - (float) Math.floor(v); }
}
