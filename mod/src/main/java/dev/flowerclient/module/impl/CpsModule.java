package dev.flowerclient.module.impl;

import dev.flowerclient.module.TextHudModule;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;

/** Samples the mouse every frame so fast clicks between ticks are still counted. */
public class CpsModule extends TextHudModule {
    private final ArrayDeque<Long> left = new ArrayDeque<>();
    private final ArrayDeque<Long> right = new ArrayDeque<>();
    private boolean wasLeft, wasRight;

    public CpsModule() { super("CPS", "Clicks per second (left | right)", true, 4, 22); }

    public void sample(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        boolean inGame = mc.currentScreen == null && mc.player != null;
        long handle = mc.getWindow().getHandle();
        boolean l = inGame && GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = inGame && GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (l && !wasLeft) left.addLast(now);
        if (r && !wasRight) right.addLast(now);
        wasLeft = l;
        wasRight = r;
        while (!left.isEmpty() && now - left.peekFirst() > 1000) left.pollFirst();
        while (!right.isEmpty() && now - right.peekFirst() > 1000) right.pollFirst();
    }

    public int leftCps() { return left.size(); }
    public int rightCps() { return right.size(); }

    @Override protected String label() { return "CPS"; }
    @Override protected String value(MinecraftClient mc) { return left.size() + " | " + right.size(); }
}
