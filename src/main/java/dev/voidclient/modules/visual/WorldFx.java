package dev.voidclient.modules.visual;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.NumberSetting;
import dev.voidclient.render.VoidDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Ambiente: Fireflies, Embers, Leaves, Ash, Pollen, Wind streaks.
 * Partículas SEM ESTADO: posição = f(índice, tempo), dobrada numa caixa em volta da câmera.
 * Desenha pelo VoidDraw.Sink (box/line). Lógica derivada do "Visual Client Community" (c) 2026 Alatic, MIT.
 *
 * Compatível com VoidDraw.Extractor de 3 argumentos (PR #7: extract(sink, mc, partialTick)).
 *
 * Caminho: src/main/java/dev/voidclient/modules/visual/WorldFx.java
 */
public class WorldFx extends Module {
    private final BooleanSetting fireflies = addSetting(new BooleanSetting("Fireflies", "Glowing bugs", true));
    private final BooleanSetting embers = addSetting(new BooleanSetting("Embers", "Rising sparks", false));
    private final BooleanSetting leaves = addSetting(new BooleanSetting("Leaves", "Falling leaves", false));
    private final BooleanSetting ash = addSetting(new BooleanSetting("Ash", "Falling ash", false));
    private final BooleanSetting pollen = addSetting(new BooleanSetting("Pollen", "Drifting pollen", false));
    private final BooleanSetting wind = addSetting(new BooleanSetting("Wind", "Wind streaks", false));
    private final NumberSetting count = addSetting(new NumberSetting("Count", "Per effect (phone: 30-60)", 40, 10, 120, 5));
    private final NumberSetting radius = addSetting(new NumberSetting("Radius", "Blocks around camera", 14, 6, 24, 1));
    private final NumberSetting windAngle = addSetting(new NumberSetting("WindAngle", "Degrees", 30, 0, 360, 15));
    private final NumberSetting opacity = addSetting(new NumberSetting("Opacity", "Alpha", 0.85, 0.1, 1.0, 0.05));

    private enum Kind { FIREFLY, EMBER, LEAF, ASH, POLLEN }

    public WorldFx() {
        super("WorldFx", "Ambient particles around you.", Category.RENDER);
        VoidDraw.EXTRACTORS.add(this::extract);
    }

    private void extract(VoidDraw.Sink sink, Minecraft mc, float partialTick) {
        if (!isEnabled() || mc.level == null || mc.player == null) return;
        float pt = partialTick;   // vem do VoidDraw (PR #7)
        double time = (mc.level.getGameTime() + pt) / 20.0;
        Vec3 cam = mc.gameRenderer.mainCamera().position();
        int n = count.getInt();
        double wx = Math.cos(Math.toRadians(windAngle.get())), wz = Math.sin(Math.toRadians(windAngle.get()));

        if (fireflies.get()) field(sink, Kind.FIREFLY, n, cam, time, wx, wz);
        if (embers.get()) field(sink, Kind.EMBER, n, cam, time, wx, wz);
        if (leaves.get()) field(sink, Kind.LEAF, n, cam, time, wx, wz);
        if (ash.get()) field(sink, Kind.ASH, n, cam, time, wx, wz);
        if (pollen.get()) field(sink, Kind.POLLEN, n, cam, time, wx, wz);
        if (wind.get()) streaks(sink, Math.max(10, n / 2), cam, time, wx, wz);
    }

    private void field(VoidDraw.Sink sink, Kind kind, int n, Vec3 cam, double time, double wx, double wz) {
        double R = radius.get(), size = R * 2, alpha = opacity.get();
        for (int i = 0; i < n; i++) {
            double ph = h(i, 3) * 6.283;
            double vx, vy, vz, sz;
            int rgb;
            double a = alpha;
            switch (kind) {
                case FIREFLY -> {
                    vx = Math.sin(time * 0.5 + ph) * 0.4; vy = Math.cos(time * 0.4 + ph) * 0.2; vz = Math.cos(time * 0.45 + ph) * 0.4;
                    double pulse = Math.sin(time * 2 + ph * 3);
                    a *= 0.35 + 0.65 * pulse * pulse;
                    rgb = 0xCCFF55; sz = 0.06;
                }
                case EMBER -> { vx = Math.sin(time + ph) * 0.3; vz = Math.cos(time + ph) * 0.3; vy = 0.9 + h(i, 5); rgb = Visuals.mix(0xFF4500, 0xFFC107, h(i, 6)); sz = 0.045; }
                case LEAF -> { vx = wx * 0.6 + Math.sin(time * 1.5 + ph) * 0.5; vz = wz * 0.6 + Math.cos(time * 1.3 + ph) * 0.5; vy = -0.9; rgb = Visuals.mix(0x4CAF50, 0xE67E22, h(i, 4)); sz = 0.08; }
                case ASH -> { vx = wx * 0.3; vz = wz * 0.3; vy = -0.35; rgb = Visuals.mix(0x777777, 0xBBBBBB, h(i, 7)); sz = 0.04; a *= 0.8; }
                default -> { vx = wx * 0.25; vz = wz * 0.25; vy = Math.sin(time + ph) * 0.08; rgb = 0xFFF2A8; sz = 0.03; a *= 0.7; }
            }
            double px = wrap(h(i, 0) * size + vx * time - (cam.x - R), size) + cam.x - R;
            double py = wrap(h(i, 1) * size * 0.6 + vy * time - (cam.y - R * 0.3), size * 0.6) + cam.y - R * 0.3;
            double pz = wrap(h(i, 2) * size + vz * time - (cam.z - R), size) + cam.z - R;
            double edge = Math.clamp(1 - Math.hypot(px - cam.x, pz - cam.z) / R, 0, 1);      // some na borda
            sink.box(px - sz, py - sz, pz - sz, px + sz, py + sz, pz + sz,
                    Visuals.argb(rgb, a * Math.min(1, edge * 3)), false);
        }
    }

    private void streaks(VoidDraw.Sink sink, int n, Vec3 cam, double time, double wx, double wz) {
        double R = radius.get(), size = R * 2, travel = time * 7;
        for (int i = 0; i < n; i++) {
            double px = wrap(h(i, 0) * size + wx * travel - (cam.x - R), size) + cam.x - R;
            double pz = wrap(h(i, 2) * size + wz * travel - (cam.z - R), size) + cam.z - R;
            double py = cam.y - 2 + h(i, 1) * 8, len = 1.2 + h(i, 3) * 1.5;
            double edge = Math.clamp(1 - Math.hypot(px - cam.x, pz - cam.z) / R, 0, 1);
            sink.line(new Vec3(px, py, pz), new Vec3(px - wx * len, py, pz - wz * len),
                    0.02f, false, Visuals.argb(0xFFFFFF, opacity.get() * 0.35 * edge), false);
        }
    }

    /** hash determinístico 0..1 por (índice, canal) */
    private static double h(int i, int ch) {
        long x = (i * 0x9E3779B97F4A7C15L) ^ (ch * 0xBF58476D1CE4E5B9L);
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 27; x *= 0x94D049BB133111EBL; x ^= x >>> 31;
        return (x >>> 11) * (1.0 / (1L << 53));
    }

    private static double wrap(double v, double size) {
        double m = v % size;
        return m < 0 ? m + size : m;
    }
}