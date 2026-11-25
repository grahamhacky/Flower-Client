package dev.flowerclient.gui;

import dev.flowerclient.FlowerClient;
import dev.flowerclient.render.FlowerArt;
import dev.flowerclient.render.FlowerTextures;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

/** Flower's take on the main menu: panorama under a soft dark wash, wordmark, glass buttons and the pixel bloom. */
public class FlowerTitleScreen extends Screen {
    private static long firstShown = -1L;
    private long opened;

    public FlowerTitleScreen() { super(Text.translatable("narrator.screen.title")); }

    @Override public boolean shouldCloseOnEsc() { return false; }

    @Override
    protected void init() {
        opened = Util.getMeasuringTimeMs();
        if (firstShown < 0) firstShown = opened;
        int x = Math.max(24, width / 14), bw = 164, bh = 20, gap = 5;
        int top = height / 2 - 34;
        int row = 0;

        addDrawableChild(new FlowerButton(x, top, bw, bh, Text.translatable("menu.singleplayer"),
                b -> client.setScreen(new SelectWorldScreen(this)), FlowerButton.Style.PRIMARY));
        addDrawableChild(new FlowerButton(x, top + (bh + gap) * ++row, bw, bh, Text.translatable("menu.multiplayer"),
                b -> client.setScreen(new MultiplayerScreen(this))));
        addDrawableChild(new FlowerButton(x, top + (bh + gap) * ++row, bw, bh, Text.translatable("menu.options"),
                b -> client.setScreen(new OptionsScreen(this, client.options))));
        addDrawableChild(new FlowerButton(x, top + (bh + gap) * ++row, bw, bh, Text.literal("✿ Flower Menu"),
                b -> client.setScreen(new FlowerScreen(this))));
        // Language + Accessibility share a row with short labels so neither overflows its box.
        int half = (bw - gap) / 2;
        int rowY = top + (bh + gap) * ++row + 6;
        addDrawableChild(new FlowerButton(x, rowY, half, bh, Text.literal("Language"),
                b -> client.setScreen(new LanguageOptionsScreen(this, client.options, client.getLanguageManager())), FlowerButton.Style.CENTER));
        addDrawableChild(new FlowerButton(x + half + gap, rowY, bw - half - gap, bh, Text.literal("Accessibility"),
                b -> client.setScreen(new AccessibilityOptionsScreen(this, client.options)), FlowerButton.Style.CENTER));
        addDrawableChild(new FlowerButton(x, top + (bh + gap) * ++row + 12, bw, bh, Text.translatable("menu.quit"),
                b -> client.scheduleStop()));
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderPanoramaBackground(ctx, delta);
        // dark wash, heavier on the left where the menu sits
        int strips = 48;
        for (int i = 0; i < strips; i++) {
            float k = i / (float) (strips - 1);
            int a = Math.round(235 - 120 * k);
            int x1 = width * i / strips, x2 = width * (i + 1) / strips;
            ctx.fill(x1, 0, x2, height, a << 24 | 0x0B090C);
        }
        ctx.fillGradient(0, height - 60, width, height, 0x000B090C, 0xC00B090C);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = Util.getMeasuringTimeMs();
        float fade = Math.min(1f, (now - opened) / 600f);
        for (var child : children()) if (child instanceof FlowerButton fb) fb.setAlpha(fade);

        super.render(ctx, mouseX, mouseY, delta);

        int x = Math.max(24, width / 14);
        int top = height / 2 - 34;

        // the bloom keeps growing from the first time the menu appeared, then idles
        float t = (now - firstShown) / 1000f;
        float r = Math.min(width, height) * 0.2f;
        FlowerArt.draw(ctx, width * 0.7f, height * 0.42f, r, r > 50 ? 2 : 1, t, Math.min(1f, t / 4.5f), fade);

        FlowerTextures.wordmarkLeft(ctx, x - 3, top - 50, 150, fade);
        String chips = SharedConstants.getGameVersion().getName() + "  ·  Fabric  ·  PvP";
        ctx.drawText(textRenderer, chips, x, top - 14, Theme.withAlpha(Theme.MUTED, fade), false);

        String left = FlowerClient.NAME + " " + FlowerClient.VERSION;
        ctx.drawText(textRenderer, left, 6, height - 12, Theme.withAlpha(0xFF6E646A, fade), false);
        String right = "Copyright Mojang AB. Do not distribute!";
        ctx.drawText(textRenderer, right, width - textRenderer.getWidth(right) - 6, height - 12, Theme.withAlpha(0xFF6E646A, fade), false);
    }
}
