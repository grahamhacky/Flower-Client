package dev.flowerclient.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Glass button with a petal accent bar that grows on hover. Keeps vanilla keyboard nav and narration. */
public class FlowerButton extends ButtonWidget {
    public enum Style { GLASS, PRIMARY, CENTER }

    private final Style style;
    private float hover;

    public FlowerButton(int x, int y, int w, int h, Text text, PressAction onPress, Style style) {
        super(x, y, w, h, text, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.style = style;
    }

    public FlowerButton(int x, int y, int w, int h, Text text, PressAction onPress) {
        this(x, y, w, h, text, onPress, Style.GLASS);
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        boolean on = active && (isHovered() || isFocused());
        hover += ((on ? 1f : 0f) - hover) * 0.3f;
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        Text label = getMessage().copy().styled(st -> st.withFont(Theme.FONT));
        float a = alpha;
        var tr = MinecraftClient.getInstance().textRenderer;

        if (a > 0.02f) Theme.shadow(ctx, x, y, w, h, 3);
        if (style == Style.PRIMARY) {
            int top = Theme.lerpColor(0xFFF08CB0, 0xFFF7C3D4, hover), bottom = Theme.lerpColor(0xFFE85D8A, 0xFFF08CB0, hover);
            Theme.roundedGradient(ctx, x, y, w, h, 3, Theme.withAlpha(top, a), Theme.withAlpha(bottom, a));
            Theme.sheen(ctx, x, y, w, 3, Theme.withAlpha(0x70FFFFFF, a));
            ctx.drawText(tr, label, x + 11, y + (h - 8) / 2, Theme.withAlpha(0xFF1A0A10, a), false);
            return;
        }

        int bgTop = Theme.lerpColor(0x24FFFFFF, 0x40FFFFFF, hover), bgBot = Theme.lerpColor(0x16FFFFFF, 0x28FFFFFF, hover);
        Theme.roundedGradient(ctx, x, y, w, h, 3, Theme.withAlpha(active ? bgTop : 0x14FFFFFF, a), Theme.withAlpha(active ? bgBot : 0x0CFFFFFF, a));
        Theme.roundedOutline(ctx, x, y, w, h, 3, Theme.withAlpha(Theme.lerpColor(0x26FFFFFF, 0x70F4A6C0, hover), a));
        Theme.sheen(ctx, x, y, w, 3, Theme.withAlpha(0x14FFFFFF, a));
        int bar = Math.round(3 + hover * 5);
        if (style != Style.CENTER) Theme.roundedRect(ctx, x + 4, y + (h - bar) / 2, 2, bar, 1, Theme.withAlpha(Theme.ACCENT, a * (0.4f + 0.6f * hover)));
        int col = active ? Theme.lerpColor(0xFFE9E0E4, 0xFFFFFFFF, hover) : 0xFF8A8086;
        if (style == Style.CENTER) {
            int tw = tr.getWidth(label);
            ctx.drawText(tr, label, x + (w - tw) / 2, y + (h - 8) / 2, Theme.withAlpha(col, a), false);
        } else {
            ctx.drawText(tr, label, x + 10 + Math.round(hover * 2), y + (h - 8) / 2, Theme.withAlpha(col, a), false);
        }
    }
}
