package dev.flowerclient;

import dev.flowerclient.config.ConfigManager;
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

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        ConfigManager.load();

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
            if (!(screen instanceof TitleScreen)) return;
            Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("✿ Flower"),
                    b -> client.setScreen(new FlowerScreen(screen)))
                    .dimensions(6, 6, 70, 20).build());
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> ConfigManager.save());
        LOG.info("{} {} loaded with {} modules", NAME, VERSION, ModuleManager.all().size());
    }
}
