package dev.voidclient.modules.visual;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.ModeSetting;
import dev.voidclient.core.setting.NumberSetting;
import dev.voidclient.render.VoidDraw;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class Visuals extends Module {
    private final BooleanSetting halo = addSetting(new BooleanSetting("Halo", "Ring above head", true));
    private final ModeSetting haloStyle = addSetting(new ModeSetting("Halo style", "Halo look", "Ring", "Ring", "Double", "Orbit"));
    private final BooleanSetting chinaHat = addSetting(new BooleanSetting("ChinaHat", "Cone hat", false));
    private final ModeSetting hatStyle = addSetting(new ModeSetting("Hat style", "Hat look", "Cone", "Cone", "Layered"));
    private final BooleanSetting wings = addSetting(new BooleanSetting("Wings", "Wings on your back", false));
    private final ModeSetting wingStyle = addSetting(new ModeSetting("Wing style", "Wing look", "Angel", "Angel", "Dragon"));
    private final BooleanSetting trail = addSetting(new BooleanSetting("Trail", "Ribbon behind you", false));
    private final ModeSetting trailStyle = addSetting(new ModeSetting("Trail style", "Trail look", "Ribbon", "Ribbon", "Sparkle"));
    private final BooleanSetting jumpCircles = addSetting(new BooleanSetting("JumpCircles", "Ring on landing", true));
    private final BooleanSetting footsteps = addSetting(new BooleanSetting("Footsteps", "Footprints", false));
    private final BooleanSetting shockwave = addSetting(new BooleanSetting("Shockwave", "Big fall impact", true));
    private final BooleanSetting others = addSetting(new BooleanSetting("Others", "Also other players", false));
    private final BooleanSetting glow = addSetting(new BooleanSetting("Glow", "Soft glow layer", true));
    private final BooleanSetting rainbow = addSetting(new BooleanSetting("Rainbow", "Animated colors", false));
    private final ColorSetting colorA = addSetting(new ColorSetting("Color A", "Primary", 0xFF7B2CFF));
    private final ColorSetting colorB = addSetting(new ColorSetting("Color B", "Secondary", 0xFF00E5FF));
    private final NumberSetting opacity = addSetting(new NumberSetting("Opacity", "Alpha", 0.8, 0.1, 1.0, 0.05));
    private final NumberSetting thickness = addSetting(new NumberSetting("Thickness", "Line thickness", 1.0, 0.5, 2.5, 0.1));
    private final NumberSetting size = addSetting(new NumberSetting("Size", "Effect size", 1.4, 0.6, 3.0, 0.1));

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

    private VoidDraw.Sink sk;
    private double th, alphaMul;

    public Visuals() {
        super("Visuals", "Halo, wings, trails, jump circles and more.", Category.RENDER);
        VoidDraw.EXTRACTORS.add(this::extract);
    }

    @Override
    protected void onDisable() {
        states.clear();
        fx.clear();
    }

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
            boolean ground = p.onGround();
            boolean excluded = p.isSpectator() || !p.isAlive() || p.isPassenger()
                    || p.isInWater() || p.isInLava() || p.isFallFlying();

            double mx = x - s.x, mz = z - s.z, dy = y - s.y, moved = Math.hypot(mx, mz);
            boolean continuous = s.init && !s.excluded && !excluded && Math.hypot(moved, dy) < 3;

            if (continuous && !ground && s.ground && dy > 0.05 && dy < 2) s.jumped = true;
            if (continuous && !ground) s.peak = Math.max(s.peak, y);
            if (ground && !s.ground) {
                if (continuous && jumpCircles.get() && s.jumped && s.air >= 2) {
                    fx.add(new Fx(JUMP, x, y, z, now, 18, 1, 0, 0, false));
                }
                if (continuous && shockwave.get() && s.air >= 3 && s.peak - y >= 2.5) {
                    double power = Math.max(0.45, Math.min(1.5, (s.peak - y) / 2.0));
                    fx.add(new Fx(SHOCK, x, y, z, now, 16, power, 0, 0, false));
                }
                s.jumped = false;
            }
            if (ground) { s.air = 0; s.peak = y; } else s.air++;

            if (footsteps.get() && continuous && ground && s.ground && moved > 0.002 && moved < 2 && Math.abs(dy) < 0.76) {
                s.dist += moved;
                if (s.dist >= 0.75) {
                    s.dist %= 0.75;
                    s.left = !s.left;
                    fx.add(new Fx(FOOT, x, y, z, now, 120, 1, mx / moved, mz / moved, s.left));
                }
            } else if (!ground) s.dist = 0;

            if (trail.get()) {
                if (s.trail.isEmpty() || moved > 0.02) s.trail.addLast(new double[]{x, y, z, now});
                while (s.trail.size() > 30) s.trail.removeFirst();
            } else s.trail.clear();

            s.x = x; s.y = y; s.z = z; s.init = true; s.ground = ground; s.excluded = excluded;
        }
        states.keySet().retainAll(seen);
        while (fx.size() > 64) fx.remove(0);
    }

    private void extract(VoidDraw.Sink sink, Minecraft mc, float pt) {
        if (!isEnabled() || mc.level == null || mc.player == null) return;
        sk = sink;
        th = thickness.get();
        long gt = mc.level.getGameTime();
        double sec = (gt + pt) / 20.0;
        alphaMul = opacity.get();
        int ca = rainbow.get() ? hsv(sec * 0.2) : (colorA.get() & 0xFFFFFF);
        int cb = rainbow.get() ? hsv(sec * 0.2 + 0.5) : (colorB.get() & 0xFFFFFF);
        Vec3 cam = mc.gameRenderer.mainCamera().position();

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
                if (halo.get()) halo(x, y + h + 0.25, z, sec, ca, cb);
                if (chinaHat.get()) hat(x, y + h, z, sec, ca, cb);
                if (wings.get()) wings(x, y + h * 0.78, z, p.getYRot(), sec, ca, cb);
            }
            PState s = states.get(p.getId());
            if (trail.get() && s != null) trailDraw(s, gt, sec, ca, cb);
        }

        for (Fx e : fx) {
            double age = (gt - e.born()) + pt;
            double t = clamp(age / e.dur(), 0, 1);
            double a = fade(t) * alphaMul;
            double eo = 1 - Math.pow(1 - t, 3);
            switch (e.kind()) {
                case JUMP -> {
                    double r = (0.12 + 0.88 * eo) * size.get();
                    ring(e.x(), e.y() + 0.03, e.z(), r, 0.06, 44, 0, ca, cb, a);
                    ring(e.x(), e.y() + 0.035, e.z(), r * 0.72, 0.03, 36, 1, cb, ca, a * 0.6);
                }
                case SHOCK -> {
                    double r = (0.12 + 0.88 * eo) * (1.5 + 2.5 * e.power());
                    ring(e.x(), e.y() + 0.03, e.z(), r, 0.1, 56, 0, cb, ca, a);
                    ring(e.x(), e.y() + 0.04, e.z(), r * 0.5, 0.06, 40, 1, ca, cb, a * 0.6);
                }
                case FOOT -> {
                    double px = -e.dz() * (e.left() ? -0.12 : 0.12), pz = e.dx() * (e.left() ? -0.12 : 0.12);
                    double fa = clamp((1 - t) * 1.5, 0, 1) * alphaMul * 0.7;
                    seg(new Vec3(e.x() + px - e.dx() * 0.14, e.y() + 0.02, e.z() + pz - e.dz() * 0.14),
                        new Vec3(e.x() + px + e.dx() * 0.14, e.y() + 0.02, e.z() + pz + e.dz() * 0.14),
                        0.1, ca, fa);
                }
                default -> { }
            }
        }
    }

    private void seg(Vec3 a, Vec3 b, double w, int rgb, double al) {
        w *= th;
        if (glow.get()) sk.line(a, b, (float) (w * 3.4), false, argb(rgb, al * 0.15), false);
        sk.line(a, b, (float) w, false, argb(rgb, al), false);
    }

    private void star(double x, double y, double z, double s, int rgb, double al) {
        seg(new Vec3(x - s, y, z), new Vec3(x + s, y, z), s * 0.45, rgb, al);
        seg(new Vec3(x, y - s, z), new Vec3(x, y + s, z), s * 0.45, rgb, al);
        seg(new Vec3(x, y, z - s), new Vec3(x, y, z + s), s * 0.45, rgb, al);
    }

    private void ring(double cx, double cy, double cz, double r, double w, int segs, double spin, int ca, int cb, double al) {
        Vec3 prev = null;
        for (int i = 0; i <= segs; i++) {
            double ang = spin + i * Math.PI * 2 / segs;
            Vec3 p = new Vec3(cx + Math.cos(ang) * r, cy, cz + Math.sin(ang) * r);
            if (prev != null) seg(prev, p, w, mix(ca, cb, (Math.cos(ang) + 1) * 0.5), al);
            prev = p;
        }
    }

    private void halo(double x, double y, double z, double sec, int ca, int cb) {
        double bob = 0.03 * Math.sin(sec * 2);
        switch (haloStyle.get()) {
            case "Double" -> {
                ring(x, y + bob, z, 0.3, 0.04, 40, sec * 0.7, ca, cb, alphaMul);
                ring(x, y + 0.08 + bob, z, 0.24, 0.03, 36, -sec * 0.9, cb, ca, alphaMul * 0.9);
            }
            case "Orbit" -> {
                ring(x, y + bob, z, 0.3, 0.02, 40, 0, ca, cb, alphaMul * 0.45);
                for (int k = 0; k < 6; k++) {
                    double an = sec * 1.8 + k * Math.PI / 3;
                    star(x + Math.cos(an) * 0.3, y + bob, z + Math.sin(an) * 0.3, 0.05, mix(ca, cb, k / 6.0), alphaMul);
                }
            }
            default -> {
                ring(x, y + bob, z, 0.3, 0.045, 44, sec * 0.6, ca, cb, alphaMul);
                ring(x, y + bob, z, 0.23, 0.02, 36, -sec * 0.4, cb, ca, alphaMul * 0.55);
            }
        }
    }

    private void hat(double x, double y, double z, double sec, int ca, int cb) {
        if ("Layered".equals(hatStyle.get())) {
            double[][] layers = {{0.56, 0.20, 0.0}, {0.40, 0.18, 0.12}, {0.25, 0.14, 0.24}};
            for (int l = 0; l < layers.length; l++) {
                double r = layers[l][0], hh = layers[l][1], by = y + layers[l][2] - 0.02;
                ring(x, by, z, r, 0.04, 32, sec * 0.2 * (l % 2 == 0 ? 1 : -1), ca, cb, alphaMul);
                for (int i = 0; i < 12; i++) {
                    double an = sec * 0.2 + i * Math.PI * 2 / 12;
                    seg(new Vec3(x + Math.cos(an) * r, by, z + Math.sin(an) * r),
                        new Vec3(x, by + hh, z), 0.03, mix(ca, cb, i / 12.0), alphaMul * 0.8);
                }
            }
            star(x, y + 0.5, z, 0.06, 0xFFFFFF, alphaMul);
            return;
        }
        double r = 0.55, hh = 0.3;
        Vec3 apex = new Vec3(x, y + hh, z);
        for (int i = 0; i < 24; i++) {
            double an = sec * 0.2 + i * Math.PI * 2 / 24;
            seg(apex, new Vec3(x + Math.cos(an) * r, y - 0.02, z + Math.sin(an) * r), 0.04,
                    mix(ca, cb, (Math.cos(an) + 1) * 0.5), alphaMul);
        }
        ring(x, y - 0.02, z, r, 0.05, 36, sec * 0.2, ca, cb, alphaMul);
    }

    private void wings(double x, double y, double z, float yawDeg, double sec, int ca, int cb) {
        double yaw = Math.toRadians(yawDeg);
        double bx = Math.sin(yaw), bz = -Math.cos(yaw);
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);
        double flap = Math.sin(sec * 4.4) * 0.35;
        boolean dragon = "Dragon".equals(wingStyle.get());
        int count = dragon ? 5 : 7;
        for (int side = -1; side <= 1; side += 2) {
            Vec3 prevTip = null;
            for (int i = 0; i < count; i++) {
                double k = i / (double) (count - 1);
                double thA = dragon ? (0.15 + 1.2 * k + flap * (0.5 + k)) : (-0.3 + 1.5 * k + flap * (0.4 + k));
                double len = dragon ? (0.55 + 0.6 * (1 - Math.abs(k - 0.45))) : (0.35 + 0.8 * (1 - Math.abs(k - 0.35)));
                double lx = side * Math.cos(thA), ly = Math.sin(thA), lz = 0.25;
                double n = Math.sqrt(lx * lx + ly * ly + lz * lz);
                lx /= n; ly /= n; lz /= n;
                Vec3 base = new Vec3(x + rx * side * 0.12 + bx * 0.2, y + 0.1 + 0.06 * k, z + rz * side * 0.12 + bz * 0.2);
                Vec3 tip = new Vec3(base.x + rx * lx * len + bx * lz * len, base.y + ly * len,
                        base.z + rz * lx * len + bz * lz * len);
                int c = mix(ca, cb, k);
                seg(base, tip, dragon ? 0.045 : 0.08, c, alphaMul * (1 - 0.3 * k));
                if (dragon && prevTip != null) seg(prevTip, tip, 0.05, mix(cb, ca, k), alphaMul * 0.6);
                prevTip = tip;
            }
        }
    }

    private void trailDraw(PState st, long gt, double sec, int ca, int cb) {
        boolean spark = "Sparkle".equals(trailStyle.get());
        double[] prev = null;
        int idx = 0;
        for (double[] p : st.trail) {
            double age = gt - p[3];
            idx++;
            if (age > 24) { prev = null; continue; }
            double f = 1 - age / 24.0;
            if (spark) {
                double jx = Math.sin(idx * 12.9) * 0.18, jz = Math.cos(idx * 7.7) * 0.18, jy = 0.2 + (idx % 5) * 0.32;
                star(p[0] + jx, p[1] + jy, p[2] + jz, 0.05 + 0.04 * f, mix(ca, cb, 1 - f), alphaMul * f);
            } else if (prev != null) {
                for (int band = 0; band < 3; band++) {
                    double hy = 0.25 + band * 0.65;
                    seg(new Vec3(prev[0], prev[1] + hy, prev[2]), new Vec3(p[0], p[1] + hy, p[2]),
                            0.14, mix(ca, cb, 1 - f), alphaMul * 0.55 * f);
                }
            }
            prev = p;
        }
    }

    private static double fade(double t) { return clamp(Math.min(t / 0.08, (1 - t) / 0.1), 0, 1); }
    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
    private static int argb(int rgb, double a) {
        return ((int) Math.round(clamp(a, 0, 1) * 255) << 24) | (rgb & 0xFFFFFF);
    }
    private static int mix(int c1, int c2, double t) {
        t = clamp(t, 0, 1);
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (r << 16) | (g << 8) | b;
    }
    private static int hsv(double phase) {
        double hh = ((phase % 1) + 1) % 1 * 6;
        int i = (int) hh;
        double f = hh - i;
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
