package dev.flowerclient.mixin;

import dev.flowerclient.module.SkyTime;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Client-side sky: only what you see changes; the server's time is untouched. */
@Mixin(ClientWorld.Properties.class)
public abstract class ClientWorldPropertiesMixin {
    @Inject(method = "getTimeOfDay", at = @At("RETURN"), cancellable = true)
    private void flower$skyTime(CallbackInfoReturnable<Long> cir) {
        if (SkyTime.active()) cir.setReturnValue(SkyTime.apply(cir.getReturnValue()));
    }
}
