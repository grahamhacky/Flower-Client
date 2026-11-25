package dev.flowerclient.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.flowerclient.FlowerClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;

/** Textures loaded straight from the mod jar, so they work during the very first loading screen. */
public final class FlowerTextures {
    public static final Identifier WORDMARK = Identifier.of("flowerclient", "dynamic/wordmark");
    public static final int WORDMARK_W = 1024, WORDMARK_H = 512;
    private static boolean tried, ok;

    private FlowerTextures() {}

    private static boolean ensure() {
        if (tried) return ok;
        tried = true;
        try (InputStream in = FlowerTextures.class.getResourceAsStream("/assets/flowerclient/textures/gui/wordmark.png")) {
            if (in == null) return false;
            NativeImageBackedTexture tex = new NativeImageBackedTexture(NativeImage.read(in));
            MinecraftClient.getInstance().getTextureManager().registerTexture(WORDMARK, tex);
            tex.setFilter(true, false);
            ok = true;
        } catch (Exception e) {
            FlowerClient.LOG.warn("Could not load Flower wordmark", e);
        }
        return ok;
    }

    /** Draws the "Flower" wordmark with its visible area centred on (cx, cy), `width` GUI pixels wide. */
    public static void wordmark(DrawContext ctx, int cx, int cy, int width, float alpha) {
        if (alpha <= 0.01f || !ensure()) return;
        int h = width * WORDMARK_H / WORDMARK_W;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        ctx.drawTexture(WORDMARK, cx - width / 2, cy - h / 2, width, h, 0, 0, WORDMARK_W, WORDMARK_H, WORDMARK_W, WORDMARK_H);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    /** Same, anchored at the left edge. */
    public static void wordmarkLeft(DrawContext ctx, int x, int cy, int width, float alpha) {
        wordmark(ctx, x + width / 2, cy, width, alpha);
    }
}
