package dev.flowerclient.mixin;

import dev.flowerclient.module.ModuleManager;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    /** While zooming, the scroll wheel changes zoom instead of the hotbar slot. */
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void flower$zoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (ModuleManager.ZOOM != null && ModuleManager.ZOOM.onScroll(vertical)) ci.cancel();
    }
}
