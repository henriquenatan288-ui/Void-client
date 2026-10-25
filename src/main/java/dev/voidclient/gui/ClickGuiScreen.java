package dev.voidclient.gui;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.gui.components.ModuleCard;
import dev.voidclient.gui.components.SettingsPanel;
import dev.voidclient.gui.utils.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Main ClickGUI screen — dark neon, sidebar + module cards + settings panels.
 * Opens with Right Shift.
 */
public class ClickGuiScreen extends Screen {

    private static final int SIDEBAR_WIDTH = 140;
    private static final int CARD_GAP = 8;
    private static final int PAD = 12;

    private Category selectedCategory = Category.COMBAT;
    private final List<ModuleCard> cards = new ArrayList<>();
    private SettingsPanel settingsPanel;

    public ClickGuiScreen() {
        super(Text.literal("Void Client"));
    }

    @Override
    protected void init() {
        rebuildCards();
    }

    private void rebuildCards() {
        cards.clear();
        if (VoidClient.MODULE_MANAGER == null) return;

        List<Module> modules = VoidClient.MODULE_MANAGER.getByCategory(selectedCategory);
        for (Module m : modules) {
            cards.add(new ModuleCard(m));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        RenderUtils.fill(context, 0, 0, width, height, Theme.OVERLAY);

        RenderUtils.fill(context, 0, 0, SIDEBAR_WIDTH, height, Theme.BG_SIDEBAR);
        RenderUtils.fill(context, SIDEBAR_WIDTH - 1, 0, 1, height, Theme.BORDER);

        RenderUtils.drawText(context, textRenderer, "VOID", 16, 16, Theme.ACCENT, false);
        RenderUtils.drawText(context, textRenderer, "CLIENT", 16, 28, Theme.TEXT_PRIMARY, false);

        int cy = 56;
        for (Category cat : Category.values()) {
            boolean selected = cat == selectedCategory;
            boolean hovered = RenderUtils.isHovered(mouseX, mouseY, 8, cy, SIDEBAR_WIDTH - 16, 22);

            if (selected) {
                RenderUtils.fillRounded(context, 8, cy, SIDEBAR_WIDTH - 16, 22, 4, Theme.ACCENT_DIM);
            } else if (hovered) {
                RenderUtils.fillRounded(context, 8, cy, SIDEBAR_WIDTH - 16, 22, 4, Theme.BG_CARD);
            }

            int color = selected ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY;
            RenderUtils.drawText(context, textRenderer, cat.getDisplayName(), 18, cy + 7, color, false);
            cy += 26;
        }

        int contentX = SIDEBAR_WIDTH + PAD;
        int contentY = PAD + 8;

        RenderUtils.drawText(context, textRenderer, selectedCategory.getDisplayName(), contentX, contentY, Theme.TEXT_PRIMARY, false);

        int startY = contentY + 24;
        int col = 0;
        int row = 0;
        int maxCols = Math.max(1, (width - SIDEBAR_WIDTH - PAD * 2) / (ModuleCard.WIDTH + CARD_GAP));

        for (ModuleCard card : cards) {
            int cx = contentX + col * (ModuleCard.WIDTH + CARD_GAP);
            int cy2 = startY + row * (ModuleCard.HEIGHT + CARD_GAP);
            card.setPosition(cx, cy2);
            card.render(context, textRenderer, mouseX, mouseY);

            col++;
            if (col >= maxCols) {
                col = 0;
                row++;
            }
        }

        if (cards.isEmpty()) {
            RenderUtils.drawText(context, textRenderer, "No modules in this category", contentX, startY + 20, Theme.TEXT_MUTED, false);
        }

        if (settingsPanel != null && settingsPanel.isVisible()) {
            settingsPanel.render(context, textRenderer, mouseX, mouseY);
        }

        RenderUtils.drawText(context, textRenderer, "Right Shift to close  |  Left click toggle  |  Right click settings", contentX, height - 16, Theme.TEXT_MUTED, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int cy = 56;
        for (Category cat : Category.values()) {
            if (RenderUtils.isHovered((int) mouseX, (int) mouseY, 8, cy, SIDEBAR_WIDTH - 16, 22)) {
                selectedCategory = cat;
                rebuildCards();
                settingsPanel = null;
                return true;
            }
            cy += 26;
        }

        if (settingsPanel != null && settingsPanel.isVisible()) {
            if (settingsPanel.mouseClicked(mouseX, mouseY, button, textRenderer)) {
                if (!settingsPanel.isVisible()) {
                    settingsPanel = null;
                }
                return true;
            }
        }

        for (ModuleCard card : cards) {
            if (card.mouseClicked(mouseX, mouseY, button)) {
                if (button == 1) {
                    settingsPanel = new SettingsPanel(card.getModule(), card.getX() + ModuleCard.WIDTH + 8, card.getY());
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (VoidClient.CONFIG_MANAGER != null) {
            VoidClient.CONFIG_MANAGER.save();
        }
        super.close();
    }
}
