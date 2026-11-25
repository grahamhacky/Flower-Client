package dev.flowerclient.mixin;

import dev.flowerclient.FlowerClient;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "getWindowTitle", at = @At("RETURN"), cancellable = true)
    private void flower$title(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(FlowerClient.NAME + " " + SharedConstants.getGameVersion().getName());
    }
}
