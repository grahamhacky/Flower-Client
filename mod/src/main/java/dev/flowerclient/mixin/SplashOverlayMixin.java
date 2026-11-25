package dev.flowerclient.mixin;

import dev.flowerclient.render.FlowerLoading;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Paints the Flower loading screen over the Mojang splash, following its fade-out timing. */
@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {
    @Shadow @Final private ResourceReload reload;
    @Shadow private long reloadCompleteTime;

    @Unique private long flower$start = -1L;
    @Unique private float flower$progress;

    @Inject(method = "render", at = @At("TAIL"))
    private void flower$render(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        long now = Util.getMeasuringTimeMs();
        if (flower$start < 0) flower$start = now;
        float target = reload.getProgress();
        flower$progress += (target - flower$progress) * 0.08f;
        float alpha = 1f;
        if (reloadCompleteTime > -1L) {
            float f = (now - reloadCompleteTime) / 1000f;
            if (f >= 1f) alpha = 1f - Math.min(1f, f - 1f);
            flower$progress = 1f;
        }
        FlowerLoading.render(ctx, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), flower$progress, null, alpha, now - flower$start);
    }
}
