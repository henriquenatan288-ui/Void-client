package dev.voidclient.gui;

import dev.voidclient.core.Module;
import dev.voidclient.core.setting.*;
import dev.voidclient.gui.utils.RenderUtils;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Scrollable, stand-alone module settings screen for Minecraft 1.21.11. */
public final class ModuleSettingsScreen extends Screen {
    private static final int PANEL_W = 310;
    private static final int HEADER_H = 34;
    private static final int FOOTER_H = 24;

    private final Screen parent;
    private final Module module;
    private int scroll;

    public ModuleSettingsScreen(Screen parent, Module module) {
        super(Text.literal("Void Client - " + module.getName()));
        this.parent = parent;
        this.module = module;
    }

    private int panelX() { return Math.max(8, (width - PANEL_W) / 2); }
    private int panelH() { return Math.max(160, Math.min(500, height - 20)); }
    private int panelY() { return Math.max(8, (height - panelH()) / 2); }
    private int contentTop() { return panelY() + HEADER_H + 4; }
    private int contentBottom() { return panelY() + panelH() - FOOTER_H; }

    private static int rowHeight(Setting<?> setting) {
        if (setting instanceof NumberSetting) return 29;
        if (setting instanceof ColorSetting) return 31;
        if (setting instanceof BlockListSetting) return 27;
        return 23;
    }

    private int contentHeight() {
        int total = 0;
        for (Setting<?> setting : module.getSettings()) total += rowHeight(setting);
        return total;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - Math.max(0, contentBottom() - contentTop()));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xC8000000);
        int x = panelX(), y = panelY(), w = Math.min(PANEL_W, width - 16), h = panelH();
        RenderUtils.drawPanel(ctx, x, y, w, h, 8, Theme.BG_PANEL);
        RenderUtils.drawText(ctx, textRenderer, module.getName(), x + 12, y + 10, Theme.TEXT_PRIMARY, false);
        RenderUtils.drawText(ctx, textRenderer, module.isEnabled() ? "ON" : "OFF",
                x + w - 48, y + 10, module.isEnabled() ? Theme.ACCENT : Theme.TEXT_MUTED, false);
        RenderUtils.fill(ctx, x + 10, y + HEADER_H, w - 20, 1, Theme.BORDER);

        int top = contentTop(), bottom = contentBottom();
        ctx.enableScissor(x + 6, top, x + w - 6, bottom);
        int cy = top - scroll;
        List<Setting<?>> settings = module.getSettings();
        if (settings.isEmpty()) {
            RenderUtils.drawText(ctx, textRenderer, "Este módulo não possui configurações.",
                    x + 12, cy + 7, Theme.TEXT_MUTED, false);
        }
        for (Setting<?> setting : settings) {
            int rh = rowHeight(setting);
            if (cy + rh >= top && cy <= bottom) renderSetting(ctx, setting, x + 12, cy, w - 24, rh, mouseX, mouseY);
            cy += rh;
        }
        ctx.disableScissor();

        int footerY = y + h - FOOTER_H;
        RenderUtils.fill(ctx, x + 10, footerY, w - 20, 1, Theme.BORDER);
        RenderUtils.drawText(ctx, textRenderer,
                maxScroll() > 0 ? "Rolar: mouse  |  ESC: voltar" : "ESC: voltar",
                x + 12, footerY + 8, Theme.TEXT_MUTED, false);
        if (maxScroll() > 0) {
            int trackY = top + 2, trackH = Math.max(10, bottom - top - 4);
            int thumbH = Math.max(18, trackH * trackH / Math.max(trackH, contentHeight()));
            int thumbY = trackY + (trackH - thumbH) * scroll / Math.max(1, maxScroll());
            RenderUtils.fillRounded(ctx, x + w - 7, thumbY, 3, thumbH, 2, Theme.ACCENT);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderSetting(DrawContext ctx, Setting<?> setting, int x, int y, int w, int rh,
                               int mouseX, int mouseY) {
        boolean hovered = RenderUtils.hovered(mouseX, mouseY, x, y, w, rh - 1);
        if (hovered) RenderUtils.fillRounded(ctx, x, y, w, rh - 1, 4, Theme.BG_CARD_HOVER);
        RenderUtils.drawText(ctx, textRenderer, setting.getName(), x + 3, y + 5, Theme.TEXT_SECONDARY, false);

        if (setting instanceof BooleanSetting bs) {
            int tx = x + w - 26;
            RenderUtils.fillRounded(ctx, tx, y + 5, 22, 12, 6, bs.get() ? Theme.TOGGLE_ON : Theme.TOGGLE_OFF);
            RenderUtils.fillRounded(ctx, bs.get() ? tx + 11 : tx + 2, y + 7, 8, 8, 4,
                    bs.get() ? Theme.TOGGLE_KNOB_ON : Theme.TOGGLE_KNOB_OFF);
        } else if (setting instanceof NumberSetting ns) {
            String value = ns.getStep() >= 1 ? String.valueOf(ns.getInt())
                    : String.format(java.util.Locale.ROOT, "%.2f", ns.get());
            RenderUtils.drawText(ctx, textRenderer, value,
                    x + w - RenderUtils.textWidth(textRenderer, value) - 4, y + 5,
                    Theme.TEXT_PRIMARY, false);
            int bx = x + 3, by = y + 20, bw = w - 6;
            RenderUtils.fillRounded(ctx, bx, by, bw, 3, 1, Theme.SLIDER_TRACK);
            double pct = (ns.get() - ns.getMin()) / Math.max(0.0001, ns.getMax() - ns.getMin());
            RenderUtils.fillRounded(ctx, bx, by, Math.max(2, (int) (bw * pct)), 3, 1, Theme.SLIDER_FILL);
        } else if (setting instanceof ModeSetting ms) {
            String value = ms.get() + "  ›";
            int tw = RenderUtils.textWidth(textRenderer, value);
            RenderUtils.drawText(ctx, textRenderer, value, x + w - tw - 4, y + 5, Theme.TEXT_PRIMARY, false);
        } else if (setting instanceof ColorSetting cs) {
            int swatchX = x + Math.max(84, w - ColorSetting.paletteSize() * 13 - 4);
            int swatchY = y + 8;
            for (int i = 0; i < ColorSetting.paletteSize(); i++) {
                int sx = swatchX + i * 13;
                boolean selected = ColorSetting.paletteColor(i) == cs.get();
                RenderUtils.fillRounded(ctx, sx, swatchY, 10, 10, 3, ColorSetting.paletteColor(i));
                RenderUtils.outline(ctx, sx, swatchY, 10, 10, selected ? Theme.TEXT_PRIMARY : Theme.BORDER);
            }
        } else if (setting instanceof BlockListSetting bs) {
            String label = bs.getSelectedIds().size() + " selected";
            int bw = 88, bx = x + w - bw;
            RenderUtils.fillRounded(ctx, bx, y + 3, bw, 17, 4,
                    RenderUtils.hovered(mouseX, mouseY, bx, y + 3, bw, 17) ? Theme.BG_SELECTED : Theme.BG_CARD);
            RenderUtils.drawText(ctx, textRenderer, label, bx + 5, y + 7, Theme.TEXT_PRIMARY, false);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int button = click.button();
        int x = panelX(), w = Math.min(PANEL_W, width - 16);
        int y = panelY();

        if (RenderUtils.hovered((int) mx, (int) my, x + w - 28, y + 3, 22, HEADER_H - 4)) {
            close();
            return true;
        }
        if (button != 0 || mx < x || mx > x + w || my < contentTop() || my > contentBottom()) {
            return super.mouseClicked(click, doubled);
        }

        int cy = contentTop() - scroll;
        for (Setting<?> setting : module.getSettings()) {
            int rh = rowHeight(setting);
            if (my >= cy && my < cy + rh) {
                if (setting instanceof BooleanSetting bs) {
                    bs.toggle();
                    VoidClientSave.save();
                    return true;
                }
                if (setting instanceof NumberSetting ns) {
                    int bx = x + 15, bw = w - 30;
                    double pct = Math.max(0, Math.min(1, (mx - bx) / (double) bw));
                    ns.set(ns.getMin() + pct * (ns.getMax() - ns.getMin()));
                    VoidClientSave.save();
                    return true;
                }
                if (setting instanceof ModeSetting ms) {
                    ms.cycle();
                    VoidClientSave.save();
                    return true;
                }
                if (setting instanceof ColorSetting cs) {
                    int swatchX = x + 12 + Math.max(84, (w - 24) - ColorSetting.paletteSize() * 13 - 4);
                    int swatchY = cy + 8;
                    for (int i = 0; i < ColorSetting.paletteSize(); i++) {
                        int sx = swatchX + i * 13;
                        if (RenderUtils.hovered((int) mx, (int) my, sx, swatchY, 10, 10)) {
                            cs.set(ColorSetting.paletteColor(i));
                            VoidClientSave.save();
                            return true;
                        }
                    }
                    return true;
                }
                if (setting instanceof BlockListSetting bs) {
                    client.setScreen(new BlockSelectionScreen(this, bs));
                    return true;
                }
                return true;
            }
            cy += rh;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(verticalAmount) * 24));
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE || input.key() == GLFW.GLFW_KEY_BACKSPACE) {
            close();
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_DOWN) {
            scroll = Math.min(maxScroll(), scroll + 24);
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_UP) {
            scroll = Math.max(0, scroll - 24);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() { return false; }

    private static final class VoidClientSave {
        static void save() {
            if (dev.voidclient.VoidClient.CONFIG_MANAGER != null) {
                dev.voidclient.VoidClient.CONFIG_MANAGER.save();
            }
        }
    }
}