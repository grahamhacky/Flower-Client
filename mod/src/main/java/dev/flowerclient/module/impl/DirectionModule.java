package dev.flowerclient.module.impl;

import dev.flowerclient.module.TextHudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public class DirectionModule extends TextHudModule {
    private static final String[] DIRS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    public DirectionModule() { super("Direction", "Compass heading", false, 4, 170); }
    @Override protected String label() { return "Facing"; }
    @Override protected String value(MinecraftClient mc) {
        if (mc.player == null) return "-";
        float yaw = MathHelper.wrapDegrees(mc.player.getYaw());
        int idx = Math.floorMod(Math.round(yaw / 45f), 8);
        return DIRS[idx] + " " + Math.round(yaw < 0 ? yaw + 360 : yaw) + "°";
    }
}
