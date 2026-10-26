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

/** Block picker for BlockESP — scroll via wheel, drag list, drag scrollbar, UP/DOWN. */
public final class BlockSelectionScreen extends Screen {
    private static final String[] GROUPS = {"All", "Ores", "Containers", "Utility"};
    private static final int PANEL_W = 330;
    private static final int PANEL_H = 276;
    private static final int ROW_H = 17;
    private static final int VISIBLE_ROWS = 10;
    private static final int BAR_W = 14;

    private final Screen parent;
    private final BlockListSetting setting;
    private String selectedGroup = "All";
    private int scroll;

    private boolean draggingBar;
    private boolean draggingList;
    private boolean moved;
    private double pressX, pressY;
    private int scrollAtPress;

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

    private int panelX() { return (width - PANEL_W) / 2; }
    private int panelY() { return Math.max(8, (height - PANEL_H) / 2); }
    private int maxScroll(int total) { return Math.max(0, total - VISIBLE_ROWS); }
    private void clampScroll(int total) { scroll = Math.max(0, Math.min(scroll, maxScroll(total))); }

    private int listY() { return panelY() + 56; }
    private int listH() { return VISIBLE_ROWS * ROW_H; }
    private int listW() { return PANEL_W - 28 - BAR_W; }
    private int barX() { return panelX() + PANEL_W - 18; }

    private int thumbH(int total) {
        int max = maxScroll(total);
        if (max <= 0) return listH();
        float ratio = VISIBLE_ROWS / (float) Math.max(1, total);
        return Math.max(18, (int) (listH() * ratio));
    }

    private int thumbY(int total) {
        int max = maxScroll(total);
        int th = thumbH(total);
        if (max <= 0) return listY();
        return listY() + (int) ((listH() - th) * (scroll / (float) max));
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
        int ly = listY();
        int lw = listW();

        RenderUtils.fillRounded(ctx, x + 10, ly - 2, lw + BAR_W + 4, listH() + 2, 4, Theme.BG_CARD());

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int index = scroll + i;
            if (index >= options.size()) break;
            BlockCatalog.Option option = options.get(index);
            int ry = ly + i * ROW_H;
            boolean active = setting.isSelected(option.id());
            boolean hovered = RenderUtils.hovered(mouseX, mouseY, x + 10, ry, lw, ROW_H - 1);
            if (hovered) {
                RenderUtils.fillRounded(ctx, x + 12, ry, lw - 4, ROW_H - 1, 3, Theme.BG_CARD_HOVER());
            }
            RenderUtils.outline(ctx, x + 15, ry + 3, 10, 10, active ? Theme.ACCENT() : Theme.BORDER());
            if (active) RenderUtils.fillRounded(ctx, x + 17, ry + 5, 6, 6, 2, Theme.ACCENT());
            RenderUtils.drawText(ctx, font, option.label(), x + 32, ry + 4,
                    active ? Theme.TEXT_PRIMARY() : Theme.TEXT_SECONDARY(), false);
            RenderUtils.drawText(ctx, font, option.group(), x + PANEL_W - 100, ry + 4,
                    Theme.TEXT_MUTED(), false);
        }

        // Wide scrollbar for finger
        int max = maxScroll(options.size());
        int bx = barX();
        RenderUtils.fillRounded(ctx, bx, ly, BAR_W, listH(), 3, 0x66000000);
        if (max > 0) {
            int th = thumbH(options.size());
            int ty = thumbY(options.size());
            RenderUtils.fillRounded(ctx, bx + 1, ty, BAR_W - 2, th, 3,
                    draggingBar ? 0xFFFFFFFF : Theme.ACCENT());
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
        pressX = mx;
        pressY = my;
        moved = false;
        scrollAtPress = scroll;
        draggingBar = false;
        draggingList = false;

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
        int ly = listY();
        int bx = barX();

        // Scrollbar hit (wide)
        if (mx >= bx && mx <= bx + BAR_W && my >= ly && my <= ly + listH()) {
            draggingBar = true;
            int th = thumbH(options.size());
            int ty = thumbY(options.size());
            if (my < ty || my > ty + th) {
                scrollFromBarY(my - th / 2.0, options.size());
            }
            return true;
        }

        // List area — start potential drag
        if (mx >= x + 10 && mx <= x + 10 + listW() && my >= ly && my <= ly + listH()) {
            draggingList = true;
            return true;
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
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        List<BlockCatalog.Option> options = filtered();
        if (draggingBar) {
            scrollFromBarY(click.y() - thumbH(options.size()) / 2.0, options.size());
            return true;
        }
        if (draggingList) {
            if (Math.abs(click.x() - pressX) > 4 || Math.abs(click.y() - pressY) > 4) moved = true;
            if (moved) {
                // drag up → scroll down
                int deltaRows = (int) Math.round((pressY - click.y()) / (double) ROW_H);
                scroll = Math.max(0, Math.min(maxScroll(options.size()), scrollAtPress + deltaRows));
            }
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        boolean wasList = draggingList;
        boolean wasMoved = moved;
        draggingBar = false;
        draggingList = false;

        // Tap without drag = toggle block
        if (wasList && !wasMoved && click.button() == 0) {
            List<BlockCatalog.Option> options = filtered();
            int x = panelX();
            int ly = listY();
            int lw = listW();
            double mx = click.x();
            double my = click.y();
            for (int i = 0; i < VISIBLE_ROWS; i++) {
                int index = scroll + i;
                if (index >= options.size()) break;
                int ry = ly + i * ROW_H;
                if (RenderUtils.hovered((int) mx, (int) my, x + 10, ry, lw, ROW_H - 1)) {
                    setting.toggle(options.get(index).id());
                    moved = false;
                    return true;
                }
            }
        }
        moved = false;
        return super.mouseReleased(click);
    }

    private void scrollFromBarY(double thumbTop, int total) {
        int max = maxScroll(total);
        if (max <= 0) {
            scroll = 0;
            return;
        }
        int th = thumbH(total);
        double track = listH() - th;
        double t = track <= 0 ? 0 : (thumbTop - listY()) / track;
        t = Math.max(0, Math.min(1, t));
        scroll = (int) Math.round(t * max);
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
