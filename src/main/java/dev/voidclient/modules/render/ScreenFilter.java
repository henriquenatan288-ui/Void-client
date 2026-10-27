package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.ModeSetting;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ScreenFilter extends Module {
    private final ModeSetting preset = addSetting(new ModeSetting("Preset", "Filter look", "Void",
            "Void", "Warm", "Cool", "Night", "Vintage", "Custom"));
    private final ColorSetting custom = addSetting(new ColorSetting("Custom color", "Used by Custom preset", 0xFF7B2CFF));
    private final NumberSetting intensity = addSetting(new NumberSetting("Intensity", "Filter strength", 0.16, 0.02, 0.6, 0.01));
    private final BooleanSetting vignette = addSetting(new BooleanSetting("Vignette", "Dark edges", true));
    private final NumberSetting vignetteStrength = addSetting(new NumberSetting("Vignette strength", "Edge darkness", 0.5, 0.1, 1.0, 0.05));
    private final BooleanSetting pulse = addSetting(new BooleanSetting("Pulse", "Slow breathing effect", false));

    public ScreenFilter() {
        super("ScreenFilter", "Color filter and vignette over the screen.", Category.RENDER);
    }

    private int baseColor() {
        return switch (preset.get()) {
            case "Warm" -> 0xFF9A3C;
            case "Cool" -> 0x3C8CFF;
            case "Night" -> 0x0A1040;
            case "Vintage" -> 0xC9A66B;
            case "Custom" -> custom.get() & 0xFFFFFF;
            default -> 0x7B2CFF;
        };
    }

    public void renderHud(GuiGraphicsExtractor ctx, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || ctx == null) return;
        int w, h;
        try {
            w = mc.getWindow().getGuiScaledWidth();
            h = mc.getWindow().getGuiScaledHeight();
        } catch (Throwable t) {
            return;
        }

        double k = intensity.get();
        if (pulse.get()) k *= 0.8 + 0.2 * Math.sin(System.nanoTime() / 6.0e8);
        if ("Night".equals(preset.get())) k = Math.min(0.7, k * 1.8);
        ctx.fill(0, 0, w, h, argb(baseColor(), k));

        if (vignette.get()) {
            int bands = 12;
            int th = Math.max(2, Math.max(w, h) / 60);
            for (int i = 0; i < bands; i++) {
                double f = 1 - i / (double) bands;
                int c = argb(0x000000, vignetteStrength.get() * 0.55 * f * f);
                int o = i * th;
                ctx.fill(o, o, w - o, o + th, c);
                ctx.fill(o, h - o - th, w - o, h - o, c);
                ctx.fill(o, o + th, o + th, h - o - th, c);
                ctx.fill(w - o - th, o + th, w - o, h - o - th, c);
            }
        }
    }

    private static int argb(int rgb, double a) {
        a = Math.max(0, Math.min(1, a));
        return ((int) Math.round(a * 255) << 24) | (rgb & 0xFFFFFF);
    }
}
