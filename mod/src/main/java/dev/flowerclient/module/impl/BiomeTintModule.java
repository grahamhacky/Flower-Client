package dev.flowerclient.module.impl;

import dev.flowerclient.gui.Theme;
import dev.flowerclient.module.Category;
import dev.flowerclient.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

/**
 * Shifts the HUD accent toward the colour of the biome you are standing in:
 * leafy greens in forests, sand in deserts, ice blue in snow, ocean blue at sea,
 * crimson/teal in the Nether, violet in the End. Fades smoothly between biomes.
 */
public class BiomeTintModule extends Module {
    private int target = Theme.ACCENT;
    private float r = 244, g = 166, b = 192;
    private int counter;

    public BiomeTintModule() { super("Biome Tint", "HUD accent follows the biome you're in", Category.UTILITY, true); }

    @Override
    public void tick(MinecraftClient mc) {
        if (!isEnabled() || mc.player == null || mc.world == null) {
            target = Theme.ACCENT;
        } else if (counter++ % 10 == 0) {
            BlockPos pos = mc.player.getBlockPos();
            RegistryEntry<Biome> entry = mc.world.getBiome(pos);
            String id = entry.getKey().map(k -> k.getValue().getPath()).orElse("");
            target = colourFor(id, entry.value(), pos);
        }
        // ease ~8% per tick: a 1-2 second crossfade between biomes
        r += (((target >> 16) & 255) - r) * 0.08f;
        g += (((target >> 8) & 255) - g) * 0.08f;
        b += ((target & 255) - b) * 0.08f;
        Theme.hudAccent = 0xFF000000 | Math.round(r) << 16 | Math.round(g) << 8 | Math.round(b);
    }

    @Override
    protected void onDisable() { target = Theme.ACCENT; }

    private static int colourFor(String id, Biome biome, BlockPos pos) {
        if (id.contains("crimson") || id.equals("nether_wastes")) return 0xFFFF7A6B;
        if (id.contains("warped")) return 0xFF5FE0C8;
        if (id.contains("soul_sand")) return 0xFF8FD8E6;
        if (id.contains("basalt")) return 0xFFC9C2C8;
        if (id.startsWith("end") || id.equals("the_end") || id.contains("small_end")) return 0xFFD4B0FF;
        if (id.contains("cherry")) return 0xFFF7B6CC;
        if (id.contains("mushroom")) return 0xFFD9A3E0;
        if (id.contains("frozen") || id.contains("snowy") || id.contains("ice") || id.contains("grove")
                || id.contains("peaks") || id.contains("slopes")) return 0xFFBFE6FF;
        if (id.contains("desert") || id.contains("beach")) return 0xFFF0D59A;
        if (id.contains("badlands")) return 0xFFF0A070;
        if (id.contains("ocean") || id.contains("river")) return brighten(biome.getWaterColor(), 0.45f);
        if (id.contains("deep_dark")) return 0xFF6FD6D9;
        if (id.contains("lush")) return 0xFFA7E27A;
        if (id.contains("dripstone")) return 0xFFC9A98A;
        if (id.contains("swamp") || id.contains("mangrove")) return 0xFF9BC47A;
        return brighten(biome.getGrassColorAt(pos.getX(), pos.getZ()), 0.35f);
    }

    /** Mix toward white so the colour stays readable on the dark HUD boxes. */
    private static int brighten(int rgb, float amount) {
        int cr = (rgb >> 16) & 255, cg = (rgb >> 8) & 255, cb = rgb & 255;
        cr += Math.round((255 - cr) * amount);
        cg += Math.round((255 - cg) * amount);
        cb += Math.round((255 - cb) * amount);
        return 0xFF000000 | cr << 16 | cg << 8 | cb;
    }
}
