package dev.voidclient.gui.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/**
 * Registers procedural textures at runtime.
 * Confiança: MÉDIA — NativeImage.setColorArgb / registerTexture podem variar no Yarn.
 */
public final class EmbeddedTextures {

    private static boolean registered;

    private EmbeddedTextures() {}

    public static void ensureRegistered() {
        if (registered) return;
        registered = true;
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.getTextureManager() == null) return;

            registerSolid(mc, GuiTextures.WHITE, 1, 1, 255, 255, 255, 255);
            registerRounded(mc, GuiTextures.PANEL, 64, 12, 16, 16, 16, 240);
            registerRounded(mc, GuiTextures.CARD, 64, 10, 22, 22, 22, 245);
            registerShadow(mc, GuiTextures.SHADOW, 96);
        } catch (Throwable ignored) {
            // GUI keeps procedural fallback
        }
    }

    private static void registerSolid(MinecraftClient mc, Identifier id, int w, int h, int r, int g, int b, int a) {
        NativeImage img = new NativeImage(w, h, false);
        int argb = (a << 24) | (r << 16) | (g << 8) | b;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setColorArgb(x, y, argb);
            }
        }
        mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(img));
    }

    private static void registerRounded(MinecraftClient mc, Identifier id, int size, int radius, int r, int g, int b, int a) {
        NativeImage img = new NativeImage(size, size, false);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                img.setColorArgb(x, y, 0);
            }
        }
        int color = (a << 24) | (r << 16) | (g << 8) | b;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (insideRounded(x, y, size, size, radius)) {
                    img.setColorArgb(x, y, color);
                }
            }
        }
        mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(img));
    }

    private static void registerShadow(MinecraftClient mc, Identifier id, int size) {
        NativeImage img = new NativeImage(size, size, false);
        float cx = size / 2f, cy = size / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x - cx) / cx;
                float dy = (y - cy) / cy;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                int alpha = d >= 1f ? 0 : (int) ((1f - d) * (1f - d) * 90);
                img.setColorArgb(x, y, (alpha << 24));
            }
        }
        mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(img));
    }

    private static boolean insideRounded(int x, int y, int w, int h, int r) {
        if (x >= r && x < w - r && y >= 0 && y < h) return true;
        if (y >= r && y < h - r && x >= 0 && x < w) return true;
        if (dist2(x, y, r, r) <= r * r) return true;
        if (dist2(x, y, w - r - 1, r) <= r * r) return true;
        if (dist2(x, y, r, h - r - 1) <= r * r) return true;
        return dist2(x, y, w - r - 1, h - r - 1) <= r * r;
    }

    private static int dist2(int x, int y, int cx, int cy) {
        int dx = x - cx, dy = y - cy;
        return dx * dx + dy * dy;
    }
}
