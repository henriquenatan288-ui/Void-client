package dev.voidclient.modules.visual;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.NumberSetting;
import dev.voidclient.render.VoidDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Neon ambient effects for the Void Client.
 *
 * Particles use stable per-particle seeds and continuous sinusoidal paths, so
 * they drift smoothly instead of teleporting at wrap boundaries. Their alpha
 * fades in and out over a life cycle; layered translucent quads approximate a
 * soft neon halo without drawing a solid background.
 *
 * Based on the existing Void Client WorldFx implementation.
 */
public class WorldFx extends Module {
    private final BooleanSetting fireflies = addSetting(
            new BooleanSetting("Fireflies", "Neon cyan and lime fireflies", true));
    private final BooleanSetting embers = addSetting(
            new BooleanSetting("Embers", "Warm neon sparks", false));
    private final BooleanSetting leaves = addSetting(
            new BooleanSetting("Leaves", "Soft drifting neon leaves", false));
    private final BooleanSetting ash = addSetting(
            new BooleanSetting("Ash", "Translucent drifting ash", false));
    private final BooleanSetting pollen = addSetting(
            new BooleanSetting("Pollen", "Tiny glowing motes", false));
    private final BooleanSetting wind = addSetting(
            new BooleanSetting("Wind", "Smooth neon wind streaks", false));
    private final BooleanSetting neon = addSetting(
            new BooleanSetting("Neon glow", "Layer translucent glow around particles", true));
    private final NumberSetting count = addSetting(
            new NumberSetting("Count", "Per effect (mobile: 30-60)", 40, 10, 120, 5));
    private final NumberSetting radius = addSetting(
            new NumberSetting("Radius", "Blocks around the camera", 14, 6, 24, 1));
    private final NumberSetting windAngle = addSetting(
            new NumberSetting("Wind angle", "Direction in degrees", 30, 0, 360, 15));
    private final NumberSetting opacity = addSetting(
            new NumberSetting("Opacity", "Maximum particle transparency", 0.72, 0.1, 1.0, 0.05));
    private final NumberSetting motion = addSetting(
            new NumberSetting("Motion", "Smooth movement speed", 0.8, 0.2, 2.0, 0.1));
    private final NumberSetting fadeSpeed = addSetting(
            new NumberSetting("Fade speed", "How quickly particles fade", 1.0, 0.4, 2.0, 0.1));

    private enum Kind { FIREFLY, EMBER, LEAF, ASH, POLLEN }

    public WorldFx() {
        super("WorldFx", "Smooth neon ambient particles around the camera.", Category.RENDER);
        VoidDraw.EXTRACTORS.add(this::extract);
    }

    private void extract(VoidDraw.Sink sink, Minecraft mc, float partialTick) {
        if (!isEnabled() || mc.level == null || mc.player == null) return;

        double time = (mc.level.getGameTime() + partialTick) / 20.0;
        Vec3 cam = mc.gameRenderer.mainCamera().position();
        int n = count.getInt();
        double angle = Math.toRadians(windAngle.get());
        double wx = Math.cos(angle);
        double wz = Math.sin(angle);

        if (fireflies.get()) field(sink, Kind.FIREFLY, n, cam, time, wx, wz);
        if (embers.get()) field(sink, Kind.EMBER, n, cam, time, wx, wz);
        if (leaves.get()) field(sink, Kind.LEAF, n, cam, time, wx, wz);
        if (ash.get()) field(sink, Kind.ASH, n, cam, time, wx, wz);
        if (pollen.get()) field(sink, Kind.POLLEN, n, cam, time, wx, wz);
        if (wind.get()) streaks(sink, Math.max(8, n / 2), cam, time, wx, wz);
    }

    private void field(VoidDraw.Sink sink, Kind kind, int n, Vec3 cam,
                       double time, double wx, double wz) {
        double r = radius.get();
        double baseAlpha = opacity.get();
        double move = motion.get();
        double fadeRate = fadeSpeed.get();

        for (int i = 0; i < n; i++) {
            double seed = h(i, 3) * Math.PI * 2.0;
            double q = time * move * motionScale(kind) + seed;
            double life = fract(time * 0.16 * fadeRate + h(i, 8));
            double lifeAlpha = fadeEnvelope(life);
            double drift = switch (kind) {
                case FIREFLY -> 1.1;
                case EMBER -> 0.75;
                case LEAF -> 1.5;
                case ASH -> 0.55;
                case POLLEN -> 0.8;
            };

            double bx = cam.x + (h(i, 0) * 2.0 - 1.0) * r;
            double by = cam.y - r * 0.28 + h(i, 1) * r * 0.9;
            double bz = cam.z + (h(i, 2) * 2.0 - 1.0) * r;

            double px = bx + Math.sin(q) * drift
                    + Math.sin(q * 0.43 + seed) * drift * 0.35;
            double py = by + Math.sin(q * 0.7 + seed * 1.3) * drift * 0.38;
            double pz = bz + Math.cos(q * 0.82 + seed) * drift
                    + Math.cos(q * 0.31) * drift * 0.25;

            int rgb;
            double particleSize;
            double localAlpha = lifeAlpha;
            switch (kind) {
                case FIREFLY -> {
                    // Alternate cyan, electric blue and acid-lime.
                    rgb = neonColor(i, 0x31F5FF, 0x7C4DFF, 0xB7FF4A);
                    particleSize = 0.055 + h(i, 6) * 0.025;
                    localAlpha *= 0.82 + 0.18 * (0.5 + 0.5 * Math.sin(q * 2.0));
                }
                case EMBER -> {
                    rgb = Visuals.mix(0xFF4D8D, 0xFFB52E, h(i, 4));
                    particleSize = 0.045;
                    py += (0.5 - life) * 1.6;
                }
                case LEAF -> {
                    rgb = neonColor(i, 0x72FF6A, 0x39E6B2, 0xD5FF57);
                    particleSize = 0.07;
                    px += wx * Math.sin(q * 0.5) * 0.45;
                    pz += wz * Math.sin(q * 0.5) * 0.45;
                    py += (life - 0.5) * -1.1;
                }
                case ASH -> {
                    rgb = Visuals.mix(0x8DBBFF, 0xD4C8FF, h(i, 7));
                    particleSize = 0.035;
                    localAlpha *= 0.55;
                    py += (life - 0.5) * -0.6;
                }
                default -> {
                    rgb = Visuals.mix(0x67F7FF, 0xFFF0A8, h(i, 5));
                    particleSize = 0.028;
                    localAlpha *= 0.62;
                }
            }

            double edgeDistance = Math.hypot(px - cam.x, pz - cam.z);
            double edgeAlpha = 1.0 - smoothstep(r * 0.72, r, edgeDistance);
            double alpha = baseAlpha * localAlpha * edgeAlpha;
            if (alpha <= 0.008) continue;

            drawParticle(sink, px, py, pz, particleSize, rgb, alpha);
        }
    }

    private void streaks(VoidDraw.Sink sink, int n, Vec3 cam,
                         double time, double wx, double wz) {
        double r = radius.get();
        double move = motion.get();
        for (int i = 0; i < n; i++) {
            double seed = h(i, 10) * Math.PI * 2.0;
            double q = time * move * 0.55 + seed;
            double life = fract(time * 0.12 * fadeSpeed.get() + h(i, 11));
            double alpha = opacity.get() * 0.48 * fadeEnvelope(life);
            double px = cam.x + (h(i, 0) * 2.0 - 1.0) * r
                    + Math.sin(q) * 1.4;
            double pz = cam.z + (h(i, 2) * 2.0 - 1.0) * r
                    + Math.cos(q * 0.83) * 1.4;
            double py = cam.y - 2.0 + h(i, 1) * 8.0
                    + Math.sin(q * 0.6) * 0.5;
            double len = 0.8 + h(i, 4) * 1.8;
            double edgeAlpha = 1.0 - smoothstep(r * 0.72, r,
                    Math.hypot(px - cam.x, pz - cam.z));
            alpha *= edgeAlpha;
            if (alpha <= 0.008) continue;

            Vec3 a = new Vec3(px, py, pz);
            Vec3 b = new Vec3(px - wx * len, py - Math.sin(q) * 0.08, pz - wz * len);
            int rgb = Visuals.mix(0x31F5FF, 0x8B5CFF, 0.5 + 0.5 * Math.sin(q));
            if (neon.get()) {
                sink.line(a, b, 0.055f, false, Visuals.argb(rgb, alpha * 0.22), false);
            }
            sink.line(a, b, 0.012f, false, Visuals.argb(rgb, alpha), false);
        }
    }

    private void drawParticle(VoidDraw.Sink sink, double x, double y, double z,
                              double size, int rgb, double alpha) {
        if (neon.get()) {
            // Layered, low-alpha quads create a translucent neon halo.
            cube(sink, x, y, z, size * 2.8, Visuals.argb(rgb, alpha * 0.15));
        }
        cube(sink, x, y, z, size, Visuals.argb(rgb, alpha * 0.9));
    }

    private static void cube(VoidDraw.Sink sink, double x, double y, double z,
                             double halfSize, int color) {
        sink.box(x - halfSize, y - halfSize, z - halfSize,
                x + halfSize, y + halfSize, z + halfSize, color, false);
    }

    private static double motionScale(Kind kind) {
        return switch (kind) {
            case FIREFLY -> 0.45;
            case EMBER -> 0.33;
            case LEAF -> 0.24;
            case ASH -> 0.2;
            case POLLEN -> 0.3;
        };
    }

    private static int neonColor(int i, int cyan, int purple, int lime) {
        return switch (i % 3) {
            case 0 -> cyan;
            case 1 -> purple;
            default -> lime;
        };
    }

    private static double fadeEnvelope(double life) {
        // Fade in during the first 18%, stay visible, then fade out before respawn.
        return smoothstep(0.0, 0.18, life) * (1.0 - smoothstep(0.78, 1.0, life));
    }

    private static double smoothstep(double edge0, double edge1, double x) {
        double t = Math.clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private static double fract(double value) {
        return value - Math.floor(value);
    }

    /** Stable hash in the range 0..1 for (particle index, channel). */
    private static double h(int i, int ch) {
        long x = (i * 0x9E3779B97F4A7C15L) ^ (ch * 0xBF58476D1CE4E5B9L);
        x ^= x >>> 30;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 27;
        x *= 0x94D049BB133111EBL;
        x ^= x >>> 31;
        return (x >>> 11) * (1.0 / (1L << 53));
    }
}