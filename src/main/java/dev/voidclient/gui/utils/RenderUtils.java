package dev.voidclient.gui.utils;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Identifier;

/**
 * Drawing helpers: procedural shapes + textured panels (1.21.11).
 */
public final class RenderUtils {

    private RenderUtils() {}

    public static void fill(DrawContext ctx, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        ctx.fill(x, y, x + w, y + h, color);
    }

    public static void outline(DrawContext ctx, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        fill(ctx, x, y, w, 1, color);
        fill(ctx, x, y + h - 1, w, 1, color);
        fill(ctx, x, y, 1, h, color);
        fill(ctx, x + w - 1, y, 1, h, color);
    }

    public static void fillRounded(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        if (r <= 0 || w < r * 2 || h < r * 2) {
            fill(ctx, x, y, w, h, color);
            return;
        }
        fill(ctx, x + r, y, w - 2 * r, h, color);
        fill(ctx, x, y + r, r, h - 2 * r, color);
        fill(ctx, x + w - r, y + r, r, h - 2 * r, color);
        for (int i = 0; i < r; i++) {
            int t = r - i;
            fill(ctx, x + i, y + t - 1, 1, r - t + 1, color);
            fill(ctx, x + w - i - 1, y + t - 1, 1, r - t + 1, color);
            fill(ctx, x + i, y + h - r, 1, r - t + 1, color);
            fill(ctx, x + w - i - 1, y + h - r, 1, r - t + 1, color);
        }
    }

    public static void drawShadow(DrawContext ctx, int x, int y, int w, int h, int radius) {
        if (blit(ctx, GuiTextures.SHADOW, x - 8, y - 4, w + 16, h + 16)) {
            return;
        }
        int[] alphas = {0x10000000, 0x18000000, 0x22000000};
        for (int i = 0; i < alphas.length; i++) {
            int o = (alphas.length - i) * 2;
            fillRounded(ctx, x - o, y - o + 2, w + o * 2, h + o * 2, radius + 2, alphas[i]);
        }
    }

    public static void drawPanel(DrawContext ctx, int x, int y, int w, int h, int radius, int fallbackColor) {
        drawShadow(ctx, x, y, w, h, radius);
        if (!blit(ctx, GuiTextures.PANEL, x, y, w, h)) {
            fillRounded(ctx, x, y, w, h, radius, fallbackColor);
            outline(ctx, x, y, w, h, 0x1CFFFFFF);
        }
    }

    public static void drawCard(DrawContext ctx, int x, int y, int w, int h, int radius, int fallbackColor) {
        if (!blit(ctx, GuiTextures.CARD, x, y, w, h)) {
            fillRounded(ctx, x, y, w, h, radius, fallbackColor);
            outline(ctx, x, y, w, h, 0x14FFFFFF);
        }
    }

    /**
     * VERIFICAR: overload de blit no Yarn 1.21.11 do seu build.
     * Docs: blit(RenderPipelines.GUI_TEXTURED, id, x, y, u, v, w, h, texW, texH)
     */
    public static boolean blit(DrawContext ctx, Identifier texture, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return false;
        int tex = textureSize(texture);
        try {
            ctx.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, w, h, tex, tex);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static int textureSize(Identifier texture) {
        String p = texture.getPath();
        if (p.contains("shadow")) return 96;
        if (p.contains("white")) return 1;
        if (p.contains("accent")) return 32;
        return 64;
    }

    public static void drawText(DrawContext ctx, TextRenderer tr, String text, int x, int y, int color, boolean shadow) {
        ctx.drawText(tr, text, x, y, color, shadow);
    }

    public static int textWidth(TextRenderer tr, String text) {
        return tr.getWidth(text);
    }

    public static boolean hovered(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static int lerpColor(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int af = (from >> 24) & 0xFF, rf = (from >> 16) & 0xFF, gf = (from >> 8) & 0xFF, bf = from & 0xFF;
        int at = (to >> 24) & 0xFF, rt = (to >> 16) & 0xFF, gt = (to >> 8) & 0xFF, bt = to & 0xFF;
        int a = (int) lerp(af, at, t);
        int r = (int) lerp(rf, rt, t);
        int g = (int) lerp(gf, gt, t);
        int b = (int) lerp(bf, bt, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
