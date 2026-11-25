package dev.flowerclient.module.impl;

import dev.flowerclient.config.ConfigManager;
import dev.flowerclient.integration.DiscordRPC;
import dev.flowerclient.module.Category;
import dev.flowerclient.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

/**
 * Shows Flower Client on your own Discord profile (Rich Presence). Off by default and entirely
 * opt-in: it only runs when you enable it, and it needs a Discord Application ID set in the
 * launcher's Settings. Nothing is sent anywhere except your local Discord app.
 */
public class DiscordPresenceModule extends Module {
    private final DiscordRPC rpc = new DiscordRPC();
    private boolean started;
    private String startedWith = "";

    public DiscordPresenceModule() {
        super("Discord Presence", "Show Flower Client on your Discord profile", Category.UTILITY, false);
    }

    @Override
    protected void onDisable() {
        if (started) { rpc.stop(); started = false; }
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (!isEnabled()) return;
        String appId = ConfigManager.getDiscordAppId();
        if (appId == null || appId.isBlank()) {
            if (started) { rpc.stop(); started = false; }
            return;
        }
        if (!started || !appId.equals(startedWith)) {
            rpc.start(appId);
            started = true;
            startedWith = appId;
        }

        String details, state;
        if (mc.world == null) {
            details = "In the menus";
            state = "";
        } else if (mc.getCurrentServerEntry() != null) {
            details = "On a server";
            ServerInfo e = mc.getCurrentServerEntry();
            state = e.name != null && !e.name.isBlank() && !e.name.equalsIgnoreCase("Minecraft Server")
                    ? e.name : dimension(mc);
        } else {
            details = "Singleplayer";
            state = dimension(mc);
        }
        rpc.setPresence(details, state);
    }

    private static String dimension(MinecraftClient mc) {
        if (mc.world == null) return "";
        String path = mc.world.getRegistryKey().getValue().getPath();
        return switch (path) {
            case "overworld" -> "Overworld";
            case "the_nether" -> "The Nether";
            case "the_end" -> "The End";
            default -> capitalise(path.replace('_', ' '));
        };
    }

    private static String capitalise(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
