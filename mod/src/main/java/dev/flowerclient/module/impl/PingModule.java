package dev.flowerclient.module.impl;

import dev.flowerclient.module.TextHudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

public class PingModule extends TextHudModule {
    public PingModule() { super("Ping", "Latency to the server", true, 60, 4); }

    @Override public boolean shouldRender(MinecraftClient mc) {
        return mc.player != null && mc.getNetworkHandler() != null && !mc.isInSingleplayer();
    }
    @Override protected String label() { return "Ping"; }
    @Override protected String value(MinecraftClient mc) {
        if (mc.player == null || mc.getNetworkHandler() == null) return "-";
        PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return e == null ? "-" : e.getLatency() + " ms";
    }
}
