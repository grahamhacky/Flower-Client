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
        for (String s : lines(mc)) w = Math.max(w, mc.textRenderer.getWidth(s));
        return w + PAD * 2;
    }
    @Override public int height(MinecraftClient mc) { return 4 * 10 + PAD * 2 - 2; }

    @Override
    public void render(DrawContext ctx, MinecraftClient mc) {
        if (mc.player == null) return;
        String[] l = lines(mc);
        for (int i = 0; i < l.length; i++) {
            int y = PAD + i * 10;
            if (i < 3) {
                ctx.drawText(mc.textRenderer, l[i].substring(0, 1), PAD, y, Theme.hudAccent, true);
                ctx.drawText(mc.textRenderer, l[i].substring(2), PAD + 10, y, Theme.TEXT, true);
            } else {
                ctx.drawText(mc.textRenderer, l[i], PAD, y, Theme.MUTED, true);
            }
        }
    }
}
