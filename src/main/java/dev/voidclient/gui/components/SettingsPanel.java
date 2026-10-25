package dev.voidclient.gui.components;

import dev.voidclient.core.Module;
import dev.voidclient.core.setting.*;
import dev.voidclient.gui.Theme;
import dev.voidclient.gui.utils.RenderUtils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

public class SettingsPanel {

    public static final int WIDTH = 190;
    private int x, y;
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

    public int getHeight() {
        List<Setting<?>> settings = module.getSettings();
        int h = 26;
        for (Setting<?> s : settings) {
            if (s instanceof NumberSetting) h += 26;
            else h += 18;
        }
        if (settings.isEmpty()) h += 16;
        return h + 6;
    }

    public void render(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY) {
        if (!visible) return;
        int h = getHeight();
        int drawY = y + (int) ((1f - openAnim) * 8);

        RenderUtils.drawPanel(ctx, x, drawY, WIDTH, h, 6, Theme.BG_PANEL);

        RenderUtils.drawText(ctx, tr, module.getName(), x + 8, drawY + 8, Theme.TEXT_PRIMARY, false);
        RenderUtils.drawText(ctx, tr, "×", x + WIDTH - 14, drawY + 7, Theme.TEXT_SECONDARY, false);

        int cy = drawY + 26;
        List<Setting<?>> settings = module.getSettings();

        if (settings.isEmpty()) {
            RenderUtils.drawText(ctx, tr, "No settings", x + 8, cy, Theme.TEXT_MUTED, false);
            return;
        }

        for (Setting<?> setting : settings) {
            RenderUtils.drawText(ctx, tr, setting.getName(), x + 8, cy, Theme.TEXT_SECONDARY, false);

            if (setting instanceof BooleanSetting bs) {
                drawToggle(ctx, x + WIDTH - 30, cy - 1, bs.get());
                cy += 18;
            } else if (setting instanceof NumberSetting ns) {
                String val = ns.getStep() >= 1 ? String.valueOf(ns.getInt()) : String.format("%.1f", ns.get());
                RenderUtils.drawText(ctx, tr, val, x + WIDTH - RenderUtils.textWidth(tr, val) - 8, cy, Theme.TEXT_PRIMARY, false);
                int barX = x + 8, barY = cy + 12, barW = WIDTH - 16;
                RenderUtils.fillRounded(ctx, barX, barY, barW, 3, 1, Theme.SLIDER_TRACK);
                double pct = (ns.get() - ns.getMin()) / Math.max(0.0001, ns.getMax() - ns.getMin());
                RenderUtils.fillRounded(ctx, barX, barY, Math.max(2, (int) (barW * pct)), 3, 1, Theme.SLIDER_FILL);
                cy += 26;
            } else if (setting instanceof ModeSetting ms) {
                String mode = ms.get();
                RenderUtils.drawText(ctx, tr, mode, x + WIDTH - RenderUtils.textWidth(tr, mode) - 8, cy, Theme.TEXT_PRIMARY, false);
                cy += 18;
            } else if (setting instanceof ColorSetting cs) {
                RenderUtils.fill(ctx, x + WIDTH - 20, cy + 1, 10, 10, cs.get());
                RenderUtils.outline(ctx, x + WIDTH - 20, cy + 1, 10, 10, Theme.BORDER);
                cy += 18;
            } else {
                cy += 18;
            }
        }
    }

    private void drawToggle(DrawContext ctx, int tx, int ty, boolean on) {
        RenderUtils.fillRounded(ctx, tx, ty, 20, 11, 5, on ? Theme.TOGGLE_ON : Theme.TOGGLE_OFF);
        int kx = on ? tx + 10 : tx + 2;
        RenderUtils.fillRounded(ctx, kx, ty + 2, 7, 7, 3, on ? Theme.TOGGLE_KNOB_ON : Theme.TOGGLE_KNOB_OFF);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, TextRenderer tr) {
        if (!visible) return false;
        int h = getHeight();
        int drawY = y + (int) ((1f - openAnim) * 8);

        if (RenderUtils.hovered((int) mouseX, (int) mouseY, x + WIDTH - 18, drawY + 4, 14, 14)) {
            visible = false;
            return true;
        }
        if (!RenderUtils.hovered((int) mouseX, (int) mouseY, x, drawY, WIDTH, h)) return false;

        int cy = drawY + 26;
        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof BooleanSetting bs) {
                if (RenderUtils.hovered((int) mouseX, (int) mouseY, x + WIDTH - 30, cy - 1, 20, 11)) {
                    bs.toggle();
                    return true;
                }
                cy += 18;
            } else if (setting instanceof NumberSetting ns) {
                int barX = x + 8, barY = cy + 12, barW = WIDTH - 16;
                if (RenderUtils.hovered((int) mouseX, (int) mouseY, barX, barY - 4, barW, 11)) {
                    double pct = Math.max(0, Math.min(1, (mouseX - barX) / (double) barW));
                    ns.set(ns.getMin() + pct * (ns.getMax() - ns.getMin()));
                    return true;
                }
                cy += 26;
            } else if (setting instanceof ModeSetting ms) {
                if (RenderUtils.hovered((int) mouseX, (int) mouseY, x, cy, WIDTH, 16)) {
                    ms.cycle();
                    return true;
                }
                cy += 18;
            } else {
                cy += 18;
            }
        }
        return true;
    }

    public boolean isVisible() { return visible; }
}
