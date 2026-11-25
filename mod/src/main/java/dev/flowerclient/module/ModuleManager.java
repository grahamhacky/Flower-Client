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

    public static Module byName(String name) {
        for (Module m : MODULES) if (m.getName().equalsIgnoreCase(name)) return m;
        return null;
    }
}
