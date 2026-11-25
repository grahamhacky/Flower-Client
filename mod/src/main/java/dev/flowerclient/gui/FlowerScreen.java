package dev.flowerclient.gui;

import dev.flowerclient.FlowerClient;
import dev.flowerclient.config.ConfigManager;
import dev.flowerclient.module.Category;
import dev.flowerclient.module.Module;
import dev.flowerclient.module.ModuleManager;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The Right-Shift menu: module tiles grouped by category, plus a shortcut to the HUD editor. */
public class FlowerScreen extends Screen {
    private static final int TILE_H = 36, GAP = 6, HEADER = 30, TABS = 18, FOOTER = 16;

    private final Screen parent;
    private final long openedAt = System.currentTimeMillis();
    private final Map<Module, Float> anim = new HashMap<>();
    private Category filter = null; // null = all
    private float scroll, scrollTarget;

    private int px, py, pw, ph;

    public FlowerScreen(Screen parent) {
        super(Text.literal("Flower Client"));
        this.parent = parent;
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        pw = Math.min(380, width - 24);
        ph = Math.min(250, height - 24);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
    }

    private List<Module> visible() {
        List<Module> out = new ArrayList<>();
        for (Module m : ModuleManager.all()) if (filter == null || m.getCategory() == filter) out.add(m);
        return out;
    }

    private int cols() { return Math.max(1, (pw - 16 + GAP) / (112 + GAP)); }
    private int tileW() { int c = cols(); return (pw - 16 - GAP * (c - 1)) / c; }
    private int gridTop() { return py + HEADER + TABS + 6; }
    private int gridBottom() { return py + ph - FOOTER - 4; }

    private int contentHeight() {
        int rows = (visible().size() + cols() - 1) / cols();
        return rows * TILE_H + Math.max(0, rows - 1) * GAP;
    }

    /** Clear glass: just the vanilla blur (uses the Menu Background Blurriness setting) and a faint tint. */
    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (client == null || client.world == null) renderPanoramaBackground(ctx, delta);
        applyBlur(delta);
        ctx.fill(0, 0, width, height, 0x14000000);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        TextRenderer tr = textRenderer;
        float t = Math.min(1f, (System.currentTimeMillis() - openedAt) / 180f);
        float ease = 1f - (1f - t) * (1f - t) * (1f - t);
        int slide = Math.round((1f - ease) * 10);

        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, slide, 0);

        Theme.shadow(ctx, px, py, pw, ph, 5);
        Theme.roundedGradient(ctx, px, py, pw, ph, 5, Theme.GLASS_TOP, Theme.GLASS_BOT);
        Theme.roundedOutline(ctx, px, py, pw, ph, 5, Theme.GLASS_EDGE);
        Theme.sheen(ctx, px, py, pw, 5, Theme.GLASS_SHINE);

        // Header: wordmark + Edit HUD
        int hx = px + 10, hy = py + 11;
        ctx.drawText(tr, "✿", hx, hy, Theme.ACCENT_DEEP, false);
        ctx.drawText(tr, "Flower", hx + 11, hy, Theme.TEXT, false);
        ctx.drawText(tr, "client", hx + 11 + tr.getWidth("Flower "), hy, Theme.MUTED, false);

        String edit = "Edit HUD";
        int ew = tr.getWidth(edit) + 14, ex = px + pw - ew - 8, ey = py + 7;
        boolean eh = Theme.inside(mouseX, mouseY - slide, ex, ey, ew, 16);
        if (eh) Theme.roundedGradient(ctx, ex, ey, ew, 16, 3, Theme.ACCENT_SOFT, Theme.ACCENT);
        else Theme.roundedGradient(ctx, ex, ey, ew, 16, 3, Theme.GLASS_HOVER_TOP, Theme.GLASS_HOVER_BOT);
        Theme.sheen(ctx, ex, ey, ew, 3, 0x22FFFFFF);
        ctx.drawText(tr, edit, ex + 7, ey + 4, eh ? 0xFF1A1418 : Theme.TEXT, false);

        ctx.fill(px + 1, py + HEADER - 1, px + pw - 1, py + HEADER, Theme.GLASS_TILE);

        // Tabs
        int tx = px + 10, ty = py + HEADER + 5;
        for (Category c : tabs()) {
            String label = c == null ? "All" : c.label;
            int w = tr.getWidth(label);
            boolean active = filter == c;
            boolean hov = Theme.inside(mouseX, mouseY - slide, tx - 2, ty - 2, w + 4, 13);
            ctx.drawText(tr, label, tx, ty, active ? Theme.TEXT : hov ? Theme.ACCENT_SOFT : Theme.MUTED, false);
            if (active) Theme.roundedRect(ctx, tx - 1, ty + 10, w + 2, 2, 1, Theme.ACCENT_DEEP);
            tx += w + 14;
        }

        // Grid
        scroll += (scrollTarget - scroll) * 0.4f;
        int top = gridTop(), bottom = gridBottom();
        ctx.enableScissor(px + 1, top, px + pw - 1, bottom);
        List<Module> mods = visible();
        int cols = cols(), tw = tileW();
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int x = px + 8 + (i % cols) * (tw + GAP);
            int y = top + (i / cols) * (TILE_H + GAP) - Math.round(scroll);
            if (y + TILE_H < top || y > bottom) continue;
            boolean hov = mouseY - slide >= top && mouseY - slide < bottom && Theme.inside(mouseX, mouseY - slide, x, y, tw, TILE_H);
            drawTile(ctx, m, x, y, tw, hov);
        }
        ctx.disableScissor();

        // Scrollbar
        int ch = contentHeight(), view = bottom - top;
        if (ch > view) {
            int barH = Math.max(14, view * view / ch);
            int barY = top + Math.round((view - barH) * (scroll / (ch - view)));
            ctx.fill(px + pw - 4, barY, px + pw - 2, barY + barH, Theme.GLASS_EDGE);
        }

        // Footer
        String foot = "Right Shift to close  ·  " + FlowerClient.NAME + " " + FlowerClient.VERSION;
        ctx.drawText(tr, foot, px + 10, py + ph - FOOTER + 2, Theme.MUTED, false);

        ctx.getMatrices().pop();
    }

    private void drawTile(DrawContext ctx, Module m, int x, int y, int w, boolean hover) {
        TextRenderer tr = textRenderer;
        float a = anim.getOrDefault(m, m.isEnabled() ? 1f : 0f);
        a += ((m.isEnabled() ? 1f : 0f) - a) * 0.3f;
        anim.put(m, a);

        if (hover) Theme.roundedGradient(ctx, x, y, w, TILE_H, 3, Theme.GLASS_HOVER_TOP, Theme.GLASS_HOVER_BOT);
        else Theme.roundedGradient(ctx, x, y, w, TILE_H, 3, Theme.GLASS_TILE_TOP, Theme.GLASS_TILE_BOT);
        Theme.sheen(ctx, x, y, w, 3, 0x14FFFFFF);
        // left accent bar shows enabled state
        Theme.roundedRect(ctx, x + 2, y + 4, 2, TILE_H - 8, 1, Theme.lerpColor(Theme.GLASS_OFF, Theme.ACCENT_DEEP, a));

        ctx.drawText(tr, m.getName(), x + 8, y + 7, Theme.TEXT, false);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x + 8, y + 21, 0);
        ctx.getMatrices().scale(0.75f, 0.75f, 1f);
        ctx.drawText(tr, tr.trimToWidth(m.getDescription(), Math.round((w - 16) / 0.75f)), 0, 0, Theme.MUTED, false);
        ctx.getMatrices().pop();

        // toggle pill
        int sw = 18, sh = 10, sx = x + w - sw - 7, sy = y + 6;
        Theme.roundedRect(ctx, sx, sy, sw, sh, sh / 2, Theme.lerpColor(Theme.GLASS_OFF, Theme.ACCENT_DEEP, a));
        if (a > 0.5f) Theme.sheen(ctx, sx, sy, sw, sh / 2, 0x33FFFFFF);
        int knob = sx + 1 + Math.round(a * (sw - 9));
        Theme.roundedRect(ctx, knob, sy + 1, 8, 8, 4, Theme.lerpColor(0xFFBBB0B6, 0xFFFFFFFF, a));
    }

    private Category[] tabs() { return new Category[]{null, Category.HUD, Category.UTILITY}; }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int slide = 0;
        TextRenderer tr = textRenderer;
        String edit = "Edit HUD";
        int ew = tr.getWidth(edit) + 14, ex = px + pw - ew - 8, ey = py + 7;
        if (button == 0 && Theme.inside(mx, my, ex, ey, ew, 16)) {
            client.setScreen(new HudEditorScreen(this));
            return true;
        }
        int tx = px + 10, ty = py + HEADER + 5;
        for (Category c : tabs()) {
            int w = tr.getWidth(c == null ? "All" : c.label);
            if (Theme.inside(mx, my, tx - 2, ty - 2, w + 4, 13)) {
                filter = c;
                scroll = scrollTarget = 0;
                return true;
            }
            tx += w + 14;
        }
        int top = gridTop(), bottom = gridBottom();
        if (my >= top && my < bottom) {
            List<Module> mods = visible();
            int cols = cols(), tw = tileW();
            for (int i = 0; i < mods.size(); i++) {
                int x = px + 8 + (i % cols) * (tw + GAP);
                int y = top + (i / cols) * (TILE_H + GAP) - Math.round(scroll) + slide;
                if (Theme.inside(mx, my, x, y, tw, TILE_H)) {
                    mods.get(i).toggle();
                    ConfigManager.save();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        int max = Math.max(0, contentHeight() - (gridBottom() - gridTop()));
        scrollTarget = Math.max(0, Math.min(max, scrollTarget - (float) v * 24));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (FlowerClient.menuKey.matchesKey(key, scan) || key == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void close() {
        ConfigManager.save();
        client.setScreen(parent);
    }
}
