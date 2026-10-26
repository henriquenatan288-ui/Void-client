package dev.voidclient.gui.components;

import dev.voidclient.core.Module;
import dev.voidclient.core.setting.*;
import dev.voidclient.gui.BlockSelectionScreen;
import dev.voidclient.gui.Theme;
import dev.voidclient.gui.utils.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

public class SettingsPanel {
    public static final int WIDTH = 190;
    private final int x, y;
    private final Module module;
    private boolean visible = true;
    private float openAnim = 0f;

    public SettingsPanel(Module module, int x, int y) {
        this.module = module;
        this.x = x;
        this.y = y;
    }

    public Module getModule() { return module; }

    public void update(float delta) {
        openAnim = Math.min(1f, openAnim + delta * 8f);
    }

    private static int rowHeight(Setting<?> setting) {
        if (setting instanceof NumberSetting) return 26;
        if (setting instanceof ColorSetting) return 29;
        if (setting instanceof BlockListSetting) return 26;
        return 18;
    }

    public int getHeight() {
        List<Setting<?>> settings = module.getSettings();
        int h = 26;
        for (Setting<?> setting : settings) h += rowHeight(setting);
        if (settings.isEmpty()) h += 16;
        return h + 6;
    }

    public void render(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY) {
        if (!visible) return;
        int h = getHeight();
        int drawY = y + (int) ((1f - openAnim) * 8);

        RenderUtils.drawPanel(ctx, x, drawY, WIDTH, h, 6, Theme.BG_PANEL);
        RenderUtils.drawText(ctx, tr, module.getName(), x + 8, drawY + 8, Theme.TEXT_PRIMARY, false);
        RenderUtils.drawText(ctx, tr, "x", x + WIDTH - 14, drawY + 7, Theme.TEXT_SECONDARY, false);

        int cy = drawY + 26;
        List<Setting<?>> settings = module.getSettings();
        if (settings.isEmpty()) {
            RenderUtils.drawText(ctx, tr, "No settings", x + 8, cy, Theme.TEXT_MUTED, false);
            return;
        }

        for (Setting<?> setting : settings) {
            RenderUtils.drawText(ctx, tr, setting.getName(), x + 8, cy, Theme.TEXT_SECONDARY, false);

            if (setting instanceof BooleanSetting booleanSetting) {
                drawToggle(ctx, x + WIDTH - 30, cy - 1, booleanSetting.get());
                cy += rowHeight(setting);
            } else if (setting instanceof NumberSetting numberSetting) {
                String value = numberSetting.getStep() >= 1
                        ? String.valueOf(numberSetting.getInt())
                        : String.format(java.util.Locale.ROOT, "%.1f", numberSetting.get());
                RenderUtils.drawText(ctx, tr, value,
                        x + WIDTH - RenderUtils.textWidth(tr, value) - 8, cy,
                        Theme.TEXT_PRIMARY, false);
                int barX = x + 8, barY = cy + 12, barW = WIDTH - 16;
                RenderUtils.fillRounded(ctx, barX, barY, barW, 3, 1, Theme.SLIDER_TRACK);
                double pct = (numberSetting.get() - numberSetting.getMin())
                        / Math.max(0.0001, numberSetting.getMax() - numberSetting.getMin());
                RenderUtils.fillRounded(ctx, barX, barY,
                        Math.max(2, (int) (barW * pct)), 3, 1, Theme.SLIDER_FILL);
                cy += rowHeight(setting);
            } else if (setting instanceof ModeSetting modeSetting) {
                String mode = modeSetting.get();
                RenderUtils.drawText(ctx, tr, mode,
                        x + WIDTH - RenderUtils.textWidth(tr, mode) - 8, cy,
                        Theme.TEXT_PRIMARY, false);
                cy += rowHeight(setting);
            } else if (setting instanceof ColorSetting colorSetting) {
                RenderUtils.drawText(ctx, tr, "Palette", x + 8, cy + 10, Theme.TEXT_MUTED, false);
                int swatchX = x + 70;
                int swatchY = cy + 8;
                for (int i = 0; i < ColorSetting.paletteSize(); i++) {
                    int sx = swatchX + i * 12;
                    boolean selected = ColorSetting.paletteColor(i) == colorSetting.get();
                    RenderUtils.fillRounded(ctx, sx, swatchY, 10, 10, 3, ColorSetting.paletteColor(i));
                    RenderUtils.outline(ctx, sx, swatchY, 10, 10,
                            selected ? Theme.TEXT_PRIMARY : Theme.BORDER);
                }
                cy += rowHeight(setting);
            } else if (setting instanceof BlockListSetting blockListSetting) {
                String label = blockListSetting.getSelectedIds().size() + " selected";
                int buttonX = x + WIDTH - 78;
                RenderUtils.fillRounded(ctx, buttonX, cy - 2, 70, 16, 4,
                        RenderUtils.hovered(mouseX, mouseY, buttonX, cy - 2, 70, 16)
                                ? Theme.BG_SELECTED : Theme.BG_CARD);
                RenderUtils.drawText(ctx, tr, label, buttonX + 5, cy + 2,
                        Theme.TEXT_PRIMARY, false);
                cy += rowHeight(setting);
            } else {
                cy += rowHeight(setting);
            }
        }
    }

    private void drawToggle(DrawContext ctx, int tx, int ty, boolean on) {
        RenderUtils.fillRounded(ctx, tx, ty, 20, 11, 5, on ? Theme.TOGGLE_ON : Theme.TOGGLE_OFF);
        int kx = on ? tx + 10 : tx + 2;
        RenderUtils.fillRounded(ctx, kx, ty + 2, 7, 7, 3,
                on ? Theme.TOGGLE_KNOB_ON : Theme.TOGGLE_KNOB_OFF);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, TextRenderer tr) {
        if (!visible) return false;
        int h = getHeight();
        int drawY = y + (int) ((1f - openAnim) * 8);

        if (RenderUtils.hovered((int) mouseX, (int) mouseY,
                x + WIDTH - 18, drawY + 4, 14, 14)) {
            visible = false;
            return true;
        }
        if (!RenderUtils.hovered((int) mouseX, (int) mouseY, x, drawY, WIDTH, h)) return false;

        int cy = drawY + 26;
        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof BooleanSetting booleanSetting) {
                if (RenderUtils.hovered((int) mouseX, (int) mouseY,
                        x + WIDTH - 30, cy - 1, 20, 11)) {
                    booleanSetting.toggle();
                    
                    return true;
                }
                cy += rowHeight(setting);
            } else if (setting instanceof NumberSetting numberSetting) {
                int barX = x + 8, barY = cy + 12, barW = WIDTH - 16;
                if (RenderUtils.hovered((int) mouseX, (int) mouseY, barX, barY - 4, barW, 11)) {
                    double pct = Math.max(0, Math.min(1, (mouseX - barX) / (double) barW));
                    numberSetting.set(numberSetting.getMin()
                            + pct * (numberSetting.getMax() - numberSetting.getMin()));
                    
                    return true;
                }
                cy += rowHeight(setting);
            } else if (setting instanceof ModeSetting modeSetting) {
                if (RenderUtils.hovered((int) mouseX, (int) mouseY, x, cy, WIDTH, 16)) {
                    modeSetting.cycle();
                    
                    return true;
                }
                cy += rowHeight(setting);
            } else if (setting instanceof ColorSetting colorSetting) {
                int swatchX = x + 70;
                int swatchY = cy + 8;
                for (int i = 0; i < ColorSetting.paletteSize(); i++) {
                    int sx = swatchX + i * 12;
                    if (RenderUtils.hovered((int) mouseX, (int) mouseY, sx, swatchY, 10, 10)) {
                        colorSetting.set(ColorSetting.paletteColor(i));
                        
                        return true;
                    }
                }
                if (RenderUtils.hovered((int) mouseX, (int) mouseY, x, cy, WIDTH, rowHeight(setting))) {
                    colorSetting.cyclePalette();
                    
                    return true;
                }
                cy += rowHeight(setting);
            } else if (setting instanceof BlockListSetting blockListSetting) {
                if (RenderUtils.hovered((int) mouseX, (int) mouseY, x, cy - 2, WIDTH, 22)) {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc != null) mc.setScreen(new BlockSelectionScreen(mc.currentScreen, blockListSetting));
                    return true;
                }
                cy += rowHeight(setting);
            } else {
                cy += rowHeight(setting);
            }
        }
        return true;
    }

    public boolean isVisible() { return visible; }
}
