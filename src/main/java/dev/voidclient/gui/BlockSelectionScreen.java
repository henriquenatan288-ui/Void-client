package dev.voidclient.gui;

import dev.voidclient.VoidClient;
import dev.voidclient.core.setting.BlockListSetting;
import dev.voidclient.gui.utils.RenderUtils;
import dev.voidclient.modules.render.BlockCatalog;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class BlockSelectionScreen extends Screen {
    private static final String[] GROUPS = {"All", "Ores", "Containers", "Utility"};
    private static final int PANEL_W = 330;
    private static final int PANEL_H = 276;
    private static final int ROW_H = 17;
    private static final int PAGE_SIZE = 10;

    private final Screen parent;
    private final BlockListSetting setting;
    private String selectedGroup = "All";
    private int page;

    public BlockSelectionScreen(Screen parent, BlockListSetting setting) {
        super(Text.literal("Void Client - Select ESP Blocks"));
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

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xC8000000);

        int x = panelX();
        int y = panelY();
        RenderUtils.drawPanel(ctx, x, y, PANEL_W, PANEL_H, 8, Theme.BG_PANEL);
        RenderUtils.drawText(ctx, textRenderer, "SELECT ESP BLOCKS", x + 12, y + 10, Theme.TEXT_PRIMARY, false);
        RenderUtils.drawText(ctx, textRenderer, setting.getSelectedIds().size() + " selected",
                x + PANEL_W - 88, y + 10, Theme.TEXT_MUTED, false);

        int tabY = y + 31;
        for (int i = 0; i < GROUPS.length; i++) {
            int tx = x + 10 + i * 78;
            boolean active = GROUPS[i].equals(selectedGroup);
            boolean hovered = RenderUtils.hovered(mouseX, mouseY, tx, tabY, 72, 19);
            int bg = active ? Theme.BG_SELECTED : (hovered ? Theme.BG_CARD_HOVER : Theme.BG_CARD);
            RenderUtils.fillRounded(ctx, tx, tabY, 72, 19, 4, bg);
            RenderUtils.drawText(ctx, textRenderer, GROUPS[i], tx + 7, tabY + 5,
                    active ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY, false);
        }

        List<BlockCatalog.Option> options = filtered();
        int pages = Math.max(1, (options.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page, pages - 1));
        int first = page * PAGE_SIZE;
        int listY = y + 56;

        for (int i = first; i < Math.min(options.size(), first + PAGE_SIZE); i++) {
            BlockCatalog.Option option = options.get(i);
            int row = i - first;
            int ry = listY + row * ROW_H;
            boolean active = setting.isSelected(option.id());
            boolean hovered = RenderUtils.hovered(mouseX, mouseY, x + 10, ry, PANEL_W - 20, ROW_H - 1);
            if (hovered) {
                RenderUtils.fillRounded(ctx, x + 10, ry, PANEL_W - 20, ROW_H - 1, 3, Theme.BG_CARD_HOVER);
            }
            RenderUtils.outline(ctx, x + 15, ry + 3, 10, 10, active ? Theme.ACCENT : Theme.BORDER);
            if (active) RenderUtils.fillRounded(ctx, x + 17, ry + 5, 6, 6, 2, Theme.ACCENT);
            RenderUtils.drawText(ctx, textRenderer, option.label(), x + 32, ry + 4,
                    active ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY, false);
            RenderUtils.drawText(ctx, textRenderer, option.group(), x + PANEL_W - 69, ry + 4,
                    Theme.TEXT_MUTED, false);
        }

        int footerY = y + PANEL_H - 25;
        RenderUtils.drawText(ctx, textRenderer, "Page " + (page + 1) + "/" + pages,
                x + 12, footerY + 5, Theme.TEXT_MUTED, false);
        drawButton(ctx, mouseX, mouseY, x + 91, footerY, 48, "CLEAR");
        drawButton(ctx, mouseX, mouseY, x + 143, footerY, 42, "ALL");
        drawButton(ctx, mouseX, mouseY, x + 189, footerY, 42, "<");
        drawButton(ctx, mouseX, mouseY, x + 235, footerY, 42, ">");
        drawButton(ctx, mouseX, mouseY, x + 281, footerY, 39, "DONE");

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawButton(DrawContext ctx, int mouseX, int mouseY, int x, int y, int w, String label) {
        boolean hovered = RenderUtils.hovered(mouseX, mouseY, x, y, w, 17);
        RenderUtils.fillRounded(ctx, x, y, w, 17, 4, hovered ? Theme.BG_SELECTED : Theme.BG_CARD);
        RenderUtils.drawText(ctx, textRenderer, label, x + 5, y + 4, Theme.TEXT_PRIMARY, false);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
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
                page = 0;
                return true;
            }
        }

        List<BlockCatalog.Option> options = filtered();
        int listY = y + 56;
        int first = page * PAGE_SIZE;
        for (int i = first; i < Math.min(options.size(), first + PAGE_SIZE); i++) {
            int row = i - first;
            int ry = listY + row * ROW_H;
            if (RenderUtils.hovered((int) mx, (int) my, x + 10, ry, PANEL_W - 20, ROW_H - 1)) {
                setting.toggle(options.get(i).id());
                VoidClient.saveConfig();
                return true;
            }
        }

        int footerY = y + PANEL_H - 25;
        if (RenderUtils.hovered((int) mx, (int) my, x + 91, footerY, 48, 17)) {
            setting.setSelectedIds(List.of());
            VoidClient.saveConfig();
            return true;
        }
        if (RenderUtils.hovered((int) mx, (int) my, x + 143, footerY, 42, 17)) {
            setting.setSelectedIds(BlockCatalog.OPTIONS.stream().map(BlockCatalog.Option::id).toList());
            VoidClient.saveConfig();
            return true;
        }
        if (RenderUtils.hovered((int) mx, (int) my, x + 189, footerY, 42, 17)) {
            page = Math.max(0, page - 1);
            return true;
        }
        if (RenderUtils.hovered((int) mx, (int) my, x + 235, footerY, 42, 17)) {
            int pages = Math.max(1, (options.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            page = Math.min(pages - 1, page + 1);
            return true;
        }
        if (RenderUtils.hovered((int) mx, (int) my, x + 281, footerY, 39, 17)) {
            close();
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_LEFT) {
            page = Math.max(0, page - 1);
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_RIGHT) {
            int pages = Math.max(1, (filtered().size() + PAGE_SIZE - 1) / PAGE_SIZE);
            page = Math.min(pages - 1, page + 1);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
