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

public class ClickGuiScreen extends Screen {

    private static final int SIDEBAR_W = 120;
    private static final int PAD = 14;
    private static final int CARD_GAP = 6;

    private Category selected = Category.COMBAT;
    private final List<ModuleCard> cards = new ArrayList<>();
    private SettingsPanel settingsPanel;

    private float openAnim = 0f;
    private long lastNs = System.nanoTime();

    public ClickGuiScreen() {
        super(Text.literal("Void Client"));
    }

    @Override
    protected void init() {
        openAnim = 0f;
        lastNs = System.nanoTime();
        rebuildCards();
    }

    private void rebuildCards() {
        cards.clear();
        if (VoidClient.MODULE_MANAGER == null) return;
        for (Module m : VoidClient.MODULE_MANAGER.getByCategory(selected)) {
            if (m.getName().equalsIgnoreCase("ClickGUI")) continue;
            cards.add(new ModuleCard(m));
        }
    }

    private float deltaSeconds() {
        long now = System.nanoTime();
        float d = (now - lastNs) / 1_000_000_000f;
        lastNs = now;
        return Math.min(d, 0.05f);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        float dt = deltaSeconds();
        openAnim = Math.min(1f, openAnim + dt * 6f);

        float slide = 1f - openAnim;
        int sidebarOffset = (int) (-SIDEBAR_W * slide * 0.5f);
        int contentFade = (int) (20 * slide);

        int overlayAlpha = (int) (0xB0 * openAnim);
        RenderUtils.fill(context, 0, 0, width, height, (overlayAlpha << 24));

        int sx = sidebarOffset;
        RenderUtils.fill(context, sx, 0, SIDEBAR_W, height, Theme.BG_SIDEBAR);
        RenderUtils.fill(context, sx + SIDEBAR_W - 1, 0, 1, height, Theme.BORDER);

        RenderUtils.drawText(context, textRenderer, "VOID", sx + 14, 18, Theme.TEXT_PRIMARY, false);
        RenderUtils.drawText(context, textRenderer, "CLIENT", sx + 14, 30, Theme.TEXT_MUTED, false);

        int cy = 56;
        for (Category cat : Category.values()) {
            boolean sel = cat == selected;
            boolean hov = RenderUtils.hovered(mouseX, mouseY, sx + 8, cy, SIDEBAR_W - 16, 22);

            if (sel) {
                RenderUtils.fillRounded(context, sx + 8, cy, SIDEBAR_W - 16, 22, 4, Theme.BG_SELECTED);
                RenderUtils.fill(context, sx + 8, cy + 5, 2, 12, Theme.ACCENT);
            } else if (hov) {
                RenderUtils.fillRounded(context, sx + 8, cy, SIDEBAR_W - 16, 22, 4, Theme.BG_CARD);
            }

            int col = sel ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY;
            RenderUtils.drawText(context, textRenderer, cat.getDisplayName(), sx + 18, cy + 7, col, false);
            cy += 26;
        }

        int contentX = SIDEBAR_W + PAD;
        int contentY = PAD + 6 - contentFade;

        RenderUtils.drawText(context, textRenderer, selected.getDisplayName(), contentX, contentY, Theme.TEXT_PRIMARY, false);

        int startY = contentY + 22;
        int col = 0;
        int row = 0;
        int maxCols = Math.max(1, (width - SIDEBAR_W - PAD * 2) / (ModuleCard.WIDTH + CARD_GAP));

        for (ModuleCard card : cards) {
            int cx = contentX + col * (ModuleCard.WIDTH + CARD_GAP);
            int cy2 = startY + row * (ModuleCard.HEIGHT + CARD_GAP);
            card.setPosition(cx, cy2);
            card.update(mouseX, mouseY, dt);
            card.render(context, textRenderer, mouseX, mouseY);

            col++;
            if (col >= maxCols) {
                col = 0;
                row++;
            }
        }

        if (cards.isEmpty()) {
            RenderUtils.drawText(context, textRenderer, "No modules here yet", contentX, startY + 12, Theme.TEXT_MUTED, false);
        }

        if (settingsPanel != null && settingsPanel.isVisible()) {
            settingsPanel.update(dt);
            settingsPanel.render(context, textRenderer, mouseX, mouseY);
        }

        RenderUtils.drawText(context, textRenderer,
                "RShift close  |  LMB toggle  |  RMB settings",
                contentX, height - 14, Theme.TEXT_MUTED, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int cy = 56;
        for (Category cat : Category.values()) {
            if (RenderUtils.hovered((int) mouseX, (int) mouseY, 8, cy, SIDEBAR_W - 16, 22)) {
                selected = cat;
                rebuildCards();
                settingsPanel = null;
                return true;
            }
            cy += 26;
        }

        if (settingsPanel != null && settingsPanel.isVisible()) {
            if (settingsPanel.mouseClicked(mouseX, mouseY, button, textRenderer)) {
                if (!settingsPanel.isVisible()) settingsPanel = null;
                return true;
            }
        }

        for (ModuleCard card : cards) {
            if (card.mouseClicked(mouseX, mouseY, button)) {
                if (button == 1) {
                    int px = card.getX() + ModuleCard.WIDTH + 8;
                    int py = card.getY();
                    if (px + SettingsPanel.WIDTH > width - 8) px = card.getX() - SettingsPanel.WIDTH - 8;
                    if (py + 80 > height) py = height - 100;
                    settingsPanel = new SettingsPanel(card.getModule(), Math.max(SIDEBAR_W + 4, px), Math.max(8, py));
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
