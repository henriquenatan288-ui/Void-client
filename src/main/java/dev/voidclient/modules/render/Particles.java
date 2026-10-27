package dev.voidclient.modules.render;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.ModeSetting;
import dev.voidclient.core.setting.NumberSetting;
import dev.voidclient.render.VoidDraw;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class Particles extends Module {
    private final ModeSetting style = addSetting(new ModeSetting(
            "Style", "Particle style", "Crown",
            "Crown", "Sword", "Totem", "Rod", "Crit", "Enchant", "Mix", "Money"));
    private final NumberSetting distance = addSetting(
            new NumberSetting("Distance", "Orbit radius", 0.85, 0.4, 2.5, 0.05));
    private final NumberSetting amount = addSetting(
            new NumberSetting("Amount", "Particle count", 10, 3, 24, 1));
    private final NumberSetting speed = addSetting(
            new NumberSetting("Speed", "Animation speed", 1.0, 0.2, 3.0, 0.1));
    private final NumberSetting height = addSetting(
            new NumberSetting("Height", "Orbit height", 1.15, 0.2, 2.2, 0.05));
    private final NumberSetting opacity = addSetting(
            new NumberSetting("Opacity", "Money effect transparency", 0.8, 0.2, 1.0, 0.05));

    private int tick;
    private int spawnOk;
    private int spawnFail;

    public Particles() {
        super("Particles", "Orbiting particles and translucent neon money notes.", Category.RENDER);
        // The custom money style uses the same shared world-render path as Visuals/WorldFx.
        VoidDraw.EXTRACTORS.add(this::extractMoney);
    }

    @Override
    protected void onEnable() {
        tick = 0;
        spawnOk = 0;
        spawnFail = 0;
        VoidClient.LOGGER.info("Particles ON style={}", style.get());
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null || style.is("Money")) return;

        tick++;
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        double radius = distance.get();
        int count = amount.getInt();
        double phase = tick * 0.14 * speed.get();

        switch (style.get()) {
            case "Crown" -> crown(mc, x, y, z, radius, count, phase);
            case "Sword" -> sword(mc, x, y, z, radius, count, phase);
            case "Totem" -> totem(mc, x, y, z, radius, count, phase);
            case "Crit" -> orbit(mc, ParticleTypes.CRIT, x, y + height.get(), z, radius, count, phase);
            case "Enchant" -> orbit(mc, ParticleTypes.ENCHANTED_HIT, x, y + height.get(), z, radius, count, phase);
            case "Mix" -> mix(mc, x, y + height.get(), z, radius, count, phase);
            default -> orbit(mc, ParticleTypes.END_ROD, x, y + height.get(), z, radius, count, phase);
        }

        if (tick % 200 == 0) {
            VoidClient.LOGGER.debug("Particles tick={} ok={} fail={}", tick, spawnOk, spawnFail);
        }
    }

    private void extractMoney(VoidDraw.Sink sink, Minecraft mc, float partialTick) {
        if (!isEnabled() || !style.is("Money") || mc.level == null || mc.player == null) return;

        double time = (mc.level.getGameTime() + partialTick) / 20.0;
        double radius = distance.get();
        int count = amount.getInt();
        Player player = mc.player;

        Camera camera = mc.gameRenderer.mainCamera();
        var direction = camera.forwardVector();
        Vec3 forward = new Vec3(direction.x(), direction.y(), direction.z()).normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0e-5) right = new Vec3(1, 0, 0);
        else right = right.normalize();
        Vec3 up = right.cross(forward).normalize();

        for (int i = 0; i < count; i++) {
            double seed = hash(i, 2);
            double phase = time * speed.get() * 0.42 + i * Math.PI * 2.0 / count;
            double life = fract(time * 0.13 + hash(i, 5));
            double alpha = opacity.get() * fadeEnvelope(life);
            if (alpha <= 0.01) continue;

            double orbit = radius * (0.65 + seed * 0.7);
            double x = player.getX() + Math.cos(phase) * orbit;
            double y = player.getY() + height.get()
                    + Math.sin(phase * 0.72 + seed * Math.PI * 2.0) * 0.28
                    + (life - 0.5) * 0.18;
            double z = player.getZ() + Math.sin(phase) * orbit;
            Vec3 center = new Vec3(x, y, z);

            double halfWidth = 0.22 + hash(i, 8) * 0.08;
            double halfHeight = halfWidth * 0.53;
            int green = VisualsColor.mix(0x3BFF83, 0xA5FF54, hash(i, 9));
            drawBill(sink, center, right, up, halfWidth, halfHeight, green, alpha);
        }
    }

    private void drawBill(VoidDraw.Sink sink, Vec3 center, Vec3 right, Vec3 up,
                          double halfWidth, double halfHeight, int rgb, double alpha) {
        Vec3 tl = center.add(right.scale(-halfWidth)).add(up.scale(halfHeight));
        Vec3 tr = center.add(right.scale(halfWidth)).add(up.scale(halfHeight));
        Vec3 br = center.add(right.scale(halfWidth)).add(up.scale(-halfHeight));
        Vec3 bl = center.add(right.scale(-halfWidth)).add(up.scale(-halfHeight));

        neonLine(sink, tl, tr, rgb, alpha);
        neonLine(sink, tr, br, rgb, alpha);
        neonLine(sink, br, bl, rgb, alpha);
        neonLine(sink, bl, tl, rgb, alpha);

        // Inset frame and diamond mark keep the notes recognizable without an opaque texture.
        Vec3 itl = center.add(right.scale(-halfWidth * 0.72)).add(up.scale(halfHeight * 0.62));
        Vec3 itr = center.add(right.scale(halfWidth * 0.72)).add(up.scale(halfHeight * 0.62));
        Vec3 ibr = center.add(right.scale(halfWidth * 0.72)).add(up.scale(-halfHeight * 0.62));
        Vec3 ibl = center.add(right.scale(-halfWidth * 0.72)).add(up.scale(-halfHeight * 0.62));
        detailLine(sink, itl, itr, rgb, alpha);
        detailLine(sink, itr, ibr, rgb, alpha);
        detailLine(sink, ibr, ibl, rgb, alpha);
        detailLine(sink, ibl, itl, rgb, alpha);

        Vec3 top = center.add(up.scale(halfHeight * 0.45));
        Vec3 rightPoint = center.add(right.scale(halfWidth * 0.22));
        Vec3 bottom = center.add(up.scale(-halfHeight * 0.45));
        Vec3 leftPoint = center.add(right.scale(-halfWidth * 0.22));
        detailLine(sink, top, rightPoint, rgb, alpha * 0.9);
        detailLine(sink, rightPoint, bottom, rgb, alpha * 0.9);
        detailLine(sink, bottom, leftPoint, rgb, alpha * 0.9);
        detailLine(sink, leftPoint, top, rgb, alpha * 0.9);
    }

    private void neonLine(VoidDraw.Sink sink, Vec3 a, Vec3 b, int rgb, double alpha) {
        sink.line(a, b, 0.065f, false, argb(rgb, alpha * 0.11), false);
        sink.line(a, b, 0.034f, false, argb(rgb, alpha * 0.24), false);
        sink.line(a, b, 0.012f, false, argb(rgb, alpha), false);
    }

    private void detailLine(VoidDraw.Sink sink, Vec3 a, Vec3 b, int rgb, double alpha) {
        sink.line(a, b, 0.012f, false, argb(rgb, alpha * 0.78), false);
    }

    private void crown(Minecraft mc, double x, double y, double z,
                       double radius, int count, double phase) {
        double hy = y + Math.max(height.get(), 1.35);
        for (int i = 0; i < count; i++) {
            double angle = phase + i * (Math.PI * 2.0 / count);
            emit(mc, ParticleTypes.END_ROD,
                    x + Math.cos(angle) * radius,
                    hy + Math.sin(angle * 2.0) * 0.1,
                    z + Math.sin(angle) * radius);
        }
    }

    private void sword(Minecraft mc, double x, double y, double z,
                       double radius, int count, double phase) {
        for (int i = 0; i < count; i++) {
            double angle = phase + i * (Math.PI * 2.0 / count);
            double h = y + 0.6 + (i % 5) * 0.18;
            emit(mc, ParticleTypes.CRIT,
                    x + Math.cos(angle) * (radius * 0.7), h,
                    z + Math.sin(angle) * (radius * 0.7));
        }
        emit(mc, ParticleTypes.ENCHANTED_HIT, x, y + 1.1, z);
    }

    private void totem(Minecraft mc, double x, double y, double z,
                       double radius, int count, double phase) {
        for (int i = 0; i < count; i++) {
            double angle = phase + i * (Math.PI * 2.0 / count);
            emit(mc, ParticleTypes.TOTEM_OF_UNDYING,
                    x + Math.cos(angle) * radius,
                    y + 0.4 + (i % 4) * 0.28,
                    z + Math.sin(angle) * radius);
        }
    }

    private void mix(Minecraft mc, double x, double y, double z,
                     double radius, int count, double phase) {
        for (int i = 0; i < count; i++) {
            double angle = phase + i * (Math.PI * 2.0 / count);
            ParticleOptions fx = switch (i % 3) {
                case 0 -> ParticleTypes.END_ROD;
                case 1 -> ParticleTypes.CRIT;
                default -> ParticleTypes.ENCHANTED_HIT;
            };
            emit(mc, fx,
                    x + Math.cos(angle) * radius,
                    y + Math.sin(angle * 2.0 + phase) * 0.12,
                    z + Math.sin(angle) * radius);
        }
    }

    private void orbit(Minecraft mc, ParticleOptions effect, double x, double y, double z,
                       double radius, int count, double phase) {
        for (int i = 0; i < count; i++) {
            double angle = phase + i * (Math.PI * 2.0 / count);
            emit(mc, effect,
                    x + Math.cos(angle) * radius,
                    y + Math.sin(angle * 2.0 + phase) * 0.12,
                    z + Math.sin(angle) * radius);
        }
    }

    private void emit(Minecraft mc, ParticleOptions effect, double x, double y, double z) {
        try {
            mc.level.addParticle(effect, true, true, x, y, z, 0, 0.02, 0);
            spawnOk++;
        } catch (Throwable first) {
            try {
                mc.level.addParticle(effect, x, y, z, 0, 0.02, 0);
                spawnOk++;
            } catch (Throwable second) {
                spawnFail++;
                if (spawnFail < 3) VoidClient.LOGGER.error("Particles spawn failed", second);
            }
        }
    }

    private static double fadeEnvelope(double life) {
        return smoothstep(0.0, 0.18, life) * (1.0 - smoothstep(0.78, 1.0, life));
    }

    private static double smoothstep(double low, double high, double value) {
        double t = Math.clamp((value - low) / (high - low), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private static double fract(double value) {
        return value - Math.floor(value);
    }

    private static double hash(int index, int channel) {
        long x = (index * 0x9E3779B97F4A7C15L) ^ (channel * 0xBF58476D1CE4E5B9L);
        x ^= x >>> 30;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 27;
        x *= 0x94D049BB133111EBL;
        x ^= x >>> 31;
        return (x >>> 11) * (1.0 / (1L << 53));
    }

    private static int argb(int rgb, double alpha) {
        return ((int) Math.round(Math.clamp(alpha, 0.0, 1.0) * 255.0) << 24)
                | (rgb & 0x00FFFFFF);
    }

    private static final class VisualsColor {
        private static int mix(int a, int b, double t) {
            double clamped = Math.clamp(t, 0.0, 1.0);
            int red = (int) Math.round(((a >> 16) & 0xFF) * (1 - clamped) + ((b >> 16) & 0xFF) * clamped);
            int green = (int) Math.round(((a >> 8) & 0xFF) * (1 - clamped) + ((b >> 8) & 0xFF) * clamped);
            int blue = (int) Math.round((a & 0xFF) * (1 - clamped) + (b & 0xFF) * clamped);
            return (red << 16) | (green << 8) | blue;
        }
    }
}
