package dev.voidclient.gui.components;

import dev.voidclient.core.Module;
import dev.voidclient.core.setting.*;
import dev.voidclient.gui.Theme;
import dev.voidclient.gui.utils.RenderUtils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/**
 * Floating settings panel for a module.
 */
public class SettingsPanel {

    public static final int WIDTH = 200;
    private int x, y;
    private final Module module;
    private boolean visible = true;

    public SettingsPanel(Module module, int x, int y) {
        this.module = module;
        this.x = x;
        this.y = y;
    }

    public Module getModule() {
        return module;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getHeight(TextRenderer tr) {
        List<Setting<?>> settings = module.getSettings();
        int h = 28;
        for (Setting<?> s : settings) {
            if (s instanceof NumberSetting) h += 28;
            else if (s instanceof BooleanSetting) h += 20;
            else if (s instanceof ModeSetting) h += 22;
            else h += 20;
        }
        if (settings.isEmpty()) h += 18;
        return h + 8;
    }

    public void render(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY) {
        if (!visible) return;

        int h = getHeight(tr);
        RenderUtils.fillRounded(ctx, x, y, WIDTH, h, 6, Theme.BG_PANEL);
        RenderUtils.outline(ctx, x, y, WIDTH, h, Theme.BORDER_ACCENT);

        RenderUtils.drawText(ctx, tr, module.getName(), x + 10, y + 8, Theme.TEXT_PRIMARY, false);
        RenderUtils.drawText(ctx, tr, "x", x + WIDTH - 14, y + 8, Theme.TEXT_SECONDARY, false);

        int cy = y + 28;
        List<Setting<?>> settings = module.getSettings();

        if (settings.isEmpty()) {
            RenderUtils.drawText(ctx, tr, "No settings", x + 10, cy, Theme.TEXT_MUTED, false);
            return;
        }

        for (Setting<?> setting : settings) {
            RenderUtils.drawText(ctx, tr, setting.getName(), x + 10, cy, Theme.TEXT_SECONDARY, false);

            if (setting instanceof BooleanSetting bs) {
                int tx = x + WIDTH - 36;
                drawToggle(ctx, tx, cy - 1, bs.get());
                cy += 20;
            } else if (setting instanceof NumberSetting ns) {
                String val = String.format("%.1f", ns.get());
                RenderUtils.drawText(ctx, tr, val, x + WIDTH - 40, cy, Theme.TEXT_PRIMARY, false);
                int barX = x + 10;
                int barY = cy + 12;
                int barW = WIDTH - 20;
                RenderUtils.fill(ctx, barX, barY, barW, 3, Theme.BG_INPUT);
                double pct = (ns.get() - ns.getMin()) / (ns.getMax() - ns.getMin());
                RenderUtils.fill(ctx, barX, barY, (int) (barW * pct), 3, Theme.ACCENT);
                cy += 28;
            } else if (setting instanceof ModeSetting ms) {
                RenderUtils.drawText(ctx, tr, ms.get(), x + WIDTH - tr.getWidth(ms.get()) - 12, cy, Theme.ACCENT, false);
                cy += 22;
            } else if (setting instanceof ColorSetting cs) {
                RenderUtils.fill(ctx, x + WIDTH - 24, cy, 12, 12, cs.get());
                cy += 20;
            } else {
                cy += 20;
            }
        }
    }

    private void drawToggle(DrawContext ctx, int x, int y, boolean on) {
        int w = 26;
        int h = 14;
        int color = on ? Theme.TOGGLE_ON : Theme.TOGGLE_OFF;
        RenderUtils.fillRounded(ctx, x, y, w, h, 7, color);
        int knobX = on ? x + w - 12 : x + 2;
        RenderUtils.fillRounded(ctx, knobX, y + 2, 10, 10, 5, Theme.TOGGLE_KNOB);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, TextRenderer tr) {
        if (!visible) return false;
        int h = getHeight(tr);

        if (RenderUtils.isHovered((int) mouseX, (int) mouseY, x + WIDTH - 18, y + 4, 14, 14)) {
            visible = false;
            return true;
        }

        if (!RenderUtils.isHovered((int) mouseX, (int) mouseY, x, y, WIDTH, h)) {
            return false;
        }

        int cy = y + 28;
        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof BooleanSetting bs) {
                int tx = x + WIDTH - 36;
                if (RenderUtils.isHovered((int) mouseX, (int) mouseY, tx, cy - 1, 26, 14)) {
                    bs.toggle();
                    return true;
                }
                cy += 20;
            } else if (setting instanceof NumberSetting ns) {
                int barX = x + 10;
                int barY = cy + 12;
                int barW = WIDTH - 20;
                if (RenderUtils.isHovered((int) mouseX, (int) mouseY, barX, barY - 4, barW, 12)) {
                    double pct = (mouseX - barX) / barW;
                    pct = Math.max(0, Math.min(1, pct));
                    double value = ns.getMin() + pct * (ns.getMax() - ns.getMin());
                    ns.set(value);
                    return true;
                }
                cy += 28;
            } else if (setting instanceof ModeSetting ms) {
                if (RenderUtils.isHovered((int) mouseX, (int) mouseY, x, cy, WIDTH, 18)) {
                    ms.cycle();
                    return true;
                }
                cy += 22;
            } else {
                cy += 20;
            }
        }
        return true;
    }

    public boolean isVisible() {
        return visible;
    }

    public void close() {
        visible = false;
    }
}
