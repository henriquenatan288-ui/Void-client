package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.ModeSetting;
import dev.voidclient.core.setting.NumberSetting;
import dev.voidclient.render.SmoothClock;
import dev.voidclient.render.VoidDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Particles: VoidDraw orbit symbols. SmoothClock = no 20Hz teleport. */
public class Particles extends Module {
    private final ModeSetting style = addSetting(new ModeSetting(
            "Style", "Particle style", "Crown",
            "Crown", "Sword", "Totem", "Rod", "Crit", "Enchant", "Mix", "Money"));
    private final NumberSetting distance = addSetting(new NumberSetting("Distance", "Orbit radius", 0.85, 0.4, 2.5, 0.05));
    private final NumberSetting amount = addSetting(new NumberSetting("Amount", "Symbol count", 10, 3, 24, 1));
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Spin speed", 1.0, 0.2, 3.0, 0.1));
    private final NumberSetting height = addSetting(new NumberSetting("Height", "Orbit height", 1.15, 0.2, 2.2, 0.05));
    private final NumberSetting size = addSetting(new NumberSetting("Size", "Particle size", 1.0, 0.5, 2.5, 0.1));
    private final BooleanSetting glow = addSetting(new BooleanSetting("Glow", "Soft glow around sparks", true));
    private final BooleanSetting rainbow = addSetting(new BooleanSetting("Rainbow", "Animated colors", false));
    private final ColorSetting colorA = addSetting(new ColorSetting("Color A", "Primary", 0xFF7B2CFF));
    private final ColorSetting colorB = addSetting(new ColorSetting("Color B", "Secondary", 0xFF00E5FF));

    public Particles() {
        super("Particles", "Orbiting symbols around you.", Category.RENDER);
        VoidDraw.EXTRACTORS.add(this::extract);
    }

    private void extract(VoidDraw.Sink sink, Minecraft mc, float pt) {
        if (!isEnabled() || mc.level == null || mc.player == null) return;
        SmoothClock.frame(mc);
        pt = SmoothClock.pt(pt);
        Player p = mc.player;
        double x = Mth.lerp(pt, p.xo, p.getX());
        double y = Mth.lerp(pt, p.yo, p.getY());
        double z = Mth.lerp(pt, p.zo, p.getZ());
        double t = SmoothClock.seconds();
        double phase = t * 2.8 * speed.get();
        int n = amount.getInt();
        double r = distance.get(), sz = size.get();
        Vec3 cam = mc.gameRenderer.mainCamera().position();
        int ca = rainbow.get() ? hsv(t * 0.2) : (colorA.get() & 0xFFFFFF);
        int cb = rainbow.get() ? hsv(t * 0.2 + 0.5) : (colorB.get() & 0xFFFFFF);
        Ctx c = new Ctx(sink, cam, glow.get(), sz);

        switch (style.get()) {
            case "Crown" -> crown(c, x, y + p.getBbHeight(), z, r, n, phase, t, ca, cb);
            case "Sword" -> sword(c, x, y + height.get(), z, r, n, phase, ca, cb);
            case "Totem" -> totem(c, x, y, z, r, n, phase, t, p.getBbHeight());
            case "Rod" -> rod(c, x, y + 0.3, z, r, n, phase, ca, cb);
            case "Crit" -> orbit(c, x, y + height.get(), z, r, n, phase, t, 0xFFFFFF, 0xFFE066);
            case "Enchant" -> orbit(c, x, y + height.get(), z, r, n, phase, t, 0xB06CFF, 0x6C8CFF);
            case "Mix" -> mix(c, x, y + height.get(), z, r, n, phase, t, ca, cb);
            case "Money" -> money(c, x, y, z, r, n, phase, t, p.getBbHeight());
            default -> orbit(c, x, y + height.get(), z, r, n, phase, t, ca, cb);
        }
    }

    private record Ctx(VoidDraw.Sink s, Vec3 cam, boolean glow, double size) {
        boolean tooClose(double x, double y, double z) { return cam.distanceToSqr(x, y, z) < 0.36; }
    }

    private static void star(Ctx c, double x, double y, double z, double s, int rgb, double a) {
        if (c.tooClose(x, y, z)) return;
        s *= c.size();
        if (c.glow()) cross(c.s(), x, y, z, s * 2.4, (float) (s * 0.9), argb(rgb, a * 0.18));
        cross(c.s(), x, y, z, s, (float) (s * 0.32), argb(rgb, a));
    }

    private static void cross(VoidDraw.Sink s, double x, double y, double z, double r, float w, int col) {
        s.line(new Vec3(x - r, y, z), new Vec3(x + r, y, z), w, false, col, false);
        s.line(new Vec3(x, y - r, z), new Vec3(x, y + r, z), w, false, col, false);
        s.line(new Vec3(x, y, z - r), new Vec3(x, y, z + r), w, false, col, false);
    }

    private static void seg(Ctx c, Vec3 a, Vec3 b, double w, int rgb, double al) {
        if (c.tooClose(a.x, a.y, a.z) && c.tooClose(b.x, b.y, b.z)) return;
        if (c.glow()) c.s().line(a, b, (float) (w * 3.2 * c.size()), false, argb(rgb, al * 0.16), false);
        c.s().line(a, b, (float) (w * c.size()), false, argb(rgb, al), false);
    }

    private void crown(Ctx c, double x, double top, double z, double r, int n, double ph, double t, int ca, int cb) {
        double cy = top + 0.08, cr = 0.3;
        int spikes = 7;
        Vec3 prev = null;
        for (int i = 0; i <= 28; i++) {
            double a = ph * 0.3 + i * Math.PI * 2 / 28;
            Vec3 pnt = new Vec3(x + Math.cos(a) * cr, cy, z + Math.sin(a) * cr);
            if (prev != null) seg(c, prev, pnt, 0.035, mix(ca, cb, (Math.cos(a) + 1) / 2), 0.95);
            prev = pnt;
        }
        for (int k = 0; k < spikes; k++) {
            double a = ph * 0.3 + k * Math.PI * 2 / spikes;
            double bx = x + Math.cos(a) * cr, bz = z + Math.sin(a) * cr, hh = (k % 2 == 0) ? 0.3 : 0.2;
            seg(c, new Vec3(bx, cy, bz), new Vec3(bx, cy + hh, bz), 0.04, mix(ca, cb, k / (double) spikes), 0.95);
            star(c, bx, cy + hh + 0.03, bz, 0.05, 0xFFF2A8, 0.7 + 0.3 * Math.sin(t * 6 + k));
        }
        for (int i = 0; i < n; i++) {
            double a = ph + i * Math.PI * 2 / n;
            star(c, x + Math.cos(a) * r, cy + 0.15 + Math.sin(a * 2 + ph) * 0.1, z + Math.sin(a) * r, 0.045,
                    mix(ca, cb, i / (double) n), 0.85);
        }
    }

    private void sword(Ctx c, double x, double y, double z, double r, int n, double ph, int ca, int cb) {
        for (int i = 0; i < n; i++) {
            double a = ph + i * Math.PI * 2 / n;
            double h = y - 0.4 + (i % 5) * 0.2;
            double bx = x + Math.cos(a) * r, bz = z + Math.sin(a) * r;
            double tx = -Math.sin(a), tz = Math.cos(a), rx = Math.cos(a), rz = Math.sin(a);
            Vec3 base = new Vec3(bx, h, bz);
            Vec3 tip = new Vec3(bx + tx * 0.28 + rx * 0.04, h + 0.42, bz + tz * 0.28 + rz * 0.04);
            seg(c, base, tip, 0.05, mix(0xFFFFFF, cb, 0.35), 0.95);
            Vec3 mid = base.add(tip.subtract(base).scale(0.12));
            seg(c, mid.add(rx * 0.07, 0, rz * 0.07), mid.subtract(rx * 0.07, 0, rz * 0.07), 0.04, ca, 0.95);
            star(c, base.x, base.y - 0.03, base.z, 0.03, ca, 0.9);
        }
    }

    private void totem(Ctx c, double x, double y, double z, double r, int n, double ph, double t, double bbH) {
        for (int i = 0; i < n * 2; i++) {
            double f = frac(t * 0.55 + i / (double) (n * 2));
            double a = ph + i * 2.4 + f * 5;
            double rr = r * (0.55 + 0.45 * Math.sin(f * Math.PI));
            double al = Math.min(1, Math.min(f * 6, (1 - f) * 4));
            star(c, x + Math.cos(a) * rr, y + 0.1 + f * (bbH + 0.5), z + Math.sin(a) * rr, 0.05,
                    mix(0x2ECC71, 0xF1C40F, h(i, 1)), al);
        }
    }

    private void rod(Ctx c, double x, double y, double z, double r, int n, double ph, int ca, int cb) {
        for (int i = 0; i < n; i++) {
            double a = ph + i * Math.PI * 2 / n, bx = x + Math.cos(a) * r, bz = z + Math.sin(a) * r;
            double by = y + 0.5 + 0.2 * Math.sin(ph + i), len = 0.3 + 0.1 * (i % 3);
            seg(c, new Vec3(bx, by, bz), new Vec3(bx, by + len, bz), 0.035, mix(ca, cb, i / (double) n), 0.9);
            star(c, bx, by + len + 0.02, bz, 0.04, 0xFFFFFF, 0.9);
        }
    }

    private void orbit(Ctx c, double x, double y, double z, double r, int n, double ph, double t, int ca, int cb) {
        for (int i = 0; i < n; i++) {
            double a = ph + i * Math.PI * 2 / n;
            double tw = 0.5 + 0.5 * Math.sin(t * 7 + i * 1.7);
            star(c, x + Math.cos(a) * r, y + Math.sin(a * 2 + ph) * 0.14, z + Math.sin(a) * r, 0.045 + 0.02 * tw,
                    mix(ca, cb, i / (double) n), 0.5 + 0.5 * tw);
        }
    }

    private void mix(Ctx c, double x, double y, double z, double r, int n, double ph, double t, int ca, int cb) {
        for (int i = 0; i < n; i++) {
            int rgb = switch (i % 3) { case 0 -> ca; case 1 -> 0xFFE066; default -> cb; };
            double a = ph + i * Math.PI * 2 / n;
            star(c, x + Math.cos(a) * r, y + Math.sin(a * 2 + ph + i) * 0.2, z + Math.sin(a) * r, 0.05, rgb, 0.9);
        }
    }

    private void money(Ctx c, double x, double y, double z, double r, int n, double ph, double t, double bbH) {
        int bills = n * 2;
        for (int i = 0; i < bills; i++) {
            double f = frac(t * 0.28 + i / (double) bills);
            double a = ph * 0.45 + i * 2.39996;
            double rr = r * (0.7 + 0.7 * h(i, 2)) + 0.1 * Math.sin(t * 2 + i);
            double px = x + Math.cos(a) * rr, pz = z + Math.sin(a) * rr;
            double py = y + (1 - f) * (bbH + 1.4) + 0.05;
            double al = Math.min(1, Math.min(f * 8, (1 - f) * 8));
            double o = t * (2.5 + h(i, 3) * 2) + i;
            Vec3 d = new Vec3(Math.cos(o) * 0.15, Math.sin(o * 0.8) * 0.1, Math.sin(o) * 0.15);
            Vec3 pa = new Vec3(px, py, pz).subtract(d), pb = new Vec3(px, py, pz).add(d);
            if (c.tooClose(px, py, pz)) continue;
            c.s().line(pa, pb, (float) (0.14 * c.size()), false, argb(0x2E8B47, al), false);
            c.s().line(pa.add(d.scale(0.25)), pb.subtract(d.scale(0.25)), (float) (0.08 * c.size()),
                    false, argb(0x8FE3A0, al), false);
        }
        for (int i = 0; i < n; i++) {
            double a = -ph * 0.9 + i * Math.PI * 2 / n;
            double px = x + Math.cos(a) * (r * 0.55), pz = z + Math.sin(a) * (r * 0.55);
            double py = y + 0.3 + (i % 4) * 0.32 + 0.06 * Math.sin(t * 4 + i);
            if (c.tooClose(px, py, pz)) continue;
            double s = 0.035 * c.size();
            c.s().box(px - s, py - s, pz - s, px + s, py + s, pz + s, argb(0xFFC107, 0.95), false);
            star(c, px, py, pz, 0.04, 0xFFF2A8, 0.5 + 0.5 * Math.sin(t * 8 + i));
        }
    }

    private static double frac(double v) { return v - Math.floor(v); }

    private static double h(int i, int ch) {
        long x = (i * 0x9E3779B97F4A7C15L) ^ (ch * 0xBF58476D1CE4E5B9L);
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 27; x *= 0x94D049BB133111EBL; x ^= x >>> 31;
        return (x >>> 11) * (1.0 / (1L << 53));
    }

    private static int argb(int rgb, double a) {
        a = Math.max(0, Math.min(1, a));
        return ((int) Math.round(a * 255) << 24) | (rgb & 0xFFFFFF);
    }

    private static int mix(int c1, int c2, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static int hsv(double phase) {
        double hh = ((phase % 1) + 1) % 1 * 6;
        int i = (int) hh;
        double f = hh - i;
        int v = 255, q = (int) (255 * (1 - f)), tt = (int) (255 * f);
        return switch (i) {
            case 0 -> (v << 16) | (tt << 8);
            case 1 -> (q << 16) | (v << 8);
            case 2 -> (v << 8) | tt;
            case 3 -> (q << 8) | v;
            case 4 -> (tt << 16) | v;
            default -> (v << 16) | q;
        };
    }
}
