package dev.flowerclient.module.impl;

import dev.flowerclient.FlowerClient;
import dev.flowerclient.module.Category;
import dev.flowerclient.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

/** Hold the zoom key (C by default). Scroll while zooming to adjust. */
public class ZoomModule extends Module {
    public float zoomLevel = 4f;
    public boolean smooth = true;
    public boolean cinematic = false;

    private float prev = 1f, current = 1f;
    private boolean wasZooming;
    private boolean savedSmoothCamera;

    public ZoomModule() { super("Zoom", "Hold C to zoom, scroll to adjust", Category.UTILITY, true); }

    public boolean isZooming() {
        return isEnabled() && FlowerClient.zoomKey != null && FlowerClient.zoomKey.isPressed()
                && MinecraftClient.getInstance().currentScreen == null;
    }

    @Override
    public void tick(MinecraftClient mc) {
        boolean zooming = isZooming();
        if (zooming != wasZooming) {
            if (zooming) {
                savedSmoothCamera = mc.options.smoothCameraEnabled;
                if (cinematic) mc.options.smoothCameraEnabled = true;
            } else {
                mc.options.smoothCameraEnabled = savedSmoothCamera;
                zoomLevel = 4f;
            }
            wasZooming = zooming;
        }
        float target = zooming ? zoomLevel : 1f;
        prev = current;
        current = smooth ? current + (target - current) * 0.45f : target;
    }

    /** Divisor applied to FOV. */
    public float factor(float tickDelta) { return MathHelper.lerp(tickDelta, prev, current); }

    /** Returns true if the scroll was consumed. */
    public boolean onScroll(double amount) {
        if (!isZooming() || amount == 0) return false;
        zoomLevel = MathHelper.clamp(zoomLevel * (amount > 0 ? 1.2f : 1 / 1.2f), 1.5f, 50f);
        return true;
    }
}
