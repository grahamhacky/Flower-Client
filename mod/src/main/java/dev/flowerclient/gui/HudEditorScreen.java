package dev.flowerclient.gui;

import dev.flowerclient.config.ConfigManager;
import dev.flowerclient.module.HudModule;
import dev.flowerclient.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

/** Drag HUD elements around, scroll to resize, right-click to toggle their background. */
public class HudEditorScreen extends Screen {
    private static final int SNAP = 4;
    private final Screen parent;
    private HudModule dragging;
    private double offX, offY;
    private boolean snapX, snapY;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("Edit HUD"));
        this.parent = parent;
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        addDrawableChild(new FlowerButton(width / 2 - 45, 8, 90, 18, net.minecraft.text.Text.literal("Reset layout"), b -> {
            dev.flowerclient.module.ModuleManager.resetLayout();
            dev.flowerclient.module.ModuleManager.layoutDefaults(client);
        }, FlowerButton.Style.CENTER));
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (client != null && client.world != null) ctx.fill(0, 0, width, height, 0x30000000);
        else super.renderBackground(ctx, mouseX, mouseY, delta);
    }

    private List<HudModule> enabled() {
        return ModuleManager.hud().stream().filter(HudModule::isEnabled).toList();
    }

    private HudModule at(double mx, double my) {
        List<HudModule> list = enabled();
        for (int i = list.size() - 1; i >= 0; i--) {
            HudModule m = list.get(i);
            if (Theme.inside(mx, my, Math.round(m.x), Math.round(m.y), m.scaledWidth(client), m.scaledHeight(client))) return m;
        }
        return null;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        if (dragging != null) {
            if (snapX) ctx.fill(width / 2, 0, width / 2 + 1, height, Theme.withAlpha(Theme.ACCENT_DEEP, 0.7f));
            if (snapY) ctx.fill(0, height / 2, width, height / 2 + 1, Theme.withAlpha(Theme.ACCENT_DEEP, 0.7f));
        }

        HudModule hover = dragging != null ? dragging : at(mouseX, mouseY);
        for (HudModule m : enabled()) {
            m.renderAt(ctx, client);
            int x = Math.round(m.x) - 1, y = Math.round(m.y) - 1;
            int w = m.scaledWidth(client) + 2, h = m.scaledHeight(client) + 2;
            Theme.outline(ctx, x, y, w, h, m == hover ? Theme.ACCENT_DEEP : 0x60FFFFFF);
            if (m == hover) {
                String tag = m.getName() + "  " + Math.round(m.scale * 100) + "%";
                int ty = y > 12 ? y - 11 : y + h + 2;
                ctx.drawText(textRenderer, tag, x, ty, Theme.ACCENT, true);
            }
        }

        String hint = "Drag to move  ·  Scroll to resize  ·  Right-click: background  ·  Esc to save";
        int hw = textRenderer.getWidth(hint) + 16;
        int hx = (width - hw) / 2, hy = height - 26;
        Theme.panel(ctx, hx, hy, hw, 16, 0xB0141015);
        Theme.outline(ctx, hx, hy, hw, 16, Theme.GLASS_EDGE);
        ctx.drawText(textRenderer, hint, hx + 8, hy + 4, Theme.MUTED, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        HudModule m = at(mx, my);
        if (m == null) return false;
        if (button == 1) { m.background = !m.background; return true; }
        if (button == 0) { dragging = m; offX = mx - m.x; offY = my - m.y; return true; }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging == null) return super.mouseDragged(mx, my, button, dx, dy);
        int w = dragging.scaledWidth(client), h = dragging.scaledHeight(client);
        float nx = (float) (mx - offX), ny = (float) (my - offY);

        snapX = Math.abs(nx + w / 2f - width / 2f) < SNAP;
        snapY = Math.abs(ny + h / 2f - height / 2f) < SNAP;
        if (snapX) nx = width / 2f - w / 2f;
        if (snapY) ny = height / 2f - h / 2f;
        if (nx < SNAP) nx = 2;
        if (ny < SNAP) ny = 2;
        if (nx + w > width - SNAP) nx = width - w - 2;
        if (ny + h > height - SNAP) ny = height - h - 2;

        dragging.x = nx;
        dragging.y = ny;
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        snapX = snapY = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        HudModule m = at(mx, my);
        if (m == null || v == 0) return false;
        float cx = m.x + m.scaledWidth(client) / 2f, cy = m.y + m.scaledHeight(client) / 2f;
        m.scale = Math.round(Math.max(0.5f, Math.min(3f, m.scale + (v > 0 ? 0.1f : -0.1f))) * 10f) / 10f;
        m.x = cx - m.scaledWidth(client) / 2f;
        m.y = cy - m.scaledHeight(client) / 2f;
        m.clampToScreen(client);
        return true;
    }

    @Override
    public void close() {
        ConfigManager.save();
        client.setScreen(parent);
    }
}
