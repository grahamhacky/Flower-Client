package dev.flowerclient.module;

/** Client-side time of day, set from the pause menu. Server = follow the real world time. */
public final class SkyTime {
    public enum Mode {
        SERVER("Server", -1), SUNRISE("Sunrise", 23200), DAY("Day", 6000), SUNSET("Sunset", 12400), NIGHT("Night", 18000);
        public final String label;
        public final long ticks;
        Mode(String label, long ticks) { this.label = label; this.ticks = ticks; }
    }

    private static Mode mode = Mode.SERVER;
    private static double shown = -1;      // currently displayed time within the day (0..24000)
    private static long lastNanos;

    private SkyTime() {}

    public static Mode mode() { return mode; }
    public static boolean active() { return mode != Mode.SERVER || shown >= 0; }

    public static void set(Mode m) { mode = m; }

    public static Mode next() {
        Mode[] all = Mode.values();
        mode = all[(mode.ordinal() + 1) % all.length];
        return mode;
    }

    /** Glides toward the chosen time (about 1.5 s for half a day) and keeps the real day number for the moon phase. */
    public static long apply(long real) {
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0 : Math.min(0.1, (now - lastNanos) / 1e9);
        lastNanos = now;
        double realInDay = Math.floorMod(real, 24000L);
        if (shown < 0) shown = realInDay;
        double target = mode == Mode.SERVER ? realInDay : mode.ticks;
        double diff = Math.floorMod((long) Math.round(target - shown), 24000L); // forward distance
        if (diff > 12000) diff -= 24000;                                         // go the short way round
        double step = Math.signum(diff) * Math.min(Math.abs(diff), Math.max(Math.abs(diff) * 4 * dt, 8000 * dt));
        shown = (shown + step + 24000) % 24000;
        if (mode == Mode.SERVER && Math.abs(diff) < 20) { shown = -1; return real; }
        return Math.floorDiv(real, 24000L) * 24000L + Math.round(shown);
    }
}
