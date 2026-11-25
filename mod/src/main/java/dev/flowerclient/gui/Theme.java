package dev.flowerclient.gui;

import net.minecraft.client.gui.DrawContext;

public final class Theme {
    public static final int ACCENT = 0xFFF4A6C0;      // blush petal
    public static final int ACCENT_DEEP = 0xFFE85D8A; // rose
    public static final int ACCENT_SOFT = 0xFFF7C3D4;
    public static final int POLLEN = 0xFFFFD678;
    public static final int TEXT = 0xFFF5EEF1;
    public static final int MUTED = 0xFF9C8F96;
    public static final int HUD_BG = 0x90141015;
    public static final int PANEL = 0xF0141015;
    public static final int PANEL_2 = 0xFF1D181E;
    public static final int PANEL_3 = 0xFF2A232B;
    public static final int LINE = 0xFF33293A;

    private Theme() {}

    /** Filled rectangle with 1px-chamfered corners — reads as rounded at GUI scale. */
    public static void panel(DrawContext ctx, int x, int y, int w, int h, int color) {
        if (w <= 2 || h <= 2) { ctx.fill(x, y, x + w, y + h, color); return; }
        ctx.fill(x + 1, y, x + w - 1, y + h, color);
        ctx.fill(x, y + 1, x + 1, y + h - 1, color);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** 2px-rounded panel for larger surfaces. */
    public static void round(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x + 2, y, x + w - 2, y + h, color);
        ctx.fill(x + 1, y + 1, x + 2, y + h - 1, color);
        ctx.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, color);
        ctx.fill(x, y + 2, x + 1, y + h - 2, color);
        ctx.fill(x + w - 1, y + 2, x + w, y + h - 2, color);
    }

    public static void outline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x + 1, y, x + w - 1, y + 1, color);
        ctx.fill(x + 1, y + h - 1, x + w - 1, y + h, color);
        ctx.fill(x, y + 1, x + 1, y + h - 1, color);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    public static int lerpColor(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int aa = a >>> 24, ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int ba = b >>> 24, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return (int) (aa + (ba - aa) * t) << 24 | (int) (ar + (br - ar) * t) << 16
                | (int) (ag + (bg - ag) * t) << 8 | (int) (ab + (bb - ab) * t);
    }

    public static int withAlpha(int color, float alpha) {
        int a = (int) ((color >>> 24) * Math.max(0, Math.min(1, alpha)));
        return a << 24 | (color & 0xFFFFFF);
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}
