package dev.flowerclient.module.impl;

import dev.flowerclient.module.HudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

/** A tidy row above the hunger bar: helmet → boots, then the held item, each with its durability bar. */
public class ArmorStatusModule extends HudModule {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND
    };

    public ArmorStatusModule() { super("Armor Status", "Armor and held item durability", true, 0, 0); }

    @Override public float[] defaultPosition(MinecraftClient mc, int sw, int sh) {
        return new float[]{sw / 2f + 91 - width(mc), sh - 52 - height(mc)};
    }

    @Override public boolean shouldRender(MinecraftClient mc) {
        if (mc.player == null) return false;
        for (EquipmentSlot s : SLOTS) if (!mc.player.getEquippedStack(s).isEmpty()) return true;
        return false;
    }

    @Override public int width(MinecraftClient mc) { return SLOTS.length * 18 - 2 + PAD * 2; }
    @Override public int height(MinecraftClient mc) { return 16 + PAD * 2; }

    @Override
    public void render(DrawContext ctx, MinecraftClient mc) {
        if (mc.player == null) return;
        int x = PAD;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack s = mc.player.getEquippedStack(slot);
            if (!s.isEmpty()) {
                ctx.drawItem(s, x, PAD);
                ctx.drawItemInSlot(mc.textRenderer, s, x, PAD);
            } else {
                ctx.fill(x + 6, PAD + 7, x + 10, PAD + 9, 0x22FFFFFF);
            }
            x += 18;
        }
    }
}
