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

    @Override public int width(MinecraftClient mc) { return Theme.width(line(mc)) + PAD * 2 + 1; }
    @Override public int height(MinecraftClient mc) { return mc.textRenderer.fontHeight + PAD * 2 - 1; }

    @Override
    public void render(DrawContext ctx, MinecraftClient mc) {
        String l = label();
        int cx = PAD + 1;
        if (!l.isEmpty()) {
            Theme.text(ctx, l, cx, PAD, Theme.hudAccent, !background);
            cx += Theme.width(l + " ");
        }
        Theme.text(ctx, value(mc), cx, PAD, Theme.TEXT, !background);
    }
}
