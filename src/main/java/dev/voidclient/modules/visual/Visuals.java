package dev.voidclient.modules.visual;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.ModeSetting;
import dev.voidclient.core.setting.NumberSetting;
import dev.voidclient.modules.render.VoidRenderLayers;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

/**
 * Minecraft 1.21.11-compatible visual options.
 * The custom sky draws lightweight depth-tested accents; it does not replace vanilla sky rendering.
 */
public final class Visuals extends Module {
    private final BooleanSetting customSky = addSetting(
            new BooleanSetting("Custom Sky", "Draw starfield and ring accents in the sky", false));
    private final ModeSetting skyStyle = addSetting(
            new ModeSetting("Sky Style", "Custom sky accent style", "Nebula",
                    "Nebula", "Aurora", "Starfield"));
    private final NumberSetting skyIntensity = addSetting(
            new NumberSetting("Sky Intensity", "Brightness of sky accents", 0.8, 0.1, 1.0, 0.05));
    private final BooleanSetting screenFilter = addSetting(
            new BooleanSetting("Screen Filter", "Apply a subtle tint to the game view", false));
    private final ModeSetting filterStyle = addSetting(
            new ModeSetting("Filter Style", "Tint color", "Violet",
                    "Violet", "Cyan", "Amber", "Night"));
    private final NumberSetting filterOpacity = addSetting(
            new NumberSetting("Filter Opacity", "Tint strength", 0.22, 0.05, 0.5, 0.01));

    public Visuals() {
        super("Visuals", "Custom sky accents and screen color filters.", Category.RENDER);
    }

    public static void renderScreenFilter(DrawContext ctx) {
        if (VoidClient.MODULE_MANAGER == null) return;
        Module candidate = VoidClient.MODULE_MANAGER.getByName("Visuals");
        if (!(candidate instanceof Visuals visuals) || !visuals.isEnabled() || !visuals.screenFilter.get()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.currentScreen != null || mc.getWindow() == null) return;

        int rgb = switch (visuals.filterStyle.get()) {
            case "Cyan" -> 0x32D8FF;
            case "Amber" -> 0xFFAC45;
            case "Night" -> 0x171A3B;
            default -> 0x8A35FF;
        };
        int alpha = Math.max(1, Math.min(128, (int) (visuals.filterOpacity.get() * 255.0)));
        ctx.fill(0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(),
                (alpha << 24) | rgb);
    }

    public static void renderCustomSky(WorldRenderContext context) {
        if (VoidClient.MODULE_MANAGER == null || context.consumers() == null) return;
        Module candidate = VoidClient.MODULE_MANAGER.getByName("Visuals");
        if (!(candidate instanceof Visuals visuals) || !visuals.isEnabled() || !visuals.customSky.get()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.gameRenderer == null) return;
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        VertexConsumer vertices = context.consumers().getBuffer(VoidRenderLayers.ESP_LINES);
        float intensity = visuals.skyIntensity.getFloat();
        String style = visuals.skyStyle.get();

        int starColor = style.equals("Aurora") ? 0xFF53FFD4
                : style.equals("Starfield") ? 0xFFB9D7FF : 0xFFB66BFF;
        int accentColor = style.equals("Aurora") ? 0xD944FFD0 : 0xD68D42FF;

        // Deterministic distant star marks so the effect does not flicker between frames.
        for (int i = 0; i < 58; i++) {
            double x = (hash(i, 0) * 2.0 - 1.0) * 82.0;
            double y = 22.0 + hash(i, 1) * 58.0;
            double z = (hash(i, 2) * 2.0 - 1.0) * 82.0;
            if (x * x + (y - 30.0) * (y - 30.0) + z * z < 34.0 * 34.0) continue;
            double s = 0.10 + hash(i, 3) * 0.20;
            line(vertices, x - s, y, z, x + s, y, z, cameraPos, withAlpha(starColor, intensity));
            line(vertices, x, y - s, z, x, y + s, z, cameraPos, withAlpha(starColor, intensity));
        }

        if (style.equals("Nebula")) {
            for (int ring = 0; ring < 3; ring++) {
                double radius = 27 + ring * 5.0;
                double tilt = ring * 0.17;
                double previousX = Math.cos(0) * radius;
                double previousY = 38 + Math.sin(0) * radius * 0.24;
                double previousZ = Math.sin(0) * radius;
                for (int step = 1; step <= 80; step++) {
                    double angle = Math.PI * 2.0 * step / 80.0;
                    double nextX = Math.cos(angle) * radius;
                    double nextY = 38 + Math.sin(angle + tilt) * radius * 0.24;
                    double nextZ = Math.sin(angle) * radius;
                    line(vertices, previousX, previousY, previousZ, nextX, nextY, nextZ,
                            cameraPos, withAlpha(accentColor, intensity * 0.72f));
                    previousX = nextX; previousY = nextY; previousZ = nextZ;
                }
            }
        } else if (style.equals("Aurora")) {
            double phase = mc.world.getTime() * 0.018;
            for (int ribbon = 0; ribbon < 4; ribbon++) {
                double px = -62;
                double py = 49 + ribbon * 2.5 + Math.sin(phase + ribbon) * 2;
                double pz = 36 + ribbon * 3;
                for (int step = 1; step <= 64; step++) {
                    double nx = -62 + step * (124.0 / 64.0);
                    double ny = 49 + ribbon * 2.5 + Math.sin(nx * 0.08 + phase + ribbon * 0.7) * 5.0;
                    double nz = 36 + ribbon * 3 + Math.cos(nx * 0.035 + phase) * 5.0;
                    line(vertices, px, py, pz, nx, ny, nz, cameraPos,
                            withAlpha(starColor, intensity * 0.66f));
                    px = nx; py = ny; pz = nz;
                }
            }
        }
    }

    private static int withAlpha(int argb, float intensity) {
        int alpha = (int) (((argb >>> 24) & 0xFF) * Math.max(0.0f, Math.min(1.0f, intensity)));
        return (alpha << 24) | (argb & 0xFFFFFF);
    }

    private static double hash(int seed, int channel) {
        long n = (long) seed * 374761393L + (long) channel * 668265263L + 1442695040888963407L;
        n = (n ^ (n >>> 13)) * 1274126177L;
        n ^= n >>> 16;
        return (n & 0x7fffffffL) / (double) 0x7fffffffL;
    }

    private static void line(VertexConsumer v, double x1, double y1, double z1,
                             double x2, double y2, double z2, Vec3d camera, int argb) {
        double ax = x1 - camera.x, ay = y1 - camera.y, az = z1 - camera.z;
        double bx = x2 - camera.x, by = y2 - camera.y, bz = z2 - camera.z;
        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0e-6) return;
        float nx = (float) (dx / length), ny = (float) (dy / length), nz = (float) (dz / length);
        int red = (argb >> 16) & 0xFF, green = (argb >> 8) & 0xFF, blue = argb & 0xFF;
        int alpha = (argb >>> 24) & 0xFF;
        vertex(v, ax, ay, az, red, green, blue, alpha, nx, ny, nz);
        vertex(v, bx, by, bz, red, green, blue, alpha, nx, ny, nz);
    }

    private static void vertex(VertexConsumer v, double x, double y, double z,
                               int r, int g, int b, int a, float nx, float ny, float nz) {
        v.vertex((float) x, (float) y, (float) z)
                .color(r, g, b, a)
                .normal(nx, ny, nz)
                .lineWidth(1.5F);
    }
}