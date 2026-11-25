package dev.flowerclient.render;

import dev.flowerclient.gui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** The full-screen Flower loading screen: pixel bloom, wordmark, progress line and an optional label. */
public final class FlowerLoading {
    public static final int BG = 0x0B090C;

    private FlowerLoading() {}

    /**
     * @param progress 0..1, or negative for an indeterminate bar
     * @param label    text under the bar, or null (fonts are not ready during the first splash)
     * @param elapsedMs time since this loading screen appeared
     */
    public static void render(DrawContext ctx, int w, int h, float progress, String label, float alpha, long elapsedMs) {
        if (alpha <= 0.003f) return;
        int a = Math.round(alpha * 255);
        ctx.fill(0, 0, w, h, a << 24 | BG);

        // soft glow behind the flower
        int cx = w / 2, cy = Math.round(h * 0.40f);
        float radius = Math.min(w, h) * 0.15f;
        for (int i = 6; i >= 1; i--) disc(ctx, cx, cy, Math.round(radius * (0.6f + i * 0.3f)), Math.round(alpha * 6) << 24 | 0xE85D8A);

        float t = elapsedMs / 1000f;
        float grow = Math.min(1f, t / 4.2f);
        FlowerArt.draw(ctx, cx, cy, radius, radius > 40 ? 2 : 1, t, grow, alpha);

        int wy = Math.round(cy + radius * 2.25f);
        int ww = Math.max(90, Math.min(170, w / 4));
        FlowerTextures.wordmark(ctx, cx, wy, ww, alpha * Math.min(1f, Math.max(0f, (t - 0.6f) / 1.2f)));

        // progress line: pink → mint
        int bw = Math.min(160, w / 3), bx = cx - bw / 2, by = wy + Math.round(ww * 0.2f) + 4;
        ctx.fill(bx, by, bx + bw, by + 1, Math.round(alpha * 40) << 24 | 0xFFFFFF);
        if (progress >= 0) {
            int fw = Math.round(bw * Math.min(1f, progress));
            gradient(ctx, bx, by, fw, bw, alpha);
        } else {
            float p = (t * 0.6f) % 1f;
            int seg = bw / 3, start = Math.round((bw + seg) * p) - seg;
            int s = Math.max(0, start), e = Math.min(bw, start + seg);
            if (e > s) gradientRange(ctx, bx, by, s, e, bw, alpha);
        }

        if (label != null && !label.isEmpty()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.textRenderer != null) {
                int tw = mc.textRenderer.getWidth(label);
                ctx.drawText(mc.textRenderer, label, cx - tw / 2, by + 8, Math.round(alpha * 255) << 24 | (Theme.MUTED & 0xFFFFFF), false);
            }
        }
    }

    /** Filled circle from horizontal strips (2px tall to keep the fill count low). */
    static void disc(DrawContext ctx, int cx, int cy, int r, int argb) {
        for (int dy = -r; dy < r; dy += 2) {
            int half = (int) Math.sqrt(Math.max(0, r * r - (dy + 1) * (dy + 1)));
            ctx.fill(cx - half, cy + dy, cx + half, cy + dy + 2, argb);
        }
    }

    private static void gradient(DrawContext ctx, int bx, int by, int fw, int bw, float alpha) {
        gradientRange(ctx, bx, by, 0, fw, bw, alpha);
    }

    private static void gradientRange(DrawContext ctx, int bx, int by, int from, int to, int bw, float alpha) {
        int a = Math.round(alpha * 255) << 24;
        for (int x = from; x < to; x += 2) {
            float k = x / (float) Math.max(1, bw);
            int col = Theme.lerpColor(0xFFE85D8A, 0xFF9FD8B4, k) & 0xFFFFFF;
            ctx.fill(bx + x, by, bx + Math.min(to, x + 2), by + 1, a | col);
        }
    }
}
