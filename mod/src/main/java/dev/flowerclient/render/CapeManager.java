package dev.flowerclient.render;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.flowerclient.FlowerClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who wears which Flower cape. Your own cape is unlocked locally with a code (stored in config);
 * everyone else's capes come from a shared capes.json so Flower users see each other. No personal
 * data is sent anywhere — the mod only *reads* the public list.
 */
public final class CapeManager {
    public static final Identifier FLOWER = Identifier.of("flowerclient", "textures/cape/flower.png");

    private static volatile boolean selfUnlocked;
    private static volatile String selfCape = "flower";
    private static final Map<UUID, String> OTHERS = new ConcurrentHashMap<>();
    private static volatile long lastFetch;
    private static volatile String capesUrl = "";

    private CapeManager() {}

    public static void configure(boolean unlocked, String cape, String url) {
        selfUnlocked = unlocked;
        selfCape = cape == null ? "none" : cape;
        capesUrl = url == null ? "" : url;
    }

    /** Cape texture for a player, or null for none. */
    public static Identifier capeFor(UUID id) {
        if (id == null) return null;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && id.equals(mc.player.getUuid())) {
            return selfUnlocked && !"none".equals(selfCape) ? textureFor(selfCape) : null;
        }
        return textureFor(OTHERS.get(id));
    }

    private static Identifier textureFor(String cape) {
        return "flower".equals(cape) ? FLOWER : null;
    }

    /** Refresh the shared list at most every 5 minutes, off-thread. */
    public static void maybeFetch() {
        if (capesUrl.isBlank()) return;
        long now = System.currentTimeMillis();
        if (now - lastFetch < 5 * 60_000L) return;
        lastFetch = now;
        Thread t = new Thread(CapeManager::fetch, "Flower-Capes");
        t.setDaemon(true);
        t.start();
    }

    private static void fetch() {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(capesUrl))
                    .header("User-Agent", "FlowerClient").timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return;
            JsonObject root = JsonParser.parseString(res.body()).getAsJsonObject();
            JsonObject capes = root.has("capes") ? root.getAsJsonObject("capes") : root;
            Map<UUID, String> next = new ConcurrentHashMap<>();
            for (Map.Entry<String, com.google.gson.JsonElement> e : capes.entrySet()) {
                UUID id = parseUuid(e.getKey());
                if (id != null && e.getValue().isJsonPrimitive()) next.put(id, e.getValue().getAsString());
            }
            OTHERS.clear();
            OTHERS.putAll(next);
            FlowerClient.LOG.info("Loaded {} Flower capes", OTHERS.size());
        } catch (Exception e) {
            FlowerClient.LOG.debug("Could not load capes list", e);
        }
    }

    private static UUID parseUuid(String s) {
        try {
            if (s.contains("-")) return UUID.fromString(s);
            if (s.length() == 32) {
                return UUID.fromString(s.replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})", "$1-$2-$3-$4-$5"));
            }
        } catch (Exception ignored) { }
        return null;
    }
}
