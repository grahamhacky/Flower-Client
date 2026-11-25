package dev.flowerclient.module.impl;

import dev.flowerclient.module.Category;
import dev.flowerclient.module.Module;
import net.minecraft.client.MinecraftClient;

/** Keeps the sprint key held so you sprint whenever you move forward. */
public class ToggleSprintModule extends Module {
    public ToggleSprintModule() { super("Toggle Sprint", "Always sprint when walking forward", false, Category.UTILITY); }

    @Override public void tick(MinecraftClient mc) {
        if (isEnabled() && mc.player != null) mc.options.sprintKey.setPressed(true);
    }

    @Override protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null) mc.options.sprintKey.setPressed(false);
    }
}
