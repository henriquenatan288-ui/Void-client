package dev.voidclient.gui;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ClickGUI black/purple (Minecraft 26.2).
 * Sidebar categories, search, scrollable module pills.
 */
public class ClickGuiScreen extends Screen {

    private static final int W = 430, H = 310;
    private static final int PAD = 8;
    private static final int SIDE_W = 96;
    private static final int CAT_H = 26;
    private static final int SEARCH_H = 18;
    private static final int ROW_H = 18, ROW_GAP = 2, ROW_W = 150;

    private final List<GuiModule> modules;

    private Category selected = Category.COMBAT;
    private final StringBuilder query = new StringBuilder();
    private boolean searchFocused;

    private float scroll, scrollTarget;

    private int px, py, pw, ph;
    private int sx, sy, sh;
    private int cx, cy, cw;
    private int lx, ly, lw, lh;

    /** Opens GUI wired to live ModuleManager modules. */
    public ClickGuiScreen() {
        this(fromManager());
    }

    public ClickGuiScreen(List<GuiModule> modules) {
        super(Component.literal("ClickGUI"));
        this.modules = modules;
    }

    private static List<GuiModule> fromManager() {
        List<GuiModule> out = new ArrayList<>();
        if (VoidClient.MODULE_MANAGER == null) return out;
        for (Module m : VoidClient.MODULE_MANAGER.getModules()) {
            if (m.getName().equalsIgnoreCase("ClickGUI")) continue;
            out.add(adapt(m));
        }
        return out;
    }

    private static GuiModule adapt(Module m) {
        return new GuiModule() {
            @Override public String name() { return m.getName(); }
            @Override public Category category() { return m.getCategory(); }
            @Override public boolean isEnabled() { return m.isEnabled(); }
            @Override public void toggle() { m.toggle(); }
            @Override public void openSettings() {
                // parent set when opening — use current screen if still ClickGuiScreen
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc != null && mc.gui != null) {
                    Screen parent = mc.gui.screen();
                    mc.gui.setScreen(new ModuleSettingsScreen(parent, m));
                }
            }
        };
    }

    @Override
    protected void init() {
        pw = Math.min(W, width - 20);
        ph = Math.min(H, height - 20);
        px = (width - pw) / 2;
        py = (height - ph) / 2;

        sx = px + PAD;
        sy = py + PAD;
        sh = ph - PAD * 2;

        cx = sx + SIDE_W + PAD;
        cy = py + PAD;
        cw = px + pw - PAD - cx;

        lx = cx;
        ly = cy + SEARCH_H + 6;
        lw = cw;
        lh = py + ph - PAD - ly;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<GuiModule> visible() {
        List<GuiModule> out = new ArrayList<>();
        String q = query.toString().toLowerCase(Locale.ROOT).trim();
        for (GuiModule m : modules) {
            if (q.isEmpty()) {
                if (m.category() == selected) out.add(m);
            } else if (m.name().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(m);
            }
        }
        return out;
    }

    private float maxScroll(int count) {
        return Math.max(0, count * (ROW_H + ROW_GAP) - ROW_GAP - lh);
    }

    private int rowY(int i) {
        return ly + i * (ROW_H + ROW_GAP) - Math.round(scroll);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, Theme.BG_OVERLAY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        List<GuiModule> list = visible();
        scrollTarget = Math.max(0, Math.min(scrollTarget, maxScroll(list.size())));
        scroll += (scrollTarget - scroll) * 0.35f;
        if (Math.abs(scrollTarget - scroll) < 0.05f) scroll = scrollTarget;

        Draw.roundedOutline(g, px, py, pw, ph, 6, Theme.BORDER_STRONG, Theme.PANEL_BG);

        drawSidebar(g, mouseX, mouseY);
        drawSearch(g);
        drawList(g, list, mouseX, mouseY);
    }

    private void drawSidebar(GuiGraphicsExtractor g, int mx, int my) {
        Draw.rounded(g, sx, sy, SIDE_W, sh, 5, Theme.BG_SIDEBAR);

        boolean searching = query.length() > 0;
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            Category c = cats[i];
            int y = sy + 6 + i * (CAT_H + 2);
            int x = sx + 5;
            int w = SIDE_W - 10;

            boolean sel = c == selected && !searching;
            boolean hov = Draw.inside(mx, my, x, y, w, CAT_H);

            if (sel) {
                Draw.rounded(g, x, y, w, CAT_H, 4, Theme.BG_SELECTED);
                g.fill(x, y + 5, x + 2, y + CAT_H - 5, Theme.ACCENT);
            } else if (hov) {
                Draw.rounded(g, x, y, w, CAT_H, 4, Theme.BG_CARD_HOVER);
            }

            int ic = sel ? Theme.ACCENT : Theme.TEXT_MUTED;
            Draw.rounded(g, x + 8, y + (CAT_H - 7) / 2, 7, 7, 2, ic);

            g.text(font, c.getDisplayName(), x + 22, y + (CAT_H - font.lineHeight) / 2 + 1,
                    sel ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY, false);
        }
    }

    private void drawSearch(GuiGraphicsExtractor g) {
        Draw.roundedOutline(g, cx, cy, cw, SEARCH_H, 4,
                searchFocused ? Theme.ACCENT : Theme.BORDER,
                searchFocused ? Theme.BG_INPUT_FOCUS : Theme.BG_INPUT);

        int ty = cy + (SEARCH_H - font.lineHeight) / 2 + 1;
        if (query.length() == 0 && !searchFocused) {
            g.text(font, "Search all modules...", cx + 8, ty, Theme.TEXT_MUTED, false);
            return;
        }
        String shown = query.toString();
        String cursor = searchFocused && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "";
        while (!shown.isEmpty() && font.width(shown + cursor) > cw - 16) shown = shown.substring(1);
        g.text(font, shown + cursor, cx + 8, ty, Theme.TEXT_PRIMARY, false);
    }

    private void drawList(GuiGraphicsExtractor g, List<GuiModule> list, int mx, int my) {
        g.enableScissor(lx, ly, lx + lw, ly + lh);

        if (list.isEmpty()) {
            g.text(font, "No modules found.", lx + 4, ly + 4, Theme.TEXT_MUTED, false);
        }

        boolean mouseInList = Draw.inside(mx, my, lx, ly, lw, lh);
        for (int i = 0; i < list.size(); i++) {
            int y = rowY(i);
            if (y + ROW_H < ly || y > ly + lh) continue;

            GuiModule m = list.get(i);
            boolean on = m.isEnabled();
            boolean hov = mouseInList && Draw.inside(mx, my, lx, y, ROW_W, ROW_H);

            if (on) {
                Draw.rounded(g, lx, y, ROW_W, ROW_H, 4, Theme.MODULE_ON);
            } else if (hov) {
                Draw.rounded(g, lx, y, ROW_W, ROW_H, 4, Theme.MODULE_HOVER);
            }

            g.text(font, m.name(), lx + 8, y + (ROW_H - font.lineHeight) / 2 + 1,
                    on || hov ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY, false);
        }

        g.disableScissor();

        float max = maxScroll(list.size());
        if (max > 0) {
            int total = list.size() * (ROW_H + ROW_GAP);
            int thumbH = Math.max(14, (int) (lh * (lh / (float) total)));
            int thumbY = ly + (int) ((lh - thumbH) * (scroll / max));
            Draw.rounded(g, lx + lw - 3, thumbY, 3, thumbH, 1, Theme.SCROLL_THUMB);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mx = event.x(), my = event.y();
        int button = event.button();

        searchFocused = Draw.inside(mx, my, cx, cy, cw, SEARCH_H);
        if (searchFocused) return true;

        if (button == 0) {
            Category[] cats = Category.values();
            for (int i = 0; i < cats.length; i++) {
                int y = sy + 6 + i * (CAT_H + 2);
                if (Draw.inside(mx, my, sx + 5, y, SIDE_W - 10, CAT_H)) {
                    selected = cats[i];
                    query.setLength(0);
                    scroll = scrollTarget = 0;
                    return true;
                }
            }
        }

        if (Draw.inside(mx, my, lx, ly, lw, lh)) {
            List<GuiModule> list = visible();
            for (int i = 0; i < list.size(); i++) {
                int y = rowY(i);
                if (Draw.inside(mx, my, lx, y, ROW_W, ROW_H)) {
                    if (button == 0) list.get(i).toggle();
                    else if (button == 1) list.get(i).openSettings();
                    return true;
                }
            }
        }

        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (Draw.inside(mouseX, mouseY, lx, ly, lw, lh)) {
            scrollTarget -= (float) scrollY * (ROW_H + ROW_GAP) * 2;
            scrollTarget = Math.max(0, Math.min(scrollTarget, maxScroll(visible().size())));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();

        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
            } else if (key == GLFW.GLFW_KEY_BACKSPACE && query.length() > 0) {
                query.deleteCharAt(query.length() - 1);
                scroll = scrollTarget = 0;
            }
            return true;
        }

        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT || key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!searchFocused) return super.charTyped(event);
        int cp = event.codepoint();
        if (!Character.isISOControl(cp) && query.length() < 24) {
            query.appendCodePoint(cp);
            scroll = scrollTarget = 0;
        }
        return true;
    }

    @Override
    public void onClose() {
        if (VoidClient.CONFIG_MANAGER != null) {
            try { VoidClient.CONFIG_MANAGER.save(); } catch (Throwable ignored) {}
        }
        super.onClose();
    }
}
