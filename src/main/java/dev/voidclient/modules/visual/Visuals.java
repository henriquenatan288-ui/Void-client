package dev.voidclient.modules.visual;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.NumberSetting;
import dev.voidclient.core.setting.ModeSetting;
import dev.voidclient.render.VoidDraw;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Visuais de jogador: Halo, ChinaHat, Wings, Trail, JumpCircles, Footsteps, Shockwave.
 * Desenha pelo VoidDraw.Sink (line/box), sem pipeline novo. Lógica derivada do
 * "Visual Client Community" (c) 2026 Alatic, licença MIT.
 *
 * Compatível com VoidDraw.Extractor de 3 argumentos (PR #7: extract(sink, mc, partialTick)).
 *
 * Caminho: src/main/java/dev/voidclient/modules/visual/Visuals.java
 */
public class Visuals extends Module {
    private final BooleanSetting halo = addSetting(new BooleanSetting("Halo", "Ring above head", true));
    private final BooleanSetting chinaHat = addSetting(new BooleanSetting("ChinaHat", "Cone hat", false));
    private final BooleanSetting wings = addSetting(new BooleanSetting("Wings", "Angel wings", false));
    private final BooleanSetting trail = addSetting(new BooleanSetting("Trail", "Ribbon behind you", false));
    private final BooleanSetting jumpCircles = addSetting(new BooleanSetting("JumpCircles", "Ring on landing", true));
    private final BooleanSetting footsteps = addSetting(new BooleanSetting("Footsteps", "Footprints", false));
    private final BooleanSetting shockwave = addSetting(new BooleanSetting("Shockwave", "Big fall impact", true));
    private final BooleanSetting others = addSetting(new BooleanSetting("Others", "Also other players", false));
    private final BooleanSetting rainbow = addSetting(new BooleanSetting("Rainbow", "Animated colors", false));
    private final ColorSetting colorA = addSetting(new ColorSetting("Color A", "Primary", 0xFF7B2CFF));
    private final ColorSetting colorB = addSetting(new ColorSetting("Color B", "Secondary", 0xFF00E5FF));
    private final NumberSetting opacity = addSetting(new NumberSetting("Opacity", "Alpha", 0.8, 0.1, 1.0, 0.05));
    private final NumberSetting size = addSetting(new NumberSetting("Size", "Effect size", 1.4, 0.6, 3.0, 0.1));

    private final BooleanSetting neonGlow = addSetting(
            new BooleanSetting("Neon glow", "Soft translucent glow around visual effects", true));
    private final BooleanSetting customSky = addSetting(
            new BooleanSetting("Custom sky", "Add a subtle neon starfield to the sky", false));
    private final ModeSetting skyStyle = addSetting(new ModeSetting(
            "Sky style", "Custom sky appearance", "Nebula", "Nebula", "Aurora", "Starfield"));
    private final NumberSetting skyIntensity = addSetting(
            new NumberSetting("Sky intensity", "Strength of sky effects", 0.7, 0.2, 1.0, 0.05));
    private final BooleanSetting screenFilter = addSetting(
            new BooleanSetting("Screen filter", "Apply a subtle color grade while playing", false));
    private final ModeSetting filterStyle = addSetting(new ModeSetting(
            "Filter style", "Screen color tint", "Violet", "Violet", "Cyan", "Amber", "Night"));
    private final NumberSetting filterOpacity = addSetting(
            new NumberSetting("Filter opacity", "Screen tint strength", 0.12, 0.02, 0.35, 0.01));

    private static final int JUMP = 0, SHOCK = 1, FOOT = 2;

    private static final class PState {
        double x, y, z, peak, dist;
        boolean init, ground, jumped, left, excluded;
        int air;
        final ArrayDeque<double[]> trail = new ArrayDeque<>();
    }

    private record Fx(int kind, double x, double y, double z, long born, int dur,
                      double power, double dx, double dz, boolean left) {}

    private final Map<Integer, PState> states = new HashMap<>();
    private final List<Fx> fx = new ArrayList<>();

    public Visuals() {
        super("Visuals", "Halo, wings, trails, jump circles and more.", Category.RENDER);
        VoidDraw.EXTRACTORS.add(this::extract);
    }

    @Override
    protected void onDisable() {
        states.clear();
        fx.clear();
    }

    // ------------------------------------------------------------------ tick (rastreio)
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            states.clear();
            fx.clear();
            return;
        }
        long now = mc.level.getGameTime();
        fx.removeIf(e -> now - e.born() >= e.dur());
        Set<Integer> seen = new HashSet<>();

        for (AbstractClientPlayer p : mc.level.players()) {
            boolean local = p == mc.player;
            if (!local && !others.get()) continue;
            if (mc.player.distanceToSqr(p) > 48 * 48) continue;

            int id = p.getId();
            seen.add(id);
            PState s = states.computeIfAbsent(id, k -> new PState());
            double x = p.getX(), y = p.getY(), z = p.getZ();
            boolean ground = p.onGround();                                   // [CONFIRMAR] se não compilar
            boolean excluded = p.isSpectator() || !p.isAlive() || p.isPassenger()
                    || p.isInWater() || p.isInLava() || p.isFallFlying();

            double mx = x - s.x, mz = z - s.z, dy = y - s.y, moved = Math.hypot(mx, mz);
            boolean continuous = s.init && !s.excluded && !excluded && Math.hypot(moved, dy) < 3;

            // pulo / aterrissagem
            if (continuous && !ground && s.ground && dy > 0.05 && dy < 2) s.jumped = true;
            if (continuous && !ground) s.peak = Math.max(s.peak, y);
            if (ground && !s.ground) {
                if (continuous && jumpCircles.get() && s.jumped && s.air >= 2) {
                    fx.add(new Fx(JUMP, x, y, z, now, 18, 1, 0, 0, false));
                }
                if (continuous && shockwave.get() && s.air >= 3 && s.peak - y >= 2.5) {
                    fx.add(new Fx(SHOCK, x, y, z, now, 16,
                            Math.clamp((s.peak - y) / 2.0, 0.45, 1.5), 0, 0, false));
                }
                s.jumped = false;
            }
            if (ground) { s.air = 0; s.peak = y; } else s.air++;

            // pegadas por distância
            if (footsteps.get() && continuous && ground && s.ground && moved > 0.002 && moved < 2 && Math.abs(dy) < 0.76) {
                s.dist += moved;
                if (s.dist >= 0.75) {
                    s.dist %= 0.75;
                    s.left = !s.left;
                    fx.add(new Fx(FOOT, x, y, z, now, 120, 1, mx / moved, mz / moved, s.left));
                }
            } else if (!ground) s.dist = 0;

            // trilha
            if (trail.get()) {
                if (s.trail.isEmpty() || moved > 0.02) s.trail.addLast(new double[]{x, y, z, now});
                while (s.trail.size() > 30) s.trail.removeFirst();
            } else s.trail.clear();

            s.x = x; s.y = y; s.z = z; s.init = true; s.ground = ground; s.excluded = excluded;
        }
        states.keySet().retainAll(seen);
        while (fx.size() > 64) fx.remove(0);
    }

    // ------------------------------------------------------------------ extração (desenho)
    private void extract(VoidDraw.Sink sink, Minecraft mc, float partialTick) {
        if (!isEnabled() || mc.level == null || mc.player == null) return;
        float pt = partialTick;   // vem do VoidDraw (PR #7)
        long gt = mc.level.getGameTime();
        double sec = (gt + pt) / 20.0;
        double alpha = opacity.get();
        int ca = rainbow.get() ? hsv(sec * 0.2) : (colorA.get() & 0xFFFFFF);
        int cb = rainbow.get() ? hsv(sec * 0.2 + 0.5) : (colorB.get() & 0xFFFFFF);
        Vec3 cam = mc.gameRenderer.mainCamera().position();
        if (customSky.get()) drawCustomSky(sink, cam, sec);

        for (AbstractClientPlayer p : mc.level.players()) {
            boolean local = p == mc.player;
            if (!local && !others.get()) continue;
            if (mc.player.distanceToSqr(p) > 48 * 48 || p.isSpectator() || !p.isAlive()) continue;

            double x = Mth.lerp(pt, p.xo, p.getX());
            double y = Mth.lerp(pt, p.yo, p.getY());
            double z = Mth.lerp(pt, p.zo, p.getZ());
            double h = p.getBbHeight();
            boolean firstPerson = local && cam.distanceToSqr(x, y + p.getEyeHeight(), z) < 0.5;

            if (!firstPerson) {
                if (halo.get()) {
                    ring(sink, x, y + h + 0.25 + 0.03 * Math.sin(sec * 2), z, 0.3, 0.045, 40, sec * 0.6, ca, cb, alpha);
                }
                if (chinaHat.get()) hat(sink, x, y + h, z, sec, ca, cb, alpha);
                if (wings.get()) wings(sink, x, y + h * 0.78, z, p.getYRot(), sec, ca, cb, alpha);
            }
            PState s = states.get(p.getId());
            if (trail.get() && s != null) trail(sink, s, gt, ca, cb, alpha);
        }

        for (Fx e : fx) {
            double age = (gt - e.born()) + pt;
            double t = Math.clamp(age / e.dur(), 0, 1);
            double a = fade(t) * alpha;
            double eo = 1 - Math.pow(1 - t, 3);
            switch (e.kind()) {
                case JUMP -> {
                    double r = (0.12 + 0.88 * eo) * size.get();
                    ring(sink, e.x(), e.y() + 0.03, e.z(), r, 0.06, 40, 0, ca, cb, a);
                }
                case SHOCK -> {
                    double r = (0.12 + 0.88 * eo) * (1.5 + 2.5 * e.power());
                    ring(sink, e.x(), e.y() + 0.03, e.z(), r, 0.09, 56, 0, cb, ca, a);
                    ring(sink, e.x(), e.y() + 0.04, e.z(), r * 0.5, 0.06, 40, 1, ca, cb, a * 0.6);
                }
                case FOOT -> {
                    double px = -e.dz() * (e.left() ? -0.12 : 0.12), pz = e.dx() * (e.left() ? -0.12 : 0.12);
                    double fa = Math.clamp((1 - t) * 1.5, 0, 1) * alpha * 0.7;
                    neonLine(sink,
                            new Vec3(e.x() + px - e.dx() * 0.14, e.y() + 0.02, e.z() + pz - e.dz() * 0.14),
                            new Vec3(e.x() + px + e.dx() * 0.14, e.y() + 0.02, e.z() + pz + e.dz() * 0.14),
                            0.1f, ca, fa);
                }
                default -> { }
            }
        }
    }

    // ------------------------------------------------------------------ formas
    /**
     * Draws a subtle camera-relative starfield. Depth-tested world geometry makes
     * terrain naturally occlude stars, so it decorates the sky instead of the HUD.
     */
    private void drawCustomSky(VoidDraw.Sink sink, Vec3 cam, double sec) {
        int starCount = skyStyle.is("Starfield") ? 72 : 48;
        for (int i = 0; i < starCount; i++) {
            double azimuth = h(i, 0) * Math.PI * 2.0;
            double elevation = 0.08 + h(i, 1) * 0.88;
            double horizontal = Math.sqrt(Math.max(0.0, 1.0 - elevation * elevation));
            double distance = 78.0 + h(i, 2) * 16.0;
            double x = cam.x + Math.cos(azimuth) * horizontal * distance;
            double y = cam.y + elevation * distance;
            double z = cam.z + Math.sin(azimuth) * horizontal * distance;
            double pulse = 0.35 + 0.65 * (0.5 + 0.5 * Math.sin(sec * (0.7 + h(i, 4)) + h(i, 5) * 6.28));
            int rgb = skyColor(i);
            double s = 0.045 + h(i, 6) * 0.07;
            sink.box(x - s, y - s, z - s, x + s, y + s, z + s,
                    argb(rgb, skyIntensity.get() * pulse * 0.72), false);
            if (i % 12 == 0) {
                Vec3 a = new Vec3(x - s * 2.2, y, z);
                Vec3 b = new Vec3(x + s * 2.2, y, z);
                neonLine(sink, a, b, 0.009f, rgb, skyIntensity.get() * pulse * 0.42);
                neonLine(sink, new Vec3(x, y - s * 2.2, z),
                        new Vec3(x, y + s * 2.2, z), 0.009f, rgb,
                        skyIntensity.get() * pulse * 0.42);
            }
        }

        if (skyStyle.is("Nebula")) {
            ring(sink, cam.x, cam.y + 25.0, cam.z, 72.0, 0.018, 56,
                    sec * 0.018, 0x32E9FF, 0x9B54FF, skyIntensity.get() * 0.16);
            ring(sink, cam.x, cam.y + 32.0, cam.z, 88.0, 0.014, 56,
                    -sec * 0.012, 0x8A4DFF, 0x48F6B8, skyIntensity.get() * 0.10);
        } else if (skyStyle.is("Aurora")) {
            for (int band = 0; band < 3; band++) {
                Vec3 previous = null;
                for (int j = 0; j <= 56; j++) {
                    double angle = j * Math.PI * 2.0 / 56.0;
                    double radius = 64.0 + band * 8.0;
                    double y = cam.y + 27.0 + Math.sin(angle * 2.0 + sec * 0.22 + band) * (2.0 + band);
                    Vec3 point = new Vec3(cam.x + Math.cos(angle) * radius, y,
                            cam.z + Math.sin(angle) * radius);
                    if (previous != null) {
                        int rgb = mix(0x39F2FF, 0xB24CFF, (Math.sin(angle + band) + 1.0) * 0.5);
                        neonLine(sink, previous, point, 0.022f, rgb,
                                skyIntensity.get() * 0.12);
                    }
                    previous = point;
                }
            }
        }
    }

    private int skyColor(int index) {
        if (skyStyle.is("Aurora")) return switch (index % 3) {
            case 0 -> 0x48FFD0;
            case 1 -> 0x48DFFF;
            default -> 0xAD6BFF;
        };
        if (skyStyle.is("Starfield")) return index % 2 == 0 ? 0xC8D7FF : 0xFFFFFF;
        return switch (index % 3) {
            case 0 -> 0x46F1FF;
            case 1 -> 0xA967FF;
            default -> 0x91FFCB;
        };
    }

    /** Called by the HUD hook only while no menu is open. */
    public void renderScreenFilter(GuiGraphicsExtractor ctx) {
        if (!isEnabled() || !screenFilter.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gui.screen() != null) return;

        int rgb = switch (filterStyle.get()) {
            case "Cyan" -> 0x0C8EAA;
            case "Amber" -> 0xA64C16;
            case "Night" -> 0x121C72;
            default -> 0x541BA0;
        };
        int alpha = (int) Math.round(filterOpacity.get() * 255.0);
        int color = (alpha << 24) | (rgb & 0x00FFFFFF);
        ctx.fill(0, 0, mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight(), color);
    }

    private void ring(VoidDraw.Sink s, double cx, double cy, double cz, double r, double w,
                             int seg, double spin, int ca, int cb, double alpha) {
        Vec3 prev = null;
        for (int i = 0; i <= seg; i++) {
            double ang = spin + i * Math.PI * 2 / seg;
            Vec3 p = new Vec3(cx + Math.cos(ang) * r, cy, cz + Math.sin(ang) * r);
            if (prev != null) neonLine(s, prev, p, (float) w,
                    mix(ca, cb, (Math.cos(ang) + 1) * 0.5), alpha);
            prev = p;
        }
    }

    private void hat(VoidDraw.Sink s, double x, double y, double z, double sec, int ca, int cb, double alpha) {
        double r = 0.55, hh = 0.3;
        Vec3 apex = new Vec3(x, y + hh, z);
        int spokes = 20;
        for (int i = 0; i < spokes; i++) {
            double ang = sec * 0.2 + i * Math.PI * 2 / spokes;
            Vec3 rim = new Vec3(x + Math.cos(ang) * r, y - 0.02, z + Math.sin(ang) * r);
            neonLine(s, apex, rim, 0.05f,
                    mix(ca, cb, (Math.cos(ang) + 1) * 0.5), alpha);
        }
        ring(s, x, y - 0.02, z, r, 0.05, 32, sec * 0.2, ca, cb, alpha);
    }

    /** Asas no espaço do mundo: base nas costas, penas em leque; yaw em graus (convenção Minecraft). */
    private void wings(VoidDraw.Sink s, double x, double y, double z, float yawDeg, double sec,
                              int ca, int cb, double alpha) {
        double yaw = Math.toRadians(yawDeg);
        double bx = Math.sin(yaw), bz = -Math.cos(yaw);        // "atrás"
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);       // "direita"
        double flap = Math.sin(sec * 4.4) * 0.35;
        int feathers = 7;
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < feathers; i++) {
                double k = i / (double) (feathers - 1);
                double th = -0.3 + 1.5 * k + flap * (0.4 + k);
                double len = 0.35 + 0.8 * (1 - Math.abs(k - 0.35));
                double lx = side * Math.cos(th), ly = Math.sin(th), lz = 0.25;
                double n = Math.sqrt(lx * lx + ly * ly + lz * lz);
                lx /= n; ly /= n; lz /= n;
                Vec3 base = new Vec3(x + rx * side * 0.12 + bx * 0.2, y + 0.1 + 0.06 * k, z + rz * side * 0.12 + bz * 0.2);
                Vec3 tip = new Vec3(base.x + rx * lx * len + bx * lz * len, base.y + ly * len, base.z + rz * lx * len + bz * lz * len);
                neonLine(s, base, tip, 0.08f, mix(ca, cb, k), alpha * (1 - 0.35 * k));
            }
        }
    }

    private void trail(VoidDraw.Sink s, PState st, long gt, int ca, int cb, double alpha) {
        double[] prev = null;
        for (double[] p : st.trail) {
            double age = gt - p[3];
            if (age > 24) { prev = null; continue; }
            if (prev != null) {
                double f = 1 - age / 24.0;
                neonLine(s, new Vec3(prev[0], prev[1] + 0.9, prev[2]),
                        new Vec3(p[0], p[1] + 0.9, p[2]), 0.28f,
                        mix(ca, cb, 1 - f), alpha * 0.6 * f);
            }
            prev = p;
        }
    }

    private void neonLine(VoidDraw.Sink sink, Vec3 a, Vec3 b, float width,
                          int rgb, double alpha) {
        if (alpha <= 0.001) return;
        if (neonGlow.get()) {
            sink.line(a, b, width * 1.8f, false, argb(rgb, alpha * 0.18), false);
        }
        sink.line(a, b, width, false, argb(rgb, alpha), false);
    }

    // ------------------------------------------------------------------ utilitários
    static double fade(double t) { return Math.clamp(Math.min(t / 0.08, (1 - t) / 0.1), 0, 1); }

    static int argb(int rgb, double a) {
        return ((int) Math.round(Math.clamp(a, 0, 1) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    static int mix(int c1, int c2, double t) {
        t = Math.clamp(t, 0, 1);
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (r << 16) | (g << 8) | b;
    }

    static int hsv(double phase) {
        double h = ((phase % 1) + 1) % 1 * 6;
        int i = (int) h;
        double f = h - i;
        int v = 255, q = (int) (255 * (1 - f)), t = (int) (255 * f);
        return switch (i) {
            case 0 -> (v << 16) | (t << 8);
            case 1 -> (q << 16) | (v << 8);
            case 2 -> (v << 8) | t;
            case 3 -> (q << 8) | v;
            case 4 -> (t << 16) | v;
            default -> (v << 16) | q;
        };
    }
}