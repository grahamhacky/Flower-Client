package dev.flowerclient.integration;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import dev.flowerclient.FlowerClient;
import net.minecraft.SharedConstants;

import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Minimal Discord Rich Presence over local IPC. Talks straight to the Discord desktop app's
 * pipe/socket on this machine — nothing leaves the computer except to the user's own Discord
 * client, and only what the presence shows (a label, a state, a timestamp). Write-only and
 * low-volume: presence is only re-sent when it actually changes.
 */
public final class DiscordRPC {
    private static final long START_SECONDS = System.currentTimeMillis() / 1000L;

    private volatile boolean running;
    private volatile boolean dirty;
    private volatile String details = "";
    private volatile String state = "";
    private String appId = "";
    private Thread thread;

    private SocketChannel channel;   // Linux / macOS
    private RandomAccessFile pipe;    // Windows
    private boolean connected;
    private long lastAttempt;

    public synchronized void start(String appId) {
        this.appId = appId == null ? "" : appId.trim();
        if (running) { dirty = true; return; }
        running = true;
        dirty = true;
        thread = new Thread(this::loop, "Flower-DiscordRPC");
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;
        if (thread != null) thread.interrupt();
        thread = null;
    }

    public void setPresence(String details, String state) {
        String d = details == null ? "" : details;
        String s = state == null ? "" : state;
        if (!d.equals(this.details) || !s.equals(this.state)) {
            this.details = d;
            this.state = s;
            this.dirty = true;
        }
    }

    private void loop() {
        while (running) {
            try {
                if (!connected) {
                    if (System.currentTimeMillis() - lastAttempt > 15000) {
                        lastAttempt = System.currentTimeMillis();
                        tryConnect();
                    }
                } else if (dirty) {
                    dirty = false;
                    sendActivity();
                }
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                closeConn();
            }
        }
        try { if (connected) clearActivity(); } catch (Exception ignored) { }
        closeConn();
    }

    private void tryConnect() {
        if (appId.isEmpty()) return;
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        for (int i = 0; i < 10; i++) {
            try {
                if (windows) {
                    pipe = new RandomAccessFile("\\\\?\\pipe\\discord-ipc-" + i, "rw");
                } else {
                    Path p = unixSocket(i);
                    if (p == null) continue;
                    SocketChannel ch = SocketChannel.open(StandardProtocolFamily.UNIX);
                    ch.connect(UnixDomainSocketAddress.of(p));
                    channel = ch;
                }
                JsonObject handshake = new JsonObject();
                handshake.addProperty("v", 1);
                handshake.addProperty("client_id", appId);
                writeFrame(0, handshake.toString());
                connected = true;
                dirty = true;
                FlowerClient.LOG.info("Discord Rich Presence connected");
                return;
            } catch (Exception e) {
                closeConn();
            }
        }
    }

    private static Path unixSocket(int i) {
        String[] dirs = {System.getenv("XDG_RUNTIME_DIR"), System.getenv("TMPDIR"),
                System.getenv("TMP"), System.getenv("TEMP"), "/tmp"};
        String[] subs = {"", "app/com.discordapp.Discord/", "snap.discord/",
                "app/com.discordapp.DiscordCanary/", "app/com.discordapp.DiscordPTB/"};
        for (String d : dirs) {
            if (d == null || d.isEmpty()) continue;
            for (String sub : subs) {
                Path p = Path.of(d, sub + "discord-ipc-" + i);
                try { if (Files.exists(p)) return p; } catch (Exception ignored) { }
            }
        }
        return null;
    }

    private void sendActivity() throws Exception {
        JsonObject activity = new JsonObject();
        if (!details.isEmpty()) activity.addProperty("details", trim(details));
        if (!state.isEmpty()) activity.addProperty("state", trim(state));
        JsonObject timestamps = new JsonObject();
        timestamps.addProperty("start", START_SECONDS);
        activity.add("timestamps", timestamps);
        JsonObject assets = new JsonObject();
        assets.addProperty("large_image", "flower");
        assets.addProperty("large_text", "Flower Client " + SharedConstants.getGameVersion().getName());
        activity.add("assets", assets);
        writeActivityFrame(activity);
    }

    private void clearActivity() throws Exception {
        writeActivityFrame(null);
    }

    private void writeActivityFrame(JsonObject activity) throws Exception {
        JsonObject args = new JsonObject();
        args.addProperty("pid", (int) ProcessHandle.current().pid());
        args.add("activity", activity == null ? JsonNull.INSTANCE : activity);
        JsonObject frame = new JsonObject();
        frame.addProperty("cmd", "SET_ACTIVITY");
        frame.add("args", args);
        frame.addProperty("nonce", UUID.randomUUID().toString());
        writeFrame(1, frame.toString());
    }

    private void writeFrame(int op, String json) throws Exception {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(op);
        buf.putInt(data.length);
        buf.put(data);
        if (channel != null) {
            buf.flip();
            while (buf.hasRemaining()) channel.write(buf);
        } else if (pipe != null) {
            pipe.write(buf.array());
        } else {
            throw new IllegalStateException("no Discord connection");
        }
    }

    private void closeConn() {
        connected = false;
        try { if (channel != null) channel.close(); } catch (Exception ignored) { }
        try { if (pipe != null) pipe.close(); } catch (Exception ignored) { }
        channel = null;
        pipe = null;
    }

    private static String trim(String s) { return s.length() > 128 ? s.substring(0, 128) : s; }
}
