package dev.flowerclient.mixin;

import dev.flowerclient.FlowerClient;
import dev.flowerclient.gui.FlowerTitleScreen;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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

    /** Swap the vanilla title screen for Flower's. */
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen flower$titleScreen(Screen screen) {
        if (screen instanceof TitleScreen && FlowerClient.customTitle) return new FlowerTitleScreen();
        return screen;
    }
}
