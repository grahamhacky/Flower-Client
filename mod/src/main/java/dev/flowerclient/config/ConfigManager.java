package dev.flowerclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.flowerclient.FlowerClient;
import dev.flowerclient.module.HudModule;
import dev.flowerclient.module.Module;
import dev.flowerclient.module.ModuleManager;
import dev.flowerclient.module.SkyTime;
import dev.flowerclient.render.CapeManager;
import dev.flowerclient.module.impl.ZoomModule;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * config/flowerclient.json — shared with the Flower launcher, which edits the "enabled" flags.
 * Unknown keys are preserved so the two never clobber each other.
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static JsonObject root = new JsonObject();
    private static final int LAYOUT = 2;
    private static boolean capeUnlocked = false;
    private static String cape = "flower";
    private static String capesUrl = "https://raw.githubusercontent.com/grahamhacky/Flower-Client/main/capes.json";
    private static String discordAppId = "";

    private ConfigManager() {}

    public static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("flowerclient.json"); }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) { save(); return; }
        try {
            JsonElement el = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8));
            if (!el.isJsonObject()) return;
            root = el.getAsJsonObject();
            JsonObject mods = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
            // layout 2 = the redesigned HUD; positions saved by older builds are dropped once
            boolean keepPositions = root.has("layout") && root.get("layout").getAsInt() >= LAYOUT;
            if (root.has("discordAppId")) discordAppId = root.get("discordAppId").getAsString();
            if (root.has("sky")) {
                try { SkyTime.set(SkyTime.Mode.valueOf(root.get("sky").getAsString())); } catch (IllegalArgumentException ignored) {}
            }
            if (root.has("capeUnlocked")) capeUnlocked = root.get("capeUnlocked").getAsBoolean();
            if (root.has("cape")) cape = root.get("cape").getAsString();
            if (root.has("capesUrl")) capesUrl = root.get("capesUrl").getAsString();
            for (Map.Entry<String, JsonElement> e : mods.entrySet()) {
                Module m = ModuleManager.byName(e.getKey());
                if (m == null || !e.getValue().isJsonObject()) continue;
                JsonObject o = e.getValue().getAsJsonObject();
                if (o.has("enabled")) m.setEnabled(o.get("enabled").getAsBoolean());
                if (m instanceof HudModule h) {
                    if (keepPositions && o.has("x")) h.x = o.get("x").getAsFloat();
                    if (keepPositions && o.has("y")) h.y = o.get("y").getAsFloat();
                    if (keepPositions && o.has("scale")) h.scale = clamp(o.get("scale").getAsFloat(), 0.5f, 3f);
                    if (o.has("background")) h.background = o.get("background").getAsBoolean();
                }
                if (m instanceof ZoomModule z) {
                    if (o.has("smooth")) z.smooth = o.get("smooth").getAsBoolean();
                    if (o.has("cinematic")) z.cinematic = o.get("cinematic").getAsBoolean();
                }
            }
        } catch (Exception ex) {
            FlowerClient.LOG.error("Could not read {}, using defaults", f, ex);
        }
        CapeManager.configure(capeUnlocked, cape, capesUrl);
    }

    public static synchronized void save() {
        JsonObject mods = root.has("modules") && root.get("modules").isJsonObject()
                ? root.getAsJsonObject("modules") : new JsonObject();
        for (Module m : ModuleManager.all()) {
            JsonObject o = mods.has(m.getName()) && mods.get(m.getName()).isJsonObject()
                    ? mods.getAsJsonObject(m.getName()) : new JsonObject();
            o.addProperty("enabled", m.isEnabled());
            if (m instanceof HudModule h) {
                o.addProperty("x", h.x);
                o.addProperty("y", h.y);
                o.addProperty("scale", h.scale);
                o.addProperty("background", h.background);
            }
            if (m instanceof ZoomModule z) {
                o.addProperty("smooth", z.smooth);
                o.addProperty("cinematic", z.cinematic);
            }
            mods.add(m.getName(), o);
        }
        root.add("modules", mods);
        root.addProperty("layout", LAYOUT);
        root.addProperty("sky", SkyTime.mode().name());
        root.addProperty("capeUnlocked", capeUnlocked);
        root.addProperty("cape", cape);
        root.addProperty("capesUrl", capesUrl);
        root.addProperty("discordAppId", discordAppId);
        root.addProperty("version", FlowerClient.VERSION);
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            Path tmp = f.resolveSibling("flowerclient.json.tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            FlowerClient.LOG.error("Could not save Flower Client config", ex);
        }
    }

    public static String getDiscordAppId() { return discordAppId; }

    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
}
