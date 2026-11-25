package dev.flowerclient.gui;

import net.minecraft.client.gui.DrawContext;

/**
 * Shared colours and drawing helpers for Flower's in-game UI. Everything is built from solid
 * fills and vertical gradients (all the DrawContext offers), but with real rounded corners,
 * soft shadows and sheens so panels read as glass rather than flat boxes.
 */
public final class Theme {
    public static final int ACCENT = 0xFFF4A6C0;      // blush petal
    public static final int ACCENT_DEEP = 0xFFE85D8A; // rose
    public static final int ACCENT_SOFT = 0xFFF7C3D4;
    public static final int LEAF = 0xFF9FD8B4;
    public static final int POLLEN = 0xFFFFD678;
    public static final int TEXT = 0xFFF5EEF1;
    public static final int MUTED = 0xFF9C8F96;

    // HUD glass (top → bottom of the subtle gradient)
    public static final int HUD_TOP = 0xCC1B1621;
    public static final int HUD_BOT = 0xCC0E0A12;
    public static final int HUD_BG = HUD_TOP; // kept for callers that want one colour

    // Menu glass (clear, over the blurred world)
    public static final int PANEL = 0xF0141015;
    public static final int PANEL_2 = 0xFF1D181E;
    public static final int PANEL_3 = 0xFF2A232B;
    public static final int LINE = 0xFF33293A;
    public static final int GLASS_TOP = 0x66201826;
    public static final int GLASS_BOT = 0x5A120D16;
    public static final int GLASS = 0x5A140F16;
    public static final int GLASS_EDGE = 0x40FFFFFF;
    public static final int GLASS_SHINE = 0x2EFFFFFF;
    public static final int GLASS_TILE_TOP = 0x22FFFFFF;
    public static final int GLASS_TILE_BOT = 0x14FFFFFF;
    public static final int GLASS_TILE = 0x18FFFFFF;
    public static final int GLASS_HOVER_TOP = 0x3CFFFFFF;
    public static final int GLASS_HOVER_BOT = 0x24FFFFFF;
    public static final int GLASS_HOVER = 0x2EFFFFFF;
    public static final int GLASS_OFF = 0x40FFFFFF;

    /** HUD accent. Biome Tint changes this every tick; otherwise it stays the blush pink. */
    public static int hudAccent = ACCENT;

    private Theme() {}

    // ---- rounded primitives -------------------------------------------------

    /** Horizontal inset of a rounded corner at row `i` of a box `h` tall, radius `r`. */
    private static int inset(int r, int i, int h) {
        int d = i < r ? i : (i >= h - r ? h - 1 - i : -1);
        if (d < 0 || r <= 0) return 0;
        int e = r - d - 1;                       // distance from the very edge
        return (int) Math.ceil(r - Math.sqrt(Math.max(0, (double) r * r - (double) e * e)));
    }

    public static void roundedRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int i = 0; i < h; i++) {
            int k = inset(r, i, h);
            ctx.fill(x + k, y + i, x + w - k, y + i + 1, color);
        }
    }

    public static void roundedGradient(DrawContext ctx, int x, int y, int w, int h, int r, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int i = 0; i < h; i++) {
            int k = inset(r, i, h);
            int c = lerpColor(top, bottom, h <= 1 ? 0 : i / (float) (h - 1));
            ctx.fill(x + k, y + i, x + w - k, y + i + 1, c);
        }
    }

    /** 1px rounded ring, drawn over whatever is already there. */
    public static void roundedOutline(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int i = 0; i < h; i++) {
            int k = inset(r, i, h);
            boolean cap = i == 0 || i == h - 1;
            if (cap) {
                ctx.fill(x + k, y + i, x + w - k, y + i + 1, color);
            } else {
                ctx.fill(x + k, y + i, x + k + 1, y + i + 1, color);
                ctx.fill(x + w - k - 1, y + i, x + w - k, y + i + 1, color);
            }
        }
    }

    /** Soft drop shadow behind a rounded box. */
    public static void shadow(DrawContext ctx, int x, int y, int w, int h, int r) {
        roundedRect(ctx, x - 1, y + 1, w + 2, h + 2, r + 1, 0x26000000);
        roundedRect(ctx, x - 2, y + 2, w + 4, h + 3, r + 2, 0x16000000);
    }

    /** Thin top sheen line that follows the top rounded edge. */
    public static void sheen(DrawContext ctx, int x, int y, int w, int r, int color) {
        int k = (int) Math.ceil(r - Math.sqrt(Math.max(0, (double) r * r - (double) (r - 1) * (r - 1))));
        ctx.fill(x + k, y, x + w - k, y + 1, color);
    }

    // ---- legacy helpers (still used in a few spots) ------------------------

    public static void panel(DrawContext ctx, int x, int y, int w, int h, int color) {
        roundedRect(ctx, x, y, w, h, Math.min(2, Math.min(w, h) / 2), color);
    }

    public static void round(DrawContext ctx, int x, int y, int w, int h, int color) {
        roundedRect(ctx, x, y, w, h, 3, color);
    }

    public static void outline(DrawContext ctx, int x, int y, int w, int h, int color) {
        roundedOutline(ctx, x, y, w, h, 2, color);
    }

    /** HUD box: shadow, subtle vertical glass gradient, top sheen and a rounded accent tick. */
    public static void hudPanel(DrawContext ctx, int x, int y, int w, int h) {
        shadow(ctx, x, y, w, h, 2);
        roundedGradient(ctx, x, y, w, h, 2, HUD_TOP, HUD_BOT);
        sheen(ctx, x, y, w, 2, 0x1CFFFFFF);
        roundedRect(ctx, x + 2, y + 3, 2, Math.max(1, h - 6), 1, withAlpha(hudAccent, 0.95f));
    }

    // ---- colour maths ------------------------------------------------------

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
