package dev.flowerclient.module;

import dev.flowerclient.gui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** One line: a pink label followed by a white value, e.g. "FPS 144". */
public abstract class TextHudModule extends HudModule {
    protected TextHudModule(String name, String description, boolean on, float x, float y) {
        super(name, description, on, x, y);
    }

    protected abstract String label();
    protected abstract String value(MinecraftClient mc);

    private String line(MinecraftClient mc) {
        String l = label();
        return l.isEmpty() ? value(mc) : l + " " + value(mc);
    }

    @Override public int width(MinecraftClient mc) { return mc.textRenderer.getWidth(line(mc)) + PAD * 2; }
    @Override public int height(MinecraftClient mc) { return mc.textRenderer.fontHeight + PAD * 2 - 1; }

    @Override
    public void render(DrawContext ctx, MinecraftClient mc) {
        String l = label();
        int cx = PAD;
        if (!l.isEmpty()) {
            ctx.drawText(mc.textRenderer, l, cx, PAD, Theme.hudAccent, true);
            cx += mc.textRenderer.getWidth(l + " ");
        }
        ctx.drawText(mc.textRenderer, value(mc), cx, PAD, Theme.TEXT, true);
    }
}
