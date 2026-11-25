package dev.flowerclient.module.impl;

import dev.flowerclient.gui.Theme;
import dev.flowerclient.module.HudModule;
import dev.flowerclient.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;

/** WASD + mouse + space. Keys are little glass tiles that fill with the accent colour when pressed. */
public class KeystrokesModule extends HudModule {
    private static final int K = 18, G = 2, M = 16, SP = 8;
    private static final int DARK = 0xFF1A1418;
    private final float[] glow = new float[7];

    public KeystrokesModule() {
        super("Keystrokes", "WASD, mouse and space display", true, 0, 0);
        background = false;
    }

    @Override public int width(MinecraftClient mc) { return K * 3 + G * 2; }
    @Override public int height(MinecraftClient mc) { return K * 2 + G + G + M + G + SP; }

    @Override
    public void render(DrawContext ctx, MinecraftClient mc) {
        var o = mc.options;
        int w = width(mc);
        key(ctx, mc, 0, o.forwardKey, K + G, 0, K, K, "W");
        key(ctx, mc, 1, o.leftKey, 0, K + G, K, K, "A");
        key(ctx, mc, 2, o.backKey, K + G, K + G, K, K, "S");
        key(ctx, mc, 3, o.rightKey, (K + G) * 2, K + G, K, K, "D");
        int my = (K + G) * 2, mw = (w - G) / 2;
        mouse(ctx, mc, 4, o.attackKey, 0, my, mw, ModuleManager.CPS.leftCps(), "LMB");
        mouse(ctx, mc, 5, o.useKey, mw + G, my, w - mw - G, ModuleManager.CPS.rightCps(), "RMB");
        int sy = my + M + G;
        tile(ctx, 6, o.jumpKey, 0, sy, w, SP);
        int bar = w / 3;
        ctx.fill(bar, sy + SP / 2, w - bar, sy + SP / 2 + 1, glow[6] > 0.5f ? DARK : 0xCCF5EEF1);
    }

    private int tile(DrawContext ctx, int i, KeyBinding kb, int x, int y, int w, int h) {
        glow[i] += ((kb.isPressed() ? 1f : 0f) - glow[i]) * 0.4f;
        float g = glow[i];
        Theme.shadow(ctx, x, y, w, h, 2);
        int top = Theme.lerpColor(0xC8201A24, Theme.lerpColor(Theme.hudAccent, 0xFFFFFFFF, 0.22f), g);
        int bot = Theme.lerpColor(0xC8100B16, Theme.hudAccent, g);
        Theme.roundedGradient(ctx, x, y, w, h, 2, top, bot);
        Theme.sheen(ctx, x, y, w, 2, Theme.withAlpha(0xFFFFFFFF, 0.12f + 0.28f * g));
        return g > 0.5f ? DARK : Theme.TEXT;
    }

    private void key(DrawContext ctx, MinecraftClient mc, int i, KeyBinding kb, int x, int y, int w, int h, String label) {
        int col = tile(ctx, i, kb, x, y, w, h);
        int tw = mc.textRenderer.getWidth(label);
        ctx.drawText(mc.textRenderer, label, x + (w - tw) / 2 + 1, y + (h - 8) / 2 + 1, col, false);
    }

    private void mouse(DrawContext ctx, MinecraftClient mc, int i, KeyBinding kb, int x, int y, int w, int cps, String label) {
        int col = tile(ctx, i, kb, x, y, w, M);
        String text = cps > 0 ? cps + " cps" : label;
        int tw = mc.textRenderer.getWidth(text);
        ctx.drawText(mc.textRenderer, text, x + (w - tw) / 2 + 1, y + (M - 8) / 2 + 1, col, false);
    }
}
