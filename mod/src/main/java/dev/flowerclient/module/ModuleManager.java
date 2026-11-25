package dev.flowerclient.module;

import dev.flowerclient.module.impl.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();
    private static final List<HudModule> HUD = new ArrayList<>();

    public static CpsModule CPS;
    public static ZoomModule ZOOM;

    private ModuleManager() {}

    public static void init() {
        if (!MODULES.isEmpty()) return;
        add(new FpsModule());
        add(CPS = new CpsModule());
        add(new KeystrokesModule());
        add(new CoordinatesModule());
        add(new ArmorStatusModule());
        add(new PotionEffectsModule());
        add(new PingModule());
        add(new DirectionModule());
        add(new ClockModule());
        add(new SpeedModule());
        add(ZOOM = new ZoomModule());
        add(new ToggleSprintModule());
        add(new BiomeTintModule());
    }

    private static void add(Module m) {
        MODULES.add(m);
        if (m instanceof HudModule h) HUD.add(h);
    }

    public static List<Module> all() { return Collections.unmodifiableList(MODULES); }
    public static List<HudModule> hud() { return Collections.unmodifiableList(HUD); }

    /** Left column, top to bottom. Modules with their own defaultPosition() are placed there instead. */
    private static final String[] COLUMN = {"FPS", "CPS", "Keystrokes", "Coordinates", "Direction", "Speed", "Clock"};

    /** Places every HUD module that has no position yet. */
    public static void layoutDefaults(net.minecraft.client.MinecraftClient mc) {
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        float y = 4;
        for (String name : COLUMN) {
            if (!(byName(name) instanceof HudModule h)) continue;
            if (!h.isPlaced()) { h.x = 4; h.y = y; }
            if (h.isEnabled() && h.x < 40) y = Math.max(y, h.y + h.scaledHeight(mc) + 3);
        }
        for (HudModule h : HUD) {
            if (h.isPlaced()) continue;
            float[] p = h.defaultPosition(mc, sw, sh);
            if (p != null) { h.x = p[0]; h.y = p[1]; }
            else { h.x = 4; h.y = y; y += h.scaledHeight(mc) + 3; }
        }
    }

    /** Forget all positions and scales so the default layout is used again. */
    public static void resetLayout() {
        for (HudModule h : HUD) { h.x = -1; h.y = -1; h.scale = 1f; }
    }

    public static Module byName(String name) {
        for (Module m : MODULES) if (m.getName().equalsIgnoreCase(name)) return m;
        return null;
    }
}
