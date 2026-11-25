package dev.flowerclient.mixin;

import dev.flowerclient.module.ModuleManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    /** World FOV only (changingFov == true); the held item is left alone, like OptiFine zoom. */
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void flower$zoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        if (!changingFov || ModuleManager.ZOOM == null) return;
        float f = ModuleManager.ZOOM.factor(tickDelta);
        if (f > 1.001f) cir.setReturnValue(cir.getReturnValue() / f);
    }
}
