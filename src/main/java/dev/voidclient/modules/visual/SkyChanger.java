package dev.voidclient.modules.visual;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.ModeSetting;
import dev.voidclient.core.setting.NumberSetting;
import dev.voidclient.render.VoidDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Standalone camera-relative sky decoration.
 * Terrain naturally occludes the stars and bands, keeping the effect in the world
 * rather than painting over the HUD.
 */
public final class SkyChanger extends Module {
    private final ModeSetting style = addSetting(new ModeSetting(
            "Style", "Sky appearance", "Nebula", "Nebula", "Aurora", "Starfield"));
    private final NumberSetting intensity = addSetting(
            new NumberSetting("Intensity", "Strength of sky effects", 0.7, 0.2, 1.0, 0.05));

    public SkyChanger() {
        super("Sky Changer", "Adds a subtle neon starfield, nebula, or aurora.", Category.RENDER);
        VoidDraw.EXTRACTORS.add(this::extract);
    }

    private void extract(VoidDraw.Sink sink, Minecraft mc, float partialTick) {
        if (!isEnabled() || mc.level == null || mc.player == null) return;
        double time = (mc.level.getGameTime() + partialTick) / 20.0;
        Vec3 cam = mc.gameRenderer.mainCamera().position();
        drawSky(sink, cam, time);
    }

    private void drawSky(VoidDraw.Sink sink, Vec3 cam, double time) {
        int starCount = style.is("Starfield") ? 88 : 56;
        for (int i = 0; i < starCount; i++) {
            double azimuth = h(i, 0) * Math.PI * 2.0;
            double elevation = 0.08 + h(i, 1) * 0.88;
            double horizontal = Math.sqrt(Math.max(0.0, 1.0 - elevation * elevation));
            double distance = 78.0 + h(i, 2) * 16.0;
            double x = cam.x + Math.cos(azimuth) * horizontal * distance;
            double y = cam.y + elevation * distance;
            double z = cam.z + Math.sin(azimuth) * horizontal * distance;
            double pulse = 0.35 + 0.65 * (0.5 + 0.5 *
                    Math.sin(time * (0.7 + h(i, 4)) + h(i, 5) * 6.28));
            int color = skyColor(i);
            double size = 0.045 + h(i, 6) * 0.07;
            sink.box(x - size, y - size, z - size, x + size, y + size, z + size,
                    argb(color, intensity.get() * pulse * 0.72), false);
            if (i % 12 == 0) {
                line(sink, new Vec3(x - size * 2.2, y, z),
                        new Vec3(x + size * 2.2, y, z), 0.009f,
                        color, intensity.get() * pulse * 0.42);
                line(sink, new Vec3(x, y - size * 2.2, z),
                        new Vec3(x, y + size * 2.2, z), 0.009f,
                        color, intensity.get() * pulse * 0.42);
            }
        }

        if (style.is("Nebula")) {
            ring(sink, cam.x, cam.y + 25.0, cam.z, 72.0, 56,
                    time * 0.018, 0x32E9FF, 0x9B54FF, intensity.get() * 0.16);
            ring(sink, cam.x, cam.y + 32.0, cam.z, 88.0, 56,
                    -time * 0.012, 0x8A4DFF, 0x48F6B8, intensity.get() * 0.10);
        } else if (style.is("Aurora")) {
            for (int band = 0; band < 3; band++) {
                Vec3 previous = null;
                for (int j = 0; j <= 56; j++) {
                    double angle = j * Math.PI * 2.0 / 56.0;
                    double radius = 64.0 + band * 8.0;
                    double y = cam.y + 27.0
                            + Math.sin(angle * 2.0 + time * 0.22 + band) * (2.0 + band);
                    Vec3 point = new Vec3(cam.x + Math.cos(angle) * radius, y,
                            cam.z + Math.sin(angle) * radius);
                    if (previous != null) {
                        int color = mix(0x39F2FF, 0xB24CFF,
                                (Math.sin(angle + band) + 1.0) * 0.5);
                        line(sink, previous, point, 0.022f, color, intensity.get() * 0.12);
                    }
                    previous = point;
                }
            }
        }
    }

    private void ring(VoidDraw.Sink sink, double x, double y, double z, double radius,
                      int segments, double phase, int startColor, int endColor, double alpha) {
        Vec3 previous = null;
        for (int i = 0; i <= segments; i++) {
            double angle = phase + i * Math.PI * 2.0 / segments;
            Vec3 point = new Vec3(x + Math.cos(angle) * radius, y,
                    z + Math.sin(angle) * radius);
            if (previous != null) {
                int color = mix(startColor, endColor, (double) i / segments);
                line(sink, previous, point, 0.014f, color, alpha);
            }
            previous = point;
        }
    }

    private void line(VoidDraw.Sink sink, Vec3 from, Vec3 to,
                      float width, int color, double alpha) {
        sink.line(from, to, width * 2.0f, false, argb(color, alpha * 0.24), false);
        sink.line(from, to, width, false, argb(color, alpha), false);
    }

    private int skyColor(int index) {
        if (style.is("Aurora")) return switch (index % 3) {
            case 0 -> 0x48FFD0;
            case 1 -> 0x48DFFF;
            default -> 0xAD6BFF;
        };
        if (style.is("Starfield")) return index % 2 == 0 ? 0xC8D7FF : 0xFFFFFF;
        return switch (index % 3) {
            case 0 -> 0x46F1FF;
            case 1 -> 0xA967FF;
            default -> 0x91FFCB;
        };
    }

    private static int mix(int a, int b, double amount) {
        double t = Math.clamp(amount, 0.0, 1.0);
        int red = (int) Math.round(((a >>> 16) & 255) * (1 - t) + ((b >>> 16) & 255) * t);
        int green = (int) Math.round(((a >>> 8) & 255) * (1 - t) + ((b >>> 8) & 255) * t);
        int blue = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return (red << 16) | (green << 8) | blue;
    }

    private static int argb(int rgb, double alpha) {
        int a = (int) Math.round(Math.clamp(alpha, 0.0, 1.0) * 255.0);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    private static double h(int i, int channel) {
        long x = (i * 0x9E3779B97F4A7C15L) ^ (channel * 0xBF58476D1CE4E5B9L);
        x ^= x >>> 30;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 27;
        x *= 0x94D049BB133111EBL;
        x ^= x >>> 31;
        return (x >>> 11) * (1.0 / (1L << 53));
    }
}
