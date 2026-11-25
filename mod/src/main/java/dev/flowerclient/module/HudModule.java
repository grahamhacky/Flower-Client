package dev.flowerclient.module;

import dev.flowerclient.gui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** A module that draws a movable, scalable box on the HUD. Position is stored in scaled GUI pixels. */
public abstract class HudModule extends Module {
    /** -1 = not placed yet; ModuleManager.layoutDefaults() places it on first render. */
    public float x = -1, y = -1;
    public float scale = 1.0f;
    public boolean background = true;

    protected HudModule(String name, String description, boolean enabledByDefault, float defX, float defY) {
        super(name, description, Category.HUD, enabledByDefault);
    }

    /** Where this module goes in the default layout. Return null to join the left column. */
    public float[] defaultPosition(MinecraftClient mc, int screenW, int screenH) { return null; }

    public boolean isPlaced() { return x >= 0 && y >= 0; }

    /** Unscaled content size. */
    public abstract int width(MinecraftClient mc);
    public abstract int height(MinecraftClient mc);
    /** Draw content with its top-left at (0,0). */
    public abstract void render(DrawContext ctx, MinecraftClient mc);

    public boolean shouldRender(MinecraftClient mc) { return mc.player != null; }

    public int scaledWidth(MinecraftClient mc) { return Math.round(width(mc) * scale); }
    public int scaledHeight(MinecraftClient mc) { return Math.round(height(mc) * scale); }

    public void clampToScreen(MinecraftClient mc) {
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        x = Math.max(0, Math.min(x, sw - scaledWidth(mc)));
        y = Math.max(0, Math.min(y, sh - scaledHeight(mc)));
    }

    public void renderAt(DrawContext ctx, MinecraftClient mc) {
        if (!isPlaced()) ModuleManager.layoutDefaults(mc);
        clampToScreen(mc);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        if (background) Theme.hudPanel(ctx, 0, 0, width(mc), height(mc));
        render(ctx, mc);
        ctx.getMatrices().pop();
    }

    /** Helper for simple one-line text modules. */
    protected static final int PAD = 4;
}
