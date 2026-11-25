package dev.flowerclient.module.impl;

import dev.flowerclient.module.TextHudModule;
import net.minecraft.client.MinecraftClient;

public class SpeedModule extends TextHudModule {
    private double bps;
    public SpeedModule() { super("Speed", "Horizontal blocks per second", false, 4, 206); }

    @Override public void tick(MinecraftClient mc) {
        if (mc.player == null) { bps = 0; return; }
        double dx = mc.player.getX() - mc.player.prevX, dz = mc.player.getZ() - mc.player.prevZ;
        double now = Math.sqrt(dx * dx + dz * dz) * 20.0;
        bps += (now - bps) * 0.5;
    }
    @Override protected String label() { return "Speed"; }
    @Override protected String value(MinecraftClient mc) { return String.format("%.2f b/s", bps); }
}
