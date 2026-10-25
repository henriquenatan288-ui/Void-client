package dev.voidclient.gui.utils;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Simple drawing helpers on top of DrawContext.
 * Rounded look is approximated with multiple fills (no custom shader).
 */
public final class RenderUtils {

    private RenderUtils() {}

    public static void fill(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + h, color);
    }

    public static void outline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static void fillRounded(DrawContext ctx, int x, int y, int w, int h, int radius, int color) {
        if (radius <= 0 || w < radius * 2 || h < radius * 2) {
            fill(ctx, x, y, w, h, color);
            return;
        }
        fill(ctx, x + radius, y, w - radius * 2, h, color);
        fill(ctx, x, y + radius, radius, h - radius * 2, color);
        fill(ctx, x + w - radius, y + radius, radius, h - radius * 2, color);
        fill(ctx, x + 1, y + 1, radius - 1, radius - 1, color);
        fill(ctx, x + w - radius, y + 1, radius - 1, radius - 1, color);
        fill(ctx, x + 1, y + h - radius, radius - 1, radius - 1, color);
        fill(ctx, x + w - radius, y + h - radius, radius - 1, radius - 1, color);
    }

    public static void drawText(DrawContext ctx, TextRenderer tr, String text, int x, int y, int color, boolean shadow) {
        ctx.drawText(tr, text, x, y, color, shadow);
    }

    public static void drawCenteredText(DrawContext ctx, TextRenderer tr, String text, int centerX, int y, int color, boolean shadow) {
        int w = tr.getWidth(text);
        ctx.drawText(tr, text, centerX - w / 2, y, color, shadow);
    }

    public static boolean isHovered(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }
}
