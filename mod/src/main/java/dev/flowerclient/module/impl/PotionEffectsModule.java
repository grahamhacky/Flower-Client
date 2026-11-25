package dev.flowerclient.module.impl;

import dev.flowerclient.gui.Theme;
import dev.flowerclient.module.HudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.ArrayList;
import java.util.List;

public class PotionEffectsModule extends HudModule {
    private static final String[] ROMAN = {"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};

    public PotionEffectsModule() { super("Potion Effects", "Active effects with time left", true, 4, 300); }

    private List<String[]> rows(MinecraftClient mc) {
        List<String[]> out = new ArrayList<>();
        if (mc.player == null) return out;
        for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            int amp = e.getAmplifier();
            String name = e.getEffectType().value().getName().getString()
                    + (amp >= 0 && amp < ROMAN.length ? ROMAN[amp] : " " + (amp + 1));
            String time;
            if (e.isInfinite()) time = "∞";
            else {
                int secs = e.getDuration() / 20;
                time = String.format("%d:%02d", secs / 60, secs % 60);
            }
            out.add(new String[]{name, time});
        }
        return out;
    }

    @Override public float[] defaultPosition(MinecraftClient mc, int sw, int sh) { return new float[]{sw - width(mc) - 4, 54}; }

    @Override public boolean shouldRender(MinecraftClient mc) {
        return mc.player != null && !mc.player.getStatusEffects().isEmpty();
    }

    @Override public int width(MinecraftClient mc) {
        int w = Theme.width("Speed II  0:00");
        for (String[] r : rows(mc)) w = Math.max(w, Theme.width(r[0] + "  " + r[1]));
        return w + PAD * 2;
    }

    @Override public int height(MinecraftClient mc) { return Math.max(1, rows(mc).size()) * 10 + PAD * 2 - 2; }

    @Override
    public void render(DrawContext ctx, MinecraftClient mc) {
        List<String[]> r = rows(mc);
        if (r.isEmpty()) { Theme.text(ctx, "No effects", PAD + 1, PAD, Theme.MUTED, !background); return; }
        int w = width(mc);
        for (int i = 0; i < r.size(); i++) {
            int y = PAD + i * 10;
            Theme.text(ctx, r.get(i)[0], PAD + 1, y, Theme.TEXT, !background);
            Theme.text(ctx, r.get(i)[1], w - PAD - Theme.width(r.get(i)[1]), y, Theme.hudAccent, !background);
        }
    }
}
