package dev.voidclient.gui.components;

import dev.voidclient.core.Module;
import dev.voidclient.gui.Theme;
import dev.voidclient.gui.utils.RenderUtils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Single module card (name + description + toggle).
 */
public class ModuleCard {

    public static final int WIDTH = 160;
    public static final int HEIGHT = 52;

    private final Module module;
    private int x, y;

    public ModuleCard(Module module) {
        this.module = module;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Module getModule() {
        return module;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public void render(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY) {
        boolean hovered = RenderUtils.isHovered(mouseX, mouseY, x, y, WIDTH, HEIGHT);
        int bg = hovered ? Theme.BG_CARD_HOVER : Theme.BG_CARD;

        RenderUtils.fillRounded(ctx, x, y, WIDTH, HEIGHT, 6, bg);
        if (module.isEnabled()) {
            RenderUtils.outline(ctx, x, y, WIDTH, HEIGHT, Theme.ACCENT_DIM);
        } else {
            RenderUtils.outline(ctx, x, y, WIDTH, HEIGHT, Theme.BORDER);
        }

        RenderUtils.drawText(ctx, tr, module.getName(), x + 10, y + 10, Theme.TEXT_PRIMARY, false);

        String desc = module.getDescription();
        if (desc.length() > 22) desc = desc.substring(0, 20) + "..";
        RenderUtils.drawText(ctx, tr, desc, x + 10, y + 24, Theme.TEXT_SECONDARY, false);

        int tx = x + WIDTH - 36;
        int ty = y + 18;
        drawToggle(ctx, tx, ty, module.isEnabled());
    }

    private void drawToggle(DrawContext ctx, int x, int y, boolean on) {
        int w = 26;
        int h = 14;
        int color = on ? Theme.TOGGLE_ON : Theme.TOGGLE_OFF;
        RenderUtils.fillRounded(ctx, x, y, w, h, 7, color);
        int knobX = on ? x + w - 12 : x + 2;
        RenderUtils.fillRounded(ctx, knobX, y + 2, 10, 10, 5, Theme.TOGGLE_KNOB);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!RenderUtils.isHovered((int) mouseX, (int) mouseY, x, y, WIDTH, HEIGHT)) {
            return false;
        }
        if (button == 0) {
            module.toggle();
            return true;
        }
        if (button == 1) {
            return true;
        }
        return false;
    }
}
