package dev.flowerclient.module.impl;

import dev.flowerclient.gui.Theme;
import dev.flowerclient.module.HudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public class ArmorStatusModule extends HudModule {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND
    };

    public ArmorStatusModule() { super("Armor Status", "Armor and held item durability", true, 4, 400); }

    @Override public int width(MinecraftClient mc) { return 16 + 4 + mc.textRenderer.getWidth("0000") + PAD * 2; }
    @Override public int height(MinecraftClient mc) { return SLOTS.length * 17 + PAD * 2 - 1; }

    @Override
    public void render(DrawContext ctx, MinecraftClient mc) {
        if (mc.player == null) return;
        int y = PAD;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack s = mc.player.getEquippedStack(slot);
            if (!s.isEmpty()) {
                ctx.drawItem(s, PAD, y);
                String txt;
                int col = Theme.TEXT;
                if (s.isDamageable()) {
                    int left = s.getMaxDamage() - s.getDamage();
                    float pct = left / (float) s.getMaxDamage();
                    txt = String.valueOf(left);
                    col = pct > 0.5f ? Theme.TEXT : pct > 0.2f ? 0xFFFFD27A : 0xFFFF6B6B;
                } else {
                    txt = s.getCount() > 1 ? String.valueOf(s.getCount()) : "";
                }
                ctx.drawText(mc.textRenderer, txt, PAD + 20, y + 4, col, true);
            }
            y += 17;
        }
    }
}
