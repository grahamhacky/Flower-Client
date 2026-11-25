package dev.flowerclient;

import dev.flowerclient.config.ConfigManager;
import dev.flowerclient.render.CapeFeatureRenderer;
import dev.flowerclient.render.CapeManager;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import dev.flowerclient.gui.FlowerButton;
import dev.flowerclient.module.SkyTime;
import dev.flowerclient.render.FlowerLoading;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.server.WorldGenerationProgressTracker;
import net.minecraft.util.Util;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;
import dev.flowerclient.gui.FlowerScreen;
import dev.flowerclient.gui.HudEditorScreen;
import dev.flowerclient.module.HudModule;
import dev.flowerclient.module.Module;
import dev.flowerclient.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlowerClient implements ClientModInitializer {
    public static final String NAME = "Flower Client";
    public static final String VERSION = "1.0.0";
    public static final Logger LOG = LoggerFactory.getLogger("FlowerClient");

    public static KeyBinding menuKey;
    public static KeyBinding zoomKey;
    public static KeyBinding hudEditorKey;

    /** Replace the vanilla title screen with FlowerTitleScreen. */
    public static boolean customTitle = true;

    private static final Map<Screen, Long> LOADING_SINCE = new WeakHashMap<>();

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        ConfigManager.load();

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
            if (renderer instanceof PlayerEntityRenderer playerRenderer) {
                helper.register(new CapeFeatureRenderer(playerRenderer));
            }
        });

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.flowerclient.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.flowerclient"));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.flowerclient.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.flowerclient"));
        hudEditorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.flowerclient.hudeditor", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.flowerclient"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) client.setScreen(new FlowerScreen(client.currentScreen));
            while (hudEditorKey.wasPressed()) client.setScreen(new HudEditorScreen(client.currentScreen));
            for (Module m : ModuleManager.all()) m.tick(client);
            CapeManager.maybeFetch();
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            ModuleManager.CPS.sample(mc);
            if (mc.options.hudHidden || mc.currentScreen instanceof HudEditorScreen) return;
            for (HudModule m : ModuleManager.hud()) {
                if (m.isEnabled() && m.shouldRender(mc)) m.renderAt(ctx, mc);
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof GameMenuScreen) addPauseButtons(client, screen);
            if (isLoadingScreen(screen)) {
                LOADING_SINCE.putIfAbsent(screen, Util.getMeasuringTimeMs());
                ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) ->
                        FlowerLoading.render(ctx, s.width, s.height, progressOf(s), labelOf(s), 1f,
                                Util.getMeasuringTimeMs() - LOADING_SINCE.getOrDefault(s, Util.getMeasuringTimeMs())));
            }
            if (!(screen instanceof TitleScreen)) return;
            Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("✿ Flower"),
                    b -> client.setScreen(new FlowerScreen(screen)))
                    .dimensions(6, 6, 70, 20).build());
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> ConfigManager.save());
        LOG.info("{} {} loaded with {} modules", NAME, VERSION, ModuleManager.all().size());
    }

    private static Text skyLabel() { return Text.literal("\u2600 Sky: " + SkyTime.mode().label); }

    /** Client-side day/night switch + Flower menu shortcut, top-left of the pause menu. */
    private static void addPauseButtons(MinecraftClient client, Screen screen) {
        var buttons = Screens.getButtons(screen);
        buttons.add(new FlowerButton(6, 6, 112, 20, skyLabel(), b -> {
            SkyTime.next();
            b.setMessage(skyLabel());
            ConfigManager.save();
        }));
        buttons.add(new FlowerButton(6, 30, 112, 20, Text.literal("\u273F Flower"), b -> client.setScreen(new FlowerScreen(screen))));
    }

    private static boolean isLoadingScreen(Screen s) {
        return s instanceof LevelLoadingScreen || s instanceof DownloadingTerrainScreen
                || s instanceof MessageScreen || s instanceof ProgressScreen;
    }

    private static String labelOf(Screen s) {
        if (s instanceof LevelLoadingScreen) {
            float p = progressOf(s);
            return p >= 0 ? "Preparing world  " + Math.round(p * 100) + "%" : "Preparing world";
        }
        if (s instanceof DownloadingTerrainScreen) return "Loading terrain";
        String title = s.getTitle().getString();
        return title.isEmpty() ? null : title;
    }

    /** World generation progress, read by field type so it survives obfuscated names at runtime. */
    private static float progressOf(Screen s) {
        if (!(s instanceof LevelLoadingScreen)) return -1f;
        try {
            for (Field f : LevelLoadingScreen.class.getDeclaredFields()) {
                if (WorldGenerationProgressTracker.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    WorldGenerationProgressTracker t = (WorldGenerationProgressTracker) f.get(s);
                    return t == null ? -1f : Math.max(0, Math.min(100, t.getProgressPercentage())) / 100f;
                }
            }
        } catch (Exception ignored) { }
        return -1f;
    }
}
