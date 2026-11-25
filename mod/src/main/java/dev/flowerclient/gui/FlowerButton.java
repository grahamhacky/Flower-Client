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
        float a = alpha;
        var tr = MinecraftClient.getInstance().textRenderer;

        if (style == Style.PRIMARY) {
            int top = Theme.lerpColor(0xFFF08CB0, 0xFFF7B6CC, hover), bottom = Theme.lerpColor(0xFFE85D8A, 0xFFF08CB0, hover);
            ctx.fillGradient(x + 1, y, x + w - 1, y + h, Theme.withAlpha(top, a), Theme.withAlpha(bottom, a));
            ctx.fill(x, y + 1, x + 1, y + h - 1, Theme.withAlpha(bottom, a));
            ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, Theme.withAlpha(bottom, a));
            ctx.fill(x + 1, y, x + w - 1, y + 1, Theme.withAlpha(0x66FFFFFF, a));
            ctx.drawText(tr, getMessage(), x + 10, y + (h - 8) / 2, Theme.withAlpha(0xFF1A0A10, a), false);
            return;
        }

        int bg = Theme.lerpColor(0x22FFFFFF, 0x3CFFFFFF, hover);
        Theme.panel(ctx, x, y, w, h, active ? Theme.withAlpha(bg, a) : Theme.withAlpha(0x14FFFFFF, a));
        Theme.outline(ctx, x, y, w, h, Theme.withAlpha(Theme.lerpColor(0x26FFFFFF, 0x66F4A6C0, hover), a));
        ctx.fill(x + 1, y + 1, x + w - 1, y + 2, Theme.withAlpha(0x12FFFFFF, a));
        int bar = Math.round(2 + hover * 4);
        if (style != Style.CENTER) ctx.fill(x + 3, y + (h - bar) / 2 - 2, x + 4, y + (h + bar) / 2 + 2, Theme.withAlpha(Theme.ACCENT, a * (0.35f + 0.65f * hover)));
        int col = active ? Theme.lerpColor(0xFFE9E0E4, 0xFFFFFFFF, hover) : 0xFF8A8086;
        if (style == Style.CENTER) {
            int tw = tr.getWidth(getMessage());
            ctx.drawText(tr, getMessage(), x + (w - tw) / 2, y + (h - 8) / 2, Theme.withAlpha(col, a), false);
        } else {
            ctx.drawText(tr, getMessage(), x + 10 + Math.round(hover * 2), y + (h - 8) / 2, Theme.withAlpha(col, a), false);
        }
    }
}
