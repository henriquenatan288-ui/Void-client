package dev.voidclient.gui;

import dev.voidclient.core.setting.BlockListSetting;
import dev.voidclient.gui.utils.RenderUtils;
import dev.voidclient.modules.render.BlockCatalog;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Block picker for BlockESP with scrollable list. */
public final class BlockSelectionScreen extends Screen {
    private static final String[] GROUPS = {"All", "Ores", "Containers", "Utility"};
    private static final int PANEL_W = 330;
    private static final int PANEL_H = 276;
    private static final int ROW_H = 17;
    private static final int VISIBLE_ROWS = 10;

    private final Screen parent;
    private final BlockListSetting setting;
    private String selectedGroup = "All";
    /** First visible row index (scroll offset). */
    private int scroll;

    public BlockSelectionScreen(Screen parent, BlockListSetting setting) {
        super(Component.literal("Void Client - Select ESP Blocks"));
        this.parent = parent;
        this.setting = setting;
    }

    private List<BlockCatalog.Option> filtered() {
        return BlockCatalog.OPTIONS.stream()
                .filter(option -> selectedGroup.equals("All") || option.group().equals(selectedGroup))
                .toList();
    }

    private int panelX() {
        return (width - PANEL_W) / 2;
    }

    private int panelY() {
        return Math.max(8, (height - PANEL_H) / 2);
    }

    private int maxScroll(int total) {
        return Math.max(0, total - VISIBLE_ROWS);
    }

    private void clampScroll(int total) {
        scroll = Math.max(0, Math.min(scroll, maxScroll(total)));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xC8000000);

        int x = panelX();
        int y = panelY();
        RenderUtils.drawPanel(ctx, x, y, PANEL_W, PANEL_H, 8, Theme.BG_PANEL());
        RenderUtils.drawText(ctx, font, "SELECT ESP BLOCKS", x + 12, y + 10, Theme.TEXT_PRIMARY(), false);
        RenderUtils.drawText(ctx, font, setting.getSelectedIds().size() + " selected",
                x + PANEL_W - 88, y + 10, Theme.TEXT_MUTED(), false);

        int tabY = y + 31;
        for (int i = 0; i < GROUPS.length; i++) {
            int tx = x + 10 + i * 78;
            boolean active = GROUPS[i].equals(selectedGroup);
            boolean hovered = RenderUtils.hovered(mouseX, mouseY, tx, tabY, 72, 19);
            int bg = active ? Theme.BG_SELECTED() : (hovered ? Theme.BG_CARD_HOVER() : Theme.BG_CARD());
            RenderUtils.fillRounded(ctx, tx, tabY, 72, 19, 4, bg);
            RenderUtils.drawText(ctx, font, GROUPS[i], tx + 7, tabY + 5,
                    active ? Theme.TEXT_PRIMARY() : Theme.TEXT_SECONDARY(), false);
        }

        List<BlockCatalog.Option> options = filtered();
        clampScroll(options.size());
        int listY = y + 56;
        int listH = VISIBLE_ROWS * ROW_H;
        int listW = PANEL_W - 28;

        // List background
        RenderUtils.fillRounded(ctx, x + 10, listY - 2, listW, listH + 2, 4, Theme.BG_CARD());

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int index = scroll + i;
            if (index >= options.size()) break;
            BlockCatalog.Option option = options.get(index);
            int ry = listY + i * ROW_H;
            boolean active = setting.isSelected(option.id());
            boolean hovered = RenderUtils.hovered(mouseX, mouseY, x + 10, ry, listW - 12, ROW_H - 1);
            if (hovered) {
                RenderUtils.fillRounded(ctx, x + 12, ry, listW - 16, ROW_H - 1, 3, Theme.BG_CARD_HOVER());
            }
            RenderUtils.outline(ctx, x + 15, ry + 3, 10, 10, active ? Theme.ACCENT() : Theme.BORDER());
            if (active) RenderUtils.fillRounded(ctx, x + 17, ry + 5, 6, 6, 2, Theme.ACCENT());
            RenderUtils.drawText(ctx, font, option.label(), x + 32, ry + 4,
                    active ? Theme.TEXT_PRIMARY() : Theme.TEXT_SECONDARY(), false);
            RenderUtils.drawText(ctx, font, option.group(), x + PANEL_W - 85, ry + 4,
                    Theme.TEXT_MUTED(), false);
        }

        // Scrollbar
        int max = maxScroll(options.size());
        if (max > 0) {
            int trackX = x + PANEL_W - 16;
            int trackY = listY;
            int trackH = listH;
            RenderUtils.fillRounded(ctx, trackX, trackY, 4, trackH, 2, Theme.BORDER());
            float ratio = VISIBLE_ROWS / (float) options.size();
            int thumbH = Math.max(12, (int) (trackH * ratio));
            int thumbY = trackY + (int) ((trackH - thumbH) * (scroll / (float) max));
            RenderUtils.fillRounded(ctx, trackX, thumbY, 4, thumbH, 2, Theme.ACCENT());
        }

        int footerY = y + PANEL_H - 25;
        RenderUtils.drawText(ctx, font, (scroll + 1) + "-" + Math.min(scroll + VISIBLE_ROWS, options.size())
                        + " / " + options.size(),
                x + 12, footerY + 5, Theme.TEXT_MUTED(), false);
        drawButton(ctx, mouseX, mouseY, x + 120, footerY, 48, "CLEAR");
        drawButton(ctx, mouseX, mouseY, x + 172, footerY, 42, "ALL");
        drawButton(ctx, mouseX, mouseY, x + 218, footerY, 42, "UP");
        drawButton(ctx, mouseX, mouseY, x + 264, footerY, 48, "DOWN");
        drawButton(ctx, mouseX, mouseY, x + PANEL_W - 48, footerY, 39, "DONE");

        super.extractRenderState(ctx, mouseX, mouseY, delta);
    }

    private void drawButton(GuiGraphicsExtractor ctx, int mouseX, int mouseY, int x, int y, int w, String label) {
        boolean hovered = RenderUtils.hovered(mouseX, mouseY, x, y, w, 17);
        RenderUtils.fillRounded(ctx, x, y, w, 17, 4, hovered ? Theme.BG_SELECTED() : Theme.BG_CARD());
        RenderUtils.drawText(ctx, font, label, x + 5, y + 4, Theme.TEXT_PRIMARY(), false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);

        double mx = click.x();
        double my = click.y();
        int x = panelX();
        int y = panelY();
        int tabY = y + 31;

        for (int i = 0; i < GROUPS.length; i++) {
            int tx = x + 10 + i * 78;
            if (RenderUtils.hovered((int) mx, (int) my, tx, tabY, 72, 19)) {
                selectedGroup = GROUPS[i];
                scroll = 0;
                return true;
            }
        }

        List<BlockCatalog.Option> options = filtered();
        clampScroll(options.size());
        int listY = y + 56;
        int listW = PANEL_W - 28;
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int index = scroll + i;
            if (index >= options.size()) break;
            int ry = listY + i * ROW_H;
            if (RenderUtils.hovered((int) mx, (int) my, x + 10, ry, listW - 12, ROW_H - 1)) {
                setting.toggle(options.get(index).id());
                return true;
            }
        }

        int footerY = y + PANEL_H - 25;
        if (RenderUtils.hovered((int) mx, (int) my, x + 120, footerY, 48, 17)) {
            setting.setSelectedIds(List.of());
            return true;
        }
        if (RenderUtils.hovered((int) mx, (int) my, x + 172, footerY, 42, 17)) {
            setting.setSelectedIds(BlockCatalog.OPTIONS.stream().map(BlockCatalog.Option::id).toList());
            return true;
        }
        if (RenderUtils.hovered((int) mx, (int) my, x + 218, footerY, 42, 17)) {
            scroll = Math.max(0, scroll - 3);
            return true;
        }
        if (RenderUtils.hovered((int) mx, (int) my, x + 264, footerY, 48, 17)) {
            scroll = Math.min(maxScroll(options.size()), scroll + 3);
            return true;
        }
        if (RenderUtils.hovered((int) mx, (int) my, x + PANEL_W - 48, footerY, 39, 17)) {
            onClose();
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        List<BlockCatalog.Option> options = filtered();
        if (vertical > 0) scroll = Math.max(0, scroll - 2);
        else if (vertical < 0) scroll = Math.min(maxScroll(options.size()), scroll + 2);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        List<BlockCatalog.Option> options = filtered();
        if (input.key() == GLFW.GLFW_KEY_UP || input.key() == GLFW.GLFW_KEY_PAGE_UP) {
            scroll = Math.max(0, scroll - 3);
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_DOWN || input.key() == GLFW.GLFW_KEY_PAGE_DOWN) {
            scroll = Math.min(maxScroll(options.size()), scroll + 3);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
