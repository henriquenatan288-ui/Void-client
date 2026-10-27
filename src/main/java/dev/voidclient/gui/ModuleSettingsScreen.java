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
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Dedicated, scrollable settings screen. It replaces the small floating
 * settings panel so lower module cards cannot push controls off-screen.
 */
public final class ModuleSettingsScreen extends Screen {
    private static final int MIN_PANEL_W = 260;
    private static final int MAX_PANEL_W = 420;
    private static final int MIN_PANEL_H = 180;
    private static final int MAX_PANEL_H = 360;
    private static final int HEADER_H = 44;
    private static final int FOOTER_H = 28;

    private final Screen parent;
    private final Module module;
    private int scroll;

    public ModuleSettingsScreen(Screen parent, Module module) {
        super(Component.literal("Void Client - " + module.getName()));
        this.parent = parent;
        this.module = module;
    }

    private int panelWidth() {
        return Math.max(Math.min(MIN_PANEL_W, width - 16), Math.min(MAX_PANEL_W, width - 24));
    }

    private int panelHeight() {
        return Math.max(Math.min(MIN_PANEL_H, height - 16), Math.min(MAX_PANEL_H, height - 20));
    }

    private int panelX() { return (width - panelWidth()) / 2; }
    private int panelY() { return (height - panelHeight()) / 2; }
    private int listTop() { return panelY() + HEADER_H; }
    private int listBottom() { return panelY() + panelHeight() - FOOTER_H; }
    private int listHeight() { return Math.max(28, listBottom() - listTop()); }

    private static int rowHeight(Setting<?> setting) {
        if (setting instanceof NumberSetting) return 40;
        if (setting instanceof ColorSetting) return 38;
        if (setting instanceof BlockListSetting) return 28;
        return 26;
    }

    private int contentHeight() {
        int total = 0;
        for (Setting<?> setting : module.getSettings()) total += rowHeight(setting);
        return total;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - listHeight());
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private int thumbHeight() {
        int total = contentHeight();
        if (total <= listHeight() || total == 0) return listHeight();
        return Math.max(18, listHeight() * listHeight() / total);
    }

    private int thumbY() {
        int max = maxScroll();
        int track = listHeight() - thumbHeight();
        if (max <= 0 || track <= 0) return listTop();
        return listTop() + (int) (track * (scroll / (double) max));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float tickDelta) {
        ctx.fill(0, 0, width, height, 0xC8000000);

        int x = panelX();
        int y = panelY();
        int w = panelWidth();
        int h = panelHeight();
        clampScroll();

        RenderUtils.drawPanel(ctx, x, y, w, h, 8, Theme.BG_PANEL());
        RenderUtils.fillRounded(ctx, x + 1, y + 1, w - 2, HEADER_H - 3, 7, Theme.BG_SIDEBAR());
        RenderUtils.drawText(ctx, font, module.getName(), x + 14, y + 10, Theme.TEXT_PRIMARY(), false);
        RenderUtils.drawText(ctx, font, module.getDescription(), x + 14, y + 25, Theme.TEXT_MUTED(), false);
        drawButton(ctx, mouseX, mouseY, x + w - 30, y + 8, 18, 18, "X");

        int top = listTop();
        int bottom = listBottom();
        RenderUtils.fillRounded(ctx, x + 10, top, w - 20, listHeight(), 4, Theme.BG_CARD());

        int cursor = top - scroll;
        List<Setting<?>> settings = module.getSettings();
        if (settings.isEmpty()) {
            RenderUtils.drawText(ctx, font, "This module has no settings.",
                    x + 18, top + 12, Theme.TEXT_MUTED(), false);
        }

        for (Setting<?> setting : settings) {
            int rh = rowHeight(setting);
            if (cursor >= top && cursor + rh <= bottom) {
                drawSetting(ctx, mouseX, mouseY, x, w, cursor, rh, setting);
            }
            cursor += rh;
        }

        int bx = x + w - 10;
        RenderUtils.fillRounded(ctx, bx - 6, top + 2, 4, Math.max(8, listHeight() - 4),
                2, 0x88000000);
        if (maxScroll() > 0) {
            RenderUtils.fillRounded(ctx, bx - 6, thumbY(), 4, thumbHeight(), 2, Theme.ACCENT());
        }

        int footerY = y + h - FOOTER_H + 5;
        RenderUtils.drawText(ctx, font,
                maxScroll() == 0 ? "All settings visible" : "Scroll to see more settings",
                x + 12, footerY + 5, Theme.TEXT_MUTED(), false);
        drawButton(ctx, mouseX, mouseY, x + w - 68, footerY, 56, 18, "BACK");

        super.extractRenderState(ctx, mouseX, mouseY, tickDelta);
    }

    private void drawSetting(GuiGraphicsExtractor ctx, int mouseX, int mouseY,
                             int panelX, int panelW, int y, int rowH, Setting<?> setting) {
        int x = panelX + 16;
        int right = panelX + panelW - 22;
        boolean hovered = RenderUtils.hovered(mouseX, mouseY, panelX + 12, y, panelW - 28, rowH - 1);
        if (hovered) {
            RenderUtils.fillRounded(ctx, panelX + 12, y, panelW - 28, rowH - 1, 4, Theme.BG_CARD_HOVER());
        }
        RenderUtils.drawText(ctx, font, setting.getName(), x, y + 6, Theme.TEXT_SECONDARY(), false);

        if (setting instanceof BooleanSetting bool) {
            int tx = right - 24;
            drawToggle(ctx, tx, y + 7, bool.get());
        } else if (setting instanceof NumberSetting number) {
            String value = number.getStep() >= 1
                    ? String.valueOf(number.getInt())
                    : String.format(java.util.Locale.ROOT, "%.2f", number.get());
            RenderUtils.drawText(ctx, font, value,
                    right - RenderUtils.textWidth(font, value), y + 6, Theme.TEXT_PRIMARY(), false);
            int barX = x;
            int barY = y + 26;
            int barW = Math.max(40, panelW - 48);
            RenderUtils.fillRounded(ctx, barX, barY, barW, 4, 2, Theme.SLIDER_TRACK());
            double pct = (number.get() - number.getMin())
                    / Math.max(0.0001, number.getMax() - number.getMin());
            RenderUtils.fillRounded(ctx, barX, barY, Math.max(3, (int) (barW * pct)),
                    4, 2, Theme.SLIDER_FILL());
        } else if (setting instanceof ModeSetting mode) {
            String value = mode.get() + "  "
                    + (mode.getModes().indexOf(mode.get()) + 1) + "/" + mode.getModes().size();
            int valueWidth = RenderUtils.textWidth(font, value);
            RenderUtils.fillRounded(ctx, right - valueWidth - 14, y + 4,
                    valueWidth + 10, 18, 4, Theme.BG_SELECTED());
            RenderUtils.drawText(ctx, font, value, right - valueWidth - 9,
                    y + 9, Theme.TEXT_PRIMARY(), false);
        } else if (setting instanceof ColorSetting color) {
            int swatchX = x + 70;
            int swatchY = y + 7;
            int swatch = 9;
            int gap = 2;
            for (int i = 0; i < ColorSetting.paletteSize(); i++) {
                int sx = swatchX + i * (swatch + gap);
                if (sx + swatch > right) break;
                boolean selected = ColorSetting.paletteColor(i) == color.get();
                RenderUtils.fillRounded(ctx, sx, swatchY, swatch, swatch, 3, ColorSetting.paletteColor(i));
                RenderUtils.outline(ctx, sx, swatchY, swatch, swatch,
                        selected ? Theme.TEXT_PRIMARY() : Theme.BORDER());
            }
        } else if (setting instanceof BlockListSetting blockList) {
            String label = blockList.getSelectedIds().size() + " selected";
            int bw = 92;
            int bx = right - bw;
            RenderUtils.fillRounded(ctx, bx, y + 4, bw, 18, 4,
                    hovered ? Theme.BG_SELECTED() : Theme.BG_CARD());
            RenderUtils.drawText(ctx, font, label, bx + 7, y + 9, Theme.TEXT_PRIMARY(), false);
        }
    }

    private void drawToggle(GuiGraphicsExtractor ctx, int x, int y, boolean enabled) {
        RenderUtils.fillRounded(ctx, x, y, 22, 12, 6,
                enabled ? Theme.TOGGLE_ON() : Theme.TOGGLE_OFF());
        RenderUtils.fillRounded(ctx, enabled ? x + 11 : x + 2, y + 2,
                8, 8, 4, enabled ? Theme.TOGGLE_KNOB_ON() : Theme.TOGGLE_KNOB_OFF());
    }

    private void drawButton(GuiGraphicsExtractor ctx, int mouseX, int mouseY,
                            int x, int y, int w, int h, String text) {
        boolean hovered = RenderUtils.hovered(mouseX, mouseY, x, y, w, h);
        RenderUtils.fillRounded(ctx, x, y, w, h, 4,
                hovered ? Theme.BG_SELECTED() : Theme.BG_CARD());
        int tx = x + Math.max(4, (w - RenderUtils.textWidth(font, text)) / 2);
        RenderUtils.drawText(ctx, font, text, tx, y + (h - 8) / 2,
                Theme.TEXT_PRIMARY(), false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        double mx = click.x();
        double my = click.y();
        int x = panelX();
        int y = panelY();
        int w = panelWidth();
        int h = panelHeight();

        if (RenderUtils.hovered((int) mx, (int) my, x + w - 30, y + 8, 18, 18)
                || RenderUtils.hovered((int) mx, (int) my, x + w - 68, y + h - FOOTER_H + 5, 56, 18)) {
            onClose();
            return true;
        }

        int top = listTop();
        int bottom = listBottom();
        if (my < top || my >= bottom) return super.mouseClicked(click, doubled);

        int cursor = top - scroll;
        for (Setting<?> setting : module.getSettings()) {
            int rh = rowHeight(setting);
            if (cursor >= top && cursor + rh <= bottom
                    && RenderUtils.hovered((int) mx, (int) my, x + 12, cursor, w - 28, rh - 1)) {
                if (setting instanceof BooleanSetting bool) {
                    bool.toggle();
                } else if (setting instanceof NumberSetting number) {
                    int barX = x + 16;
                    int barW = Math.max(40, w - 48);
                    if (my >= cursor + 20) {
                        double pct = Math.clamp((mx - barX) / barW, 0.0, 1.0);
                        number.set(number.getMin() + pct * (number.getMax() - number.getMin()));
                    }
                } else if (setting instanceof ModeSetting mode) {
                    mode.cycle();
                } else if (setting instanceof ColorSetting color) {
                    int swatchX = x + 16 + 70;
                    int swatchY = cursor + 7;
                    int swatch = 9;
                    int gap = 2;
                    boolean found = false;
                    for (int i = 0; i < ColorSetting.paletteSize(); i++) {
                        int sx = swatchX + i * (swatch + gap);
                        if (sx + swatch > x + w - 22) break;
                        if (RenderUtils.hovered((int) mx, (int) my, sx, swatchY, swatch, swatch)) {
                            color.set(ColorSetting.paletteColor(i));
                            found = true;
                            break;
                        }
                    }
                    if (!found) color.cyclePalette();
                } else if (setting instanceof BlockListSetting blockList) {
                    if (minecraft != null) minecraft.gui.setScreen(new BlockSelectionScreen(this, blockList));
                }
                return true;
            }
            cursor += rh;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (vertical > 0) scroll = Math.max(0, scroll - 34);
        else if (vertical < 0) scroll = Math.min(maxScroll(), scroll + 34);
        clampScroll();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_UP || input.key() == GLFW.GLFW_KEY_PAGE_UP) {
            scroll = Math.max(0, scroll - listHeight() / 2);
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_DOWN || input.key() == GLFW.GLFW_KEY_PAGE_DOWN) {
            scroll = Math.min(maxScroll(), scroll + listHeight() / 2);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void onClose() {
        if (VoidClient.CONFIG_MANAGER != null) VoidClient.CONFIG_MANAGER.save();
        if (minecraft != null) minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
