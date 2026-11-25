package dev.flowerclient.module.impl;

import dev.flowerclient.module.TextHudModule;
import net.minecraft.client.MinecraftClient;

public class FpsModule extends TextHudModule {
    public FpsModule() { super("FPS", "Frames per second", true, 4, 4); }
    @Override protected String label() { return "FPS"; }
    @Override protected String value(MinecraftClient mc) { return String.valueOf(mc.getCurrentFps()); }
}
