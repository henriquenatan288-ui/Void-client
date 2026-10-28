package dev.voidclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Rounded rect + hit-test helpers (GuiGraphicsExtractor / MC 26.2). */
public final class Draw {

    private Draw() {}

    public static void rounded(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        g.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            g.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
    }

    public static void roundedOutline(GuiGraphicsExtractor g, int x, int y, int w, int h, int r,
                                      int border, int fill) {
        rounded(g, x, y, w, h, r, fill);
        g.fill(x + r, y, x + w - r, y + 1, border);
        g.fill(x + r, y + h - 1, x + w - r, y + h, border);
        g.fill(x, y + r, x + 1, y + h - r, border);
        g.fill(x + w - 1, y + r, x + w, y + h - r, border);
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
