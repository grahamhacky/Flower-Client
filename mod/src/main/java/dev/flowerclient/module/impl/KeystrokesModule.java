package dev.flowerclient.module.impl;

import dev.flowerclient.gui.Theme;
import dev.flowerclient.module.HudModule;
import dev.flowerclient.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;

public class KeystrokesModule extends HudModule {
    private static final int K = 20, G = 2;
    private final float[] glow = new float[7];

    public KeystrokesModule() {
        super("Keystrokes", "WASD, mouse and space display", true, 4, 40);
        background = false;
    }

    @Override public int width(MinecraftClient mc) { return K * 3 + G * 2; }
    @Override public int height(MinecraftClient mc) { return K * 3 + G * 3 + 18 + 10; }

    @Override
    public void render(DrawContext ctx, MinecraftClient mc) {
        var o = mc.options;
        key(ctx, mc, 0, o.forwardKey, K + G, 0, K, K, "W");
        key(ctx, mc, 1, o.leftKey, 0, K + G, K, K, "A");
        key(ctx, mc, 2, o.backKey, K + G, K + G, K, K, "S");
        key(ctx, mc, 3, o.rightKey, (K + G) * 2, K + G, K, K, "D");
        int mw = (width(mc) - G) / 2, my = (K + G) * 2;
        key(ctx, mc, 4, o.attackKey, 0, my, mw, 18, "LMB");
        key(ctx, mc, 5, o.useKey, mw + G, my, width(mc) - mw - G, 18, "RMB");
        cpsSmall(ctx, mc, my, mw);
        key(ctx, mc, 6, o.jumpKey, 0, my + 18 + G, width(mc), 10, "");
        int bar = width(mc) / 3;
        int by = my + 18 + G + 4;
        ctx.fill(bar, by, width(mc) - bar, by + 1, glow[6] > 0.5f ? 0xFF1A1418 : Theme.TEXT);
    }

    private void cpsSmall(DrawContext ctx, MinecraftClient mc, int my, int mw) {
        var cps = ModuleManager.CPS;
        String l = cps.leftCps() + " CPS", r = cps.rightCps() + " CPS";
        ctx.getMatrices().push();
        ctx.getMatrices().scale(0.5f, 0.5f, 1f);
        ctx.drawText(mc.textRenderer, l, (mw - mc.textRenderer.getWidth(l) / 2) , (my + 12) * 2, glow[4] > 0.5f ? 0xFF1A1418 : Theme.MUTED, false);
        int rx = mw + G + (width(mc) - mw - G) / 2;
        ctx.drawText(mc.textRenderer, r, rx * 2 - mc.textRenderer.getWidth(r) / 2, (my + 12) * 2, glow[5] > 0.5f ? 0xFF1A1418 : Theme.MUTED, false);
        ctx.getMatrices().pop();
    }

    private void key(DrawContext ctx, MinecraftClient mc, int i, KeyBinding kb, int x, int y, int w, int h, String label) {
        float target = kb.isPressed() ? 1f : 0f;
        glow[i] += (target - glow[i]) * 0.35f;
        int bg = Theme.lerpColor(Theme.HUD_BG, Theme.ACCENT_SOFT, glow[i]);
        Theme.panel(ctx, x, y, w, h, bg);
        if (!label.isEmpty()) {
            int tw = mc.textRenderer.getWidth(label);
            int ty = label.length() > 1 ? y + 3 : y + (h - 8) / 2 + 1;
            int col = glow[i] > 0.5f ? 0xFF1A1418 : Theme.TEXT;
            ctx.drawText(mc.textRenderer, label, x + (w - tw) / 2, ty, col, glow[i] <= 0.5f);
        }
    }
}
