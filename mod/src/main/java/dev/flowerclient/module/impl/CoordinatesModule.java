package dev.flowerclient.module.impl;

import dev.flowerclient.gui.Theme;
import dev.flowerclient.module.HudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class CoordinatesModule extends HudModule {
    public CoordinatesModule() { super("Coordinates", "XYZ position and biome", true, 4, 118); }

    private String[] lines(MinecraftClient mc) {
        var p = mc.player;
        String biome = mc.world == null ? "?" : mc.world.getBiome(p.getBlockPos()).getKey()
                .map(k -> pretty(k.getValue().getPath())).orElse("?");
        return new String[]{
                "X " + p.getBlockX(),
                "Y " + p.getBlockY(),
                "Z " + p.getBlockZ(),
                biome
        };
    }

    private static String pretty(String id) {
        StringBuilder b = new StringBuilder();
        for (String part : id.split("_")) {
            if (part.isEmpty()) continue;
            if (b.length() > 0) b.append(' ');
            b.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return b.toString();
    }

    @Override public int width(MinecraftClient mc) {
        if (mc.player == null) return 60;
        int w = 0;
        for (String s : lines(mc)) w = Math.max(w, Theme.width(s));
        return w + PAD * 2 + 1;
    }
    @Override public int height(MinecraftClient mc) { return 4 * 10 + PAD * 2 - 1; }

    @Override
    public void render(DrawContext ctx, MinecraftClient mc) {
        if (mc.player == null) return;
        String[] l = lines(mc);
        for (int i = 0; i < l.length; i++) {
            int y = PAD + i * 10;
            if (i < 3) {
                Theme.text(ctx, l[i].substring(0, 1), PAD + 1, y, Theme.hudAccent, !background);
                Theme.text(ctx, l[i].substring(2), PAD + 11, y, Theme.TEXT, !background);
            } else {
                Theme.text(ctx, l[i], PAD + 1, y + 1, Theme.MUTED, !background);
            }
        }
    }
}
