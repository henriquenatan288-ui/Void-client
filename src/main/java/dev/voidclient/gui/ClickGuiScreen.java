package dev.voidclient.gui;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.gui.components.ModuleCard;
import dev.voidclient.gui.utils.RenderUtils;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ClickGuiScreen extends Screen {

    private static final int SIDEBAR_W = 120;
    private static final int PAD = 14;
    private static final int CARD_GAP = 6;

    private Category selected = Category.COMBAT;
    private final List<ModuleCard> cards = new ArrayList<>();

    private float openAnim = 0f;
    private long lastNs = System.nanoTime();

    public ClickGuiScreen() {
        super(Component.literal("Void Client"));
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
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float tickDelta) {
        float dt = deltaSeconds();
        openAnim = Math.min(1f, openAnim + dt * 6f);

        float slide = 1f - openAnim;
        int sidebarOffset = (int) (-SIDEBAR_W * slide * 0.5f);
        int contentFade = (int) (20 * slide);

        int overlayAlpha = (int) (0xB0 * openAnim);
        RenderUtils.fill(context, 0, 0, width, height, (overlayAlpha << 24));

        int sx = sidebarOffset;
        RenderUtils.fill(context, sx, 0, SIDEBAR_W, height, Theme.BG_SIDEBAR());
        RenderUtils.fill(context, sx + SIDEBAR_W - 1, 0, 1, height, Theme.BORDER());

        RenderUtils.drawText(context, font, "VOID", sx + 14, 18, Theme.TEXT_PRIMARY(), false);
        RenderUtils.drawText(context, font, "CLIENT", sx + 14, 30, Theme.TEXT_MUTED(), false);

        int cy = 56;
        for (Category cat : Category.values()) {
            boolean sel = cat == selected;
            boolean hov = RenderUtils.hovered(mouseX, mouseY, sx + 8, cy, SIDEBAR_W - 16, 22);

            if (sel) {
                RenderUtils.fillRounded(context, sx + 8, cy, SIDEBAR_W - 16, 22, 4, Theme.BG_SELECTED());
                RenderUtils.fill(context, sx + 8, cy + 5, 2, 12, Theme.ACCENT());
            } else if (hov) {
                RenderUtils.fillRounded(context, sx + 8, cy, SIDEBAR_W - 16, 22, 4, Theme.BG_CARD());
            }

            int col = sel ? Theme.TEXT_PRIMARY() : Theme.TEXT_SECONDARY();
            RenderUtils.drawText(context, font, cat.getDisplayName(), sx + 18, cy + 7, col, false);
            cy += 26;
        }

        int contentX = SIDEBAR_W + PAD;
        int contentY = PAD + 6 - contentFade;

        RenderUtils.drawText(context, font, selected.getDisplayName(), contentX, contentY, Theme.TEXT_PRIMARY(), false);

        int startY = contentY + 22;
        int col = 0;
        int row = 0;
        int maxCols = Math.max(1, (width - SIDEBAR_W - PAD * 2) / (ModuleCard.WIDTH + CARD_GAP));

        for (ModuleCard card : cards) {
            int cx = contentX + col * (ModuleCard.WIDTH + CARD_GAP);
            int cy2 = startY + row * (ModuleCard.HEIGHT + CARD_GAP);
            card.setPosition(cx, cy2);
            card.update(mouseX, mouseY, dt);
            card.render(context, font, mouseX, mouseY);

            col++;
            if (col >= maxCols) {
                col = 0;
                row++;
            }
        }

        if (cards.isEmpty()) {
            RenderUtils.drawText(context, font, "No modules here yet", contentX, startY + 12, Theme.TEXT_MUTED(), false);
        }

        RenderUtils.drawText(context, font,
                "RShift close  |  LMB toggle  |  RMB settings",
                contentX, height - 14, Theme.TEXT_MUTED(), false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        int cy = 56;
        for (Category cat : Category.values()) {
            if (RenderUtils.hovered((int) mouseX, (int) mouseY, 8, cy, SIDEBAR_W - 16, 22)) {
                selected = cat;
                rebuildCards();
                return true;
            }
            cy += 26;
        }

        for (ModuleCard card : cards) {
            if (card.mouseClicked(mouseX, mouseY, button)) {
                if (button == 1) {
                    if (minecraft != null) {
                        minecraft.gui.setScreen(new ModuleSettingsScreen(this, card.getModule()));
                    }
                }
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int keyCode = input.key();
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (VoidClient.CONFIG_MANAGER != null) {
            VoidClient.CONFIG_MANAGER.save();
        }
        super.onClose();
    }
}
