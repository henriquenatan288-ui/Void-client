package dev.voidclient.gui;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BlockListSetting;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.ModeSetting;
import dev.voidclient.core.setting.NumberSetting;
import dev.voidclient.core.setting.Setting;
import dev.voidclient.gui.utils.RenderUtils;
import dev.voidclient.modules.utility.AntVanish;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class ModuleSettingsScreen extends Screen {
    private static final int HEADER = 46, FOOTER = 34, PAD = 12, BAR_W = 14;

    private final Screen parent;
    private final Module module;

    private int scroll;
    private boolean pressing, moved, draggingBar;
    private NumberSetting dragSlider;
    private double pressX, pressY;
    private int scrollAtPress;

    public ModuleSettingsScreen(Screen parent, Module module) {
        super(Component.literal(module.getName() + " settings"));
        this.parent = parent;
        this.module = module;
    }

    private int panelW() { return Math.min(width - 16, 380); }
    private int panelX() { return (width - panelW()) / 2; }
    private int panelY() { return 6; }
    private int panelH() { return height - 12; }
    private int bodyY() { return panelY() + HEADER; }
    private int bodyH() { return panelH() - HEADER - FOOTER; }
    private int innerW() { return panelW() - PAD * 2 - BAR_W - 4; }

    private static int rowH(Setting<?> s) {
        if (s instanceof NumberSetting) return 36;
        if (s instanceof ColorSetting) return 42;
        return 26;
    }

    private int contentH() {
        int h = 4;
        for (Setting<?> s : module.getSettings()) h += rowH(s);
        return h + 4;
    }

    private int maxScroll() { return Math.max(0, contentH() - bodyH()); }
    private void clamp() { scroll = Math.max(0, Math.min(scroll, maxScroll())); }
    private static int bg() { return 0xFF000000 | (Theme.BG_PANEL() & 0xFFFFFF); }

    private int thumbH() {
        int max = maxScroll();
        if (max <= 0) return bodyH();
        return Math.max(20, (int) (bodyH() * (bodyH() / (float) Math.max(1, contentH()))));
    }

    private int thumbY() {
        int max = maxScroll();
        if (max <= 0) return bodyY();
        return bodyY() + (int) ((bodyH() - thumbH()) * (scroll / (float) max));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        clamp();
        ctx.fill(0, 0, width, height, 0xD2000000);
        int px = panelX(), py = panelY(), pw = panelW(), ph = panelH();
        RenderUtils.drawPanel(ctx, px, py, pw, ph, 8, bg());

        int rowX = px + PAD;
        int y = bodyY() - scroll + 4;
        for (Setting<?> s : module.getSettings()) {
            int h = rowH(s);
            if (y + h > bodyY() && y < bodyY() + bodyH()) drawRow(ctx, s, rowX, y, mx, my);
            y += h;
        }
        if (module.getSettings().isEmpty()) {
            RenderUtils.drawText(ctx, font, "No settings", rowX, bodyY() + 12, Theme.TEXT_MUTED(), false);
        }

        RenderUtils.fill(ctx, px, py, pw, HEADER, bg());
        RenderUtils.fill(ctx, px, py + ph - FOOTER, pw, FOOTER, bg());
        RenderUtils.fill(ctx, px + 8, py + HEADER - 1, pw - 16, 1, Theme.BORDER());
        RenderUtils.fill(ctx, px + 8, py + ph - FOOTER, pw - 16, 1, Theme.BORDER());

        RenderUtils.drawText(ctx, font, module.getName(), rowX, py + 10, Theme.TEXT_PRIMARY(), false);
        RenderUtils.drawText(ctx, font, module.getDescription(), rowX, py + 24, Theme.TEXT_MUTED(), false);
        drawToggle(ctx, px + pw - PAD - 22, py + 12, module.isEnabled());

        int bx = px + pw - PAD - 64, by = py + ph - FOOTER + 8;
        boolean hov = RenderUtils.hovered(mx, my, bx, by, 64, 18);
        RenderUtils.fillRounded(ctx, bx, by, 64, 18, 4, hov ? Theme.BG_SELECTED() : Theme.BG_CARD());
        RenderUtils.drawText(ctx, font, "DONE", bx + 20, by + 5, Theme.TEXT_PRIMARY(), false);
        RenderUtils.drawText(ctx, font, "Drag to scroll", rowX, by + 5, Theme.TEXT_MUTED(), false);

        int barX = px + pw - PAD - BAR_W + 2;
        RenderUtils.fillRounded(ctx, barX, bodyY(), BAR_W - 4, bodyH(), 3, 0x66000000);
        if (maxScroll() > 0) {
            RenderUtils.fillRounded(ctx, barX + 1, thumbY(), BAR_W - 6, thumbH(), 3,
                    draggingBar ? 0xFFFFFFFF : Theme.ACCENT());
        }
    }

    private void drawRow(GuiGraphicsExtractor ctx, Setting<?> s, int x, int y, int mx, int my) {
        int w = innerW();
        RenderUtils.fillRounded(ctx, x - 4, y, w + 8, rowH(s) - 3, 5, Theme.BG_CARD());
        RenderUtils.drawText(ctx, font, s.getName(), x + 4, y + 7, Theme.TEXT_SECONDARY(), false);

        if (s instanceof BooleanSetting b) {
            drawToggle(ctx, x + w - 22, y + 6, b.get());
        } else if (s instanceof NumberSetting n) {
            String v = n.getStep() >= 1 ? String.valueOf(n.getInt())
                    : String.format(java.util.Locale.ROOT, "%.2f", n.get());
            RenderUtils.drawText(ctx, font, v, x + w - RenderUtils.textWidth(font, v), y + 7, Theme.TEXT_PRIMARY(), false);
            int tx = x + 4, ty = y + 23, tw = w - 8;
            RenderUtils.fillRounded(ctx, tx, ty, tw, 4, 2, Theme.SLIDER_TRACK());
            double pct = (n.get() - n.getMin()) / Math.max(0.0001, n.getMax() - n.getMin());
            RenderUtils.fillRounded(ctx, tx, ty, Math.max(3, (int) (tw * pct)), 4, 2, Theme.SLIDER_FILL());
            RenderUtils.fillRounded(ctx, tx + (int) (tw * pct) - 4, ty - 3, 8, 10, 4, Theme.TEXT_PRIMARY());
        } else if (s instanceof ModeSetting m) {
            String v = m.get();
            int pw2 = RenderUtils.textWidth(font, v) + 14;
            RenderUtils.fillRounded(ctx, x + w - pw2, y + 4, pw2, 16, 5, Theme.BG_SELECTED());
            RenderUtils.drawText(ctx, font, v, x + w - pw2 + 7, y + 8, Theme.TEXT_PRIMARY(), false);
        } else if (s instanceof ColorSetting c) {
            int n = ColorSetting.paletteSize();
            int sw = Math.max(10, Math.min(16, (w - 8) / Math.max(1, n) - 2));
            for (int i = 0; i < n; i++) {
                int sx = x + 4 + i * (sw + 2);
                RenderUtils.fillRounded(ctx, sx, y + 22, sw, sw, 3, ColorSetting.paletteColor(i));
                RenderUtils.outline(ctx, sx, y + 22, sw, sw,
                        ColorSetting.paletteColor(i) == c.get() ? Theme.TEXT_PRIMARY() : Theme.BORDER());
            }
        } else if (s instanceof BlockListSetting b) {
            button(ctx, x + w - 84, y + 4, 84, b.getSelectedIds().size() + " blocks >", mx, my);
        } else if (s instanceof AntVanish.StaffListSetting st) {
            button(ctx, x + w - 84, y + 4, 84, st.names().size() + " staff >", mx, my);
        }
    }

    private void button(GuiGraphicsExtractor ctx, int x, int y, int w, String label, int mx, int my) {
        boolean hov = RenderUtils.hovered(mx, my, x, y, w, 16);
        RenderUtils.fillRounded(ctx, x, y, w, 16, 5, hov ? Theme.BG_SELECTED() : Theme.BG_CARD_HOVER());
        RenderUtils.drawText(ctx, font, label, x + 7, y + 4, Theme.TEXT_PRIMARY(), false);
    }

    private void drawToggle(GuiGraphicsExtractor ctx, int x, int y, boolean on) {
        RenderUtils.fillRounded(ctx, x, y, 22, 12, 6, on ? Theme.TOGGLE_ON() : Theme.TOGGLE_OFF());
        RenderUtils.fillRounded(ctx, on ? x + 11 : x + 2, y + 2, 8, 8, 4,
                on ? Theme.TOGGLE_KNOB_ON() : Theme.TOGGLE_KNOB_OFF());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        double mx = click.x(), my = click.y();
        pressX = mx; pressY = my; moved = false; scrollAtPress = scroll;
        pressing = false; draggingBar = false; dragSlider = null;

        int px = panelX(), pw = panelW(), py = panelY(), ph = panelH();

        if (RenderUtils.hovered((int) mx, (int) my, px + pw - PAD - 64, py + ph - FOOTER + 8, 64, 18)) {
            onClose(); return true;
        }
        if (RenderUtils.hovered((int) mx, (int) my, px + pw - PAD - 22, py + 12, 22, 12)) {
            module.toggle(); return true;
        }

        int barX = px + pw - PAD - BAR_W + 2;
        if (mx >= barX && mx <= barX + BAR_W && my >= bodyY() && my <= bodyY() + bodyH()) {
            draggingBar = true;
            scrollFromBar(my);
            return true;
        }

        if (my < bodyY() || my > bodyY() + bodyH()) return super.mouseClicked(click, doubled);

        int y = bodyY() - scroll + 4, rowX = px + PAD;
        for (Setting<?> s : module.getSettings()) {
            int h = rowH(s);
            if (s instanceof NumberSetting n && my >= y + 16 && my <= y + h) {
                dragSlider = n;
                setSlider(n, mx, rowX + 4, innerW() - 8);
                return true;
            }
            y += h;
        }
        pressing = true;
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        if (draggingBar) { scrollFromBar(click.y()); return true; }
        if (dragSlider != null) { setSlider(dragSlider, click.x(), panelX() + PAD + 4, innerW() - 8); return true; }
        if (pressing) {
            if (Math.abs(click.y() - pressY) > 4 || Math.abs(click.x() - pressX) > 4) moved = true;
            if (moved) { scroll = scrollAtPress + (int) (pressY - click.y()); clamp(); }
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        boolean tap = pressing && !moved && click.button() == 0;
        pressing = false; draggingBar = false; dragSlider = null;
        if (tap) tapAt(click.x(), click.y());
        moved = false;
        return super.mouseReleased(click);
    }

    private void tapAt(double mx, double my) {
        int y = bodyY() - scroll + 4, rowX = panelX() + PAD, w = innerW();
        for (Setting<?> s : module.getSettings()) {
            int h = rowH(s);
            if (my >= y && my < y + h - 3) {
                if (s instanceof BooleanSetting b) b.toggle();
                else if (s instanceof ModeSetting m) m.cycle();
                else if (s instanceof ColorSetting c) {
                    int n = ColorSetting.paletteSize();
                    int sw = Math.max(10, Math.min(16, (w - 8) / Math.max(1, n) - 2));
                    boolean hit = false;
                    for (int i = 0; i < n; i++) {
                        int sx = rowX + 4 + i * (sw + 2);
                        if (mx >= sx && mx <= sx + sw) { c.set(ColorSetting.paletteColor(i)); hit = true; break; }
                    }
                    if (!hit) c.cyclePalette();
                } else if (s instanceof BlockListSetting b && minecraft != null) {
                    minecraft.gui.setScreen(new BlockSelectionScreen(this, b));
                } else if (s instanceof AntVanish.StaffListSetting st && minecraft != null) {
                    minecraft.gui.setScreen(new AntVanish.StaffListScreen(this, st));
                }
                return;
            }
            y += h;
        }
    }

    private void setSlider(NumberSetting n, double mx, int trackX, int trackW) {
        double pct = Math.max(0, Math.min(1, (mx - trackX) / (double) trackW));
        double v = n.getMin() + pct * (n.getMax() - n.getMin());
        if (n.getStep() > 0) v = Math.round(v / n.getStep()) * n.getStep();
        n.set(Math.max(n.getMin(), Math.min(n.getMax(), v)));
    }

    private void scrollFromBar(double my) {
        int max = maxScroll();
        if (max <= 0) { scroll = 0; return; }
        double track = bodyH() - thumbH();
        double t = track <= 0 ? 0 : (my - thumbH() / 2.0 - bodyY()) / track;
        scroll = (int) Math.round(Math.max(0, Math.min(1, t)) * max);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll -= (int) (vertical * 24);
        clamp();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int k = input.key();
        if (k == GLFW.GLFW_KEY_ESCAPE || k == GLFW.GLFW_KEY_RIGHT_SHIFT) { onClose(); return true; }
        if (k == GLFW.GLFW_KEY_UP) { scroll -= 24; clamp(); return true; }
        if (k == GLFW.GLFW_KEY_DOWN) { scroll += 24; clamp(); return true; }
        return super.keyPressed(input);
    }

    @Override
    public void onClose() {
        if (VoidClient.CONFIG_MANAGER != null) VoidClient.CONFIG_MANAGER.save();
        if (minecraft != null) minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
