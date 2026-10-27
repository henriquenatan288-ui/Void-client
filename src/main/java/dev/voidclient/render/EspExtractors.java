package dev.voidclient.render;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Module;
import dev.voidclient.modules.render.BlockESP;
import dev.voidclient.modules.render.BreakWaves;
import dev.voidclient.modules.render.PlayerESP;
import dev.voidclient.modules.render.Tracers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Bridges existing modules into VoidDraw extractors (ribbon rendering). */
public final class EspExtractors {
    private EspExtractors() {}

    public static void register() {
        VoidDraw.EXTRACTORS.add(EspExtractors::blockEsp);
        VoidDraw.EXTRACTORS.add(EspExtractors::playerEsp);
        VoidDraw.EXTRACTORS.add(EspExtractors::tracers);
        VoidDraw.EXTRACTORS.add(EspExtractors::breakWaves);
    }

    private static void blockEsp(VoidDraw.Sink sink, Minecraft mc, float partialTick) {
        Module mod = VoidClient.MODULE_MANAGER == null ? null : VoidClient.MODULE_MANAGER.getByName("BlockESP");
        if (!(mod instanceof BlockESP esp) || !esp.isEnabled()) return;
        int col = esp.getEspColor();
        boolean tracersOn = esp.shouldDrawTracers();
        for (BlockPos p : esp.getFoundBlocks()) {
            double x0 = p.getX(), y0 = p.getY(), z0 = p.getZ();
            double x1 = x0 + 1, y1 = y0 + 1, z1 = z0 + 1;
            outlineBox(sink, x0, y0, z0, x1, y1, z1, 1.5f, col);
            if (tracersOn) {
                sink.tracer(new Vec3(x0 + 0.5, y0 + 0.5, z0 + 0.5), 0.7f, col);
            }
        }
    }

    private static void playerEsp(VoidDraw.Sink sink, Minecraft mc, float partialTick) {
        Module mod = VoidClient.MODULE_MANAGER == null ? null : VoidClient.MODULE_MANAGER.getByName("PlayerESP");
        if (!(mod instanceof PlayerESP esp) || !esp.isEnabled()) return;
        int col = esp.getEspColor();
        for (AbstractClientPlayer other : esp.getTargets()) {
            double x = lerp(other.xo, other.getX(), partialTick);
            double y = lerp(other.yo, other.getY(), partialTick);
            double z = lerp(other.zo, other.getZ(), partialTick);
            float hw = other.getBbWidth() / 2.0f;
            float h = other.getBbHeight();
            outlineBox(sink, x - hw, y, z - hw, x + hw, y + h, z + hw, 1.5f, col);
        }
    }

    private static void tracers(VoidDraw.Sink sink, Minecraft mc, float partialTick) {
        Module mod = VoidClient.MODULE_MANAGER == null ? null : VoidClient.MODULE_MANAGER.getByName("Tracers");
        if (!(mod instanceof Tracers tracers) || !tracers.isEnabled()) return;
        if (mc.player == null) return;
        double px = lerp(mc.player.xo, mc.player.getX(), partialTick);
        double py = lerp(mc.player.yo, mc.player.getY(), partialTick);
        double pz = lerp(mc.player.zo, mc.player.getZ(), partialTick);

        for (AbstractClientPlayer other : tracers.getTargets()) {
            double x = lerp(other.xo, other.getX(), partialTick);
            double y = lerp(other.yo, other.getY(), partialTick);
            double z = lerp(other.zo, other.getZ(), partialTick);
            double dx = x - px;
            double dy = y - py;
            double dz = z - pz;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            int col = tracers.getTracerColor(other, distance);
            sink.tracer(new Vec3(x, y + other.getBbHeight() * 0.5, z), 0.7f, col);
        }
    }

    private static void breakWaves(VoidDraw.Sink sink, Minecraft mc, float partialTick) {
        Module mod = VoidClient.MODULE_MANAGER == null ? null : VoidClient.MODULE_MANAGER.getByName("BreakWaves");
        if (!(mod instanceof BreakWaves waves) || !waves.isEnabled()) return;
        int base = waves.getWaveColor();
        int ringCount = waves.getWaveCount();
        int maxAge = Math.max(1, waves.getMaxAge());
        for (BreakWaves.Wave wave : waves.getActiveWaves()) {
            // wave.age changes on game ticks; interpolate its visual age between ticks.
            float interpolatedAge = Math.max(0.0f, wave.age - 1.0f + partialTick);
            for (int ring = 0; ring < ringCount; ring++) {
                float ageF = interpolatedAge - ring * 2.5f;
                if (ageF < 0 || ageF > maxAge) continue;
                float t = ageF / maxAge;
                float fade = (1.0f - t) * (1.0f - t);
                int baseA = (base >>> 24) & 0xFF;
                int a = Math.max(18, (int) (baseA * fade));
                int col = (a << 24) | (base & 0x00FFFFFF);
                double radius = 0.2 + ageF * 0.32;
                ring(sink, wave.x, wave.y + ring * 0.04, wave.z, radius, 20, col);
            }
        }
    }

    private static void ring(VoidDraw.Sink sink, double cx, double cy, double cz,
                             double radius, int segments, int col) {
        double prevX = cx + radius;
        double prevZ = cz;
        for (int i = 1; i <= segments; i++) {
            double angle = (Math.PI * 2.0) * i / segments;
            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius;
            sink.line(new Vec3(prevX, cy, prevZ), new Vec3(x, cy, z), 1.8f, true, col, true);
            prevX = x;
            prevZ = z;
        }
    }

    private static void outlineBox(VoidDraw.Sink sink,
                                   double x0, double y0, double z0,
                                   double x1, double y1, double z1,
                                   float width, int col) {
        Vec3 a = new Vec3(x0, y0, z0), b = new Vec3(x1, y0, z0);
        Vec3 c = new Vec3(x1, y0, z1), d = new Vec3(x0, y0, z1);
        Vec3 e = new Vec3(x0, y1, z0), f = new Vec3(x1, y1, z0);
        Vec3 g = new Vec3(x1, y1, z1), h = new Vec3(x0, y1, z1);
        Vec3[][] edges = {
                {a, b}, {b, c}, {c, d}, {d, a},
                {e, f}, {f, g}, {g, h}, {h, e},
                {a, e}, {b, f}, {c, g}, {d, h}
        };
        for (Vec3[] ed : edges) {
            sink.line(ed[0], ed[1], width, true, col, true);
        }
    }

    private static double lerp(double a, double b, float t) {
        return Mth.lerp(t, a, b);
    }
}
