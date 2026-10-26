package dev.voidclient.gui.components;

import dev.voidclient.core.Module;
import dev.voidclient.gui.Theme;
import dev.voidclient.gui.utils.RenderUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class ModuleCard {

    public static final int WIDTH = 168;
    public static final int HEIGHT = 36;

    private final Module module;
    private int x, y;
    private float hoverAnim;

    public ModuleCard(Module module) {
        this.module = module;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Module getModule() { return module; }
    public int getX() { return x; }
    public int getY() { return y; }

    public void update(int mouseX, int mouseY, float delta) {
        boolean h = RenderUtils.hovered(mouseX, mouseY, x, y, WIDTH, HEIGHT);
        float speed = 10f * delta;
        if (h) hoverAnim = Math.min(1f, hoverAnim + speed);
        else hoverAnim = Math.max(0f, hoverAnim - speed);
    }

    public void render(GuiGraphics ctx, Font tr, int mouseX, int mouseY) {
        int bg = RenderUtils.lerpColor(Theme.BG_CARD, Theme.BG_CARD_HOVER, hoverAnim);
        RenderUtils.drawCard(ctx, x, y, WIDTH, HEIGHT, 5, bg);

        if (module.isEnabled()) {
            RenderUtils.outline(ctx, x, y, WIDTH, HEIGHT, Theme.ACCENT_DIM);
            RenderUtils.fill(ctx, x, y + 4, 2, HEIGHT - 8, Theme.ACCENT);
        }

        RenderUtils.drawText(ctx, tr, module.getName(), x + 10, y + 7, Theme.TEXT_PRIMARY, false);

        String desc = module.getDescription();
        if (desc.length() > 24) desc = desc.substring(0, 22) + "..";
        RenderUtils.drawText(ctx, tr, desc, x + 10, y + 20, Theme.TEXT_MUTED, false);

        drawToggle(ctx, x + WIDTH - 34, y + 11, module.isEnabled());
    }

    private void drawToggle(GuiGraphics ctx, int tx, int ty, boolean on) {
        RenderUtils.fillRounded(ctx, tx, ty, 22, 12, 6, on ? Theme.TOGGLE_ON : Theme.TOGGLE_OFF);
        int kx = on ? tx + 11 : tx + 2;
        RenderUtils.fillRounded(ctx, kx, ty + 2, 8, 8, 4, on ? Theme.TOGGLE_KNOB_ON : Theme.TOGGLE_KNOB_OFF);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!RenderUtils.hovered((int) mouseX, (int) mouseY, x, y, WIDTH, HEIGHT)) return false;
        if (button == 0) {
            module.toggle();
            return true;
        }
        return button == 1;
    }
}
