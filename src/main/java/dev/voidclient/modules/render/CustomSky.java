package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.ModeSetting;
import dev.voidclient.core.setting.NumberSetting;
import dev.voidclient.render.VoidDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public class CustomSky extends Module {
    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "Sky style", "Void",
            "Void", "Aurora", "Nebula", "Stars", "Sunset"));
    private final ColorSetting colorA = addSetting(new ColorSetting("Color A", "Primary", 0xFFFFFFFF));
    private final ColorSetting colorB = addSetting(new ColorSetting("Color B", "Secondary", 0xFF7B2CFF));
    private final NumberSetting radius = addSetting(new NumberSetting("Radius", "Sky distance", 80, 30, 150, 5));
    private final NumberSetting density = addSetting(new NumberSetting("Density", "Amount of curves/bands", 14, 4, 30, 1));
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Animation speed", 1.0, 0.0, 4.0, 0.1));
    private final NumberSetting brightness = addSetting(new NumberSetting("Brightness", "Opacity", 0.8, 0.1, 1.0, 0.05));
    private final BooleanSetting stars = addSetting(new BooleanSetting("Stars", "Twinkling stars", true));
    private final BooleanSetting tint = addSetting(new BooleanSetting("Tint", "Color tint over the sky", false));
    private final NumberSetting tintStrength = addSetting(new NumberSetting("Tint strength", "Tint opacity", 0.15, 0.02, 0.5, 0.01));

    public CustomSky() {
        super("CustomSky", "Customizable sky overlay.", Category.RENDER);
        VoidDraw.EXTRACTORS.add(this::extract);
    }

    private void extract(VoidDraw.Sink s, Minecraft mc, float pt) {
        if (!isEnabled() || mc.level == null || mc.player == null) return;
        double t = (mc.level.getGameTime() + pt) / 20.0 * speed.get();
        Vec3 cam = mc.gameRenderer.mainCamera().position();
        double R = radius.get(), br = brightness.get();
        int ca = colorA.get() & 0xFFFFFF, cb = colorB.get() & 0xFFFFFF, n = density.getInt();

        if (tint.get()) {
            double e = R * 0.95;
            s.box(cam.x - e, cam.y - e, cam.z - e, cam.x + e, cam.y + e, cam.z + e,
                    argb(cb, tintStrength.get()), false);
        }
        switch (mode.get()) {
            case "Void" -> swirls(s, cam, R, t, n, ca, cb, br);
            case "Aurora" -> aurora(s, cam, R, t, n, ca, cb, br);
            case "Nebula" -> nebula(s, cam, R, t, n, ca, cb, br);
            case "Sunset" -> sunset(s, cam, R, ca, cb, br);
            default -> { }
        }
        if (stars.get() || "Stars".equals(mode.get())) stars(s, cam, R, t, ca, br);
    }

    private static Vec3 pt(Vec3 cam, double R, double az, double el) {
        double ce = Math.cos(el);
        return new Vec3(cam.x + Math.cos(az) * ce * R, cam.y + Math.sin(el) * R, cam.z + Math.sin(az) * ce * R);
    }

    private static void line(VoidDraw.Sink s, Vec3 a, Vec3 b, double w, int rgb, double al) {
        s.line(a, b, (float) (w * 3.5), false, argb(rgb, al * 0.14), false);
        s.line(a, b, (float) w, false, argb(rgb, al), false);
    }

    private void swirls(VoidDraw.Sink s, Vec3 cam, double R, double t, int n, int ca, int cb, double br) {
        for (int k = 0; k < n; k++) {
            double az0 = h(k, 0) * Math.PI * 2, el0 = 0.25 + h(k, 1) * 0.85, sweep = 1.3 + h(k, 2) * 1.8;
            Vec3 prev = null;
            int N = 34;
            for (int i = 0; i <= N; i++) {
                double u = i / (double) N;
                double az = az0 + u * sweep + 0.35 * Math.sin(t * 0.25 + k + u * 3);
                double el = el0 + 0.32 * Math.sin(u * 4 + t * 0.3 + k * 1.7);
                el = Math.max(0.06, Math.min(1.45, el));
                Vec3 p = pt(cam, R, az, el);
                if (prev != null) {
                    double fade = Math.pow(Math.sin(Math.PI * u), 1.1);
                    line(s, prev, p, R * 0.0035 * (0.4 + fade), mix(ca, cb, u), br * fade);
                }
                prev = p;
            }
        }
    }

    private void aurora(VoidDraw.Sink s, Vec3 cam, double R, double t, int n, int ca, int cb, double br) {
        int M = n * 4;
        for (int j = 0; j < M; j++) {
            double az = (j / (double) M) * Math.PI * 2;
            double wave = Math.sin(az * 3 + t * 0.4) * 0.5 + Math.sin(az * 7 - t * 0.7) * 0.25;
            double lo = 0.10 + 0.08 * wave;
            double hi = lo + 0.28 + 0.16 * (0.5 + 0.5 * Math.sin(t * 0.6 + j * 0.35));
            Vec3 a = pt(cam, R, az, lo), m = pt(cam, R, az, (lo + hi) * 0.5), b = pt(cam, R, az, hi);
            double w = R * 2 * Math.PI / M * 1.15;
            s.line(a, m, (float) w, false, argb(ca, br * 0.30), false);
            s.line(m, b, (float) w, false, argb(cb, br * 0.16), false);
        }
    }

    private void nebula(VoidDraw.Sink s, Vec3 cam, double R, double t, int n, int ca, int cb, double br) {
        for (int i = 0; i < n * 3; i++) {
            double az = h(i, 0) * Math.PI * 2 + t * 0.01, el = 0.15 + h(i, 1) * 1.1;
            double len = R * (0.08 + h(i, 2) * 0.12), a = h(i, 3) * Math.PI;
            Vec3 c = pt(cam, R, az, el);
            Vec3 d = new Vec3(Math.cos(a) * len, Math.sin(a) * len * 0.6, Math.sin(a) * len);
            double pulse = 0.6 + 0.4 * Math.sin(t * 0.5 + i);
            s.line(c.subtract(d), c.add(d), (float) (R * (0.05 + h(i, 4) * 0.07)), false,
                    argb(mix(ca, cb, h(i, 5)), br * 0.10 * pulse), false);
        }
    }

    private void sunset(VoidDraw.Sink s, Vec3 cam, double R, int ca, int cb, double br) {
        int S = 72;
        double[] el = {0.02, 0.10, 0.19, 0.29};
        double[] al = {0.34, 0.26, 0.16, 0.08};
        for (int band = 0; band < 3; band++) {
            for (int i = 0; i < S; i++) {
                double az = i / (double) S * Math.PI * 2;
                double w = R * 2 * Math.PI / S * 1.12;
                s.line(pt(cam, R, az, el[band]), pt(cam, R, az, el[band + 1]), (float) w, false,
                        argb(mix(ca, cb, band / 3.0), br * al[band]), false);
            }
        }
    }

    private void stars(VoidDraw.Sink s, Vec3 cam, double R, double t, int rgb, double br) {
        for (int i = 0; i < 140; i++) {
            double az = h(i, 10) * Math.PI * 2, el = 0.05 + h(i, 11) * 1.45;
            double tw = 0.35 + 0.65 * Math.pow(Math.sin(t * (0.6 + h(i, 12)) + i), 2);
            Vec3 p = pt(cam, R * 0.98, az, el);
            double sz = R * (h(i, 13) > 0.9 ? 0.0075 : 0.0042);
            s.box(p.x - sz, p.y - sz, p.z - sz, p.x + sz, p.y + sz, p.z + sz,
                    argb(mix(rgb, 0xFFFFFF, h(i, 14)), br * tw), false);
        }
    }

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
}
