package dev.voidclient.modules.visual;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.ColorSetting;
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

/** Player visuals: halo, jump circles, trail. Drawn via VoidDraw ribbons. */
public class Visuals extends Module {
    private final BooleanSetting halo = addSetting(new BooleanSetting("Halo", "Ring above head", true));
    private final BooleanSetting trail = addSetting(new BooleanSetting("Trail", "Ribbon behind you", false));
    private final BooleanSetting jumpCircles = addSetting(new BooleanSetting("JumpCircles", "Ring on landing", true));
    private final BooleanSetting others = addSetting(new BooleanSetting("Others", "Also other players", false));
    private final BooleanSetting rainbow = addSetting(new BooleanSetting("Rainbow", "Animated colors", false));
    private final ColorSetting colorA = addSetting(new ColorSetting("Color A", "Primary", 0xFF7B2CFF));
    private final ColorSetting colorB = addSetting(new ColorSetting("Color B", "Secondary", 0xFF00E5FF));
    private final NumberSetting opacity = addSetting(new NumberSetting("Opacity", "Alpha", 0.8, 0.1, 1.0, 0.05));
    private final NumberSetting size = addSetting(new NumberSetting("Size", "Effect size", 1.4, 0.6, 3.0, 0.1));

    private static final class PState {
        double x, y, z, peak;
        boolean init, ground, jumped;
        int air;
        final ArrayDeque<double[]> trail = new ArrayDeque<>();
    }

    private record Fx(int kind, double x, double y, double z, long born, int dur) {}

    private final Map<Integer, PState> states = new HashMap<>();
    private final List<Fx> fx = new ArrayList<>();

    public Visuals() {
        super("Visuals", "Halo, trails, jump circles.", Category.RENDER);
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
            double dy = y - s.y;
            boolean continuous = s.init && Math.hypot(x - s.x, z - s.z) < 3;

            if (continuous && !ground && s.ground && dy > 0.05 && dy < 2) s.jumped = true;
            if (continuous && !ground) s.peak = Math.max(s.peak, y);
            if (ground && !s.ground) {
                if (continuous && jumpCircles.get() && s.jumped && s.air >= 2) {
                    fx.add(new Fx(0, x, y, z, now, 18));
                }
                s.jumped = false;
            }
            if (ground) {
                s.air = 0;
                s.peak = y;
            } else {
                s.air++;
            }

            if (trail.get()) {
                double moved = Math.hypot(x - s.x, z - s.z);
                if (s.trail.isEmpty() || moved > 0.02) s.trail.addLast(new double[]{x, y, z, now});
                while (s.trail.size() > 30) s.trail.removeFirst();
            } else {
                s.trail.clear();
            }

            s.x = x;
            s.y = y;
            s.z = z;
            s.init = true;
            s.ground = ground;
        }
        states.keySet().retainAll(seen);
        while (fx.size() > 64) fx.remove(0);
    }

    private void extract(VoidDraw.Sink sink, Minecraft mc) {
        if (!isEnabled() || mc.level == null || mc.player == null) return;
        float pt;
        try {
            pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        } catch (Throwable t) {
            pt = 0f;
        }
        long gt = mc.level.getGameTime();
        double sec = (gt + pt) / 20.0;
        double alpha = opacity.get();
        int ca = rainbow.get() ? hsv(sec * 0.2) : (colorA.get() & 0xFFFFFF);
        int cb = rainbow.get() ? hsv(sec * 0.2 + 0.5) : (colorB.get() & 0xFFFFFF);

        for (AbstractClientPlayer p : mc.level.players()) {
            boolean local = p == mc.player;
            if (!local && !others.get()) continue;
            if (mc.player.distanceToSqr(p) > 48 * 48 || !p.isAlive()) continue;

            double x = Mth.lerp(pt, p.xo, p.getX());
            double y = Mth.lerp(pt, p.yo, p.getY());
            double z = Mth.lerp(pt, p.zo, p.getZ());
            double h = p.getBbHeight();

            if (halo.get()) {
                ring(sink, x, y + h + 0.25 + 0.03 * Math.sin(sec * 2), z, 0.3, 0.045f, 32, sec * 0.6, ca, cb, alpha);
            }
            PState s = states.get(p.getId());
            if (trail.get() && s != null) drawTrail(sink, s, gt, ca, cb, alpha);
        }

        for (Fx e : fx) {
            double age = (gt - e.born()) + pt;
            double t = clamp(age / e.dur(), 0, 1);
            double a = fade(t) * alpha;
            double eo = 1 - Math.pow(1 - t, 3);
            double r = (0.12 + 0.88 * eo) * size.get();
            ring(sink, e.x(), e.y() + 0.03, e.z(), r, 0.06f, 36, 0, ca, cb, a);
        }
    }

    private static void ring(VoidDraw.Sink s, double cx, double cy, double cz, double r, float w,
                             int seg, double spin, int ca, int cb, double alpha) {
        Vec3 prev = null;
        for (int i = 0; i <= seg; i++) {
            double ang = spin + i * Math.PI * 2 / seg;
            Vec3 p = new Vec3(cx + Math.cos(ang) * r, cy, cz + Math.sin(ang) * r);
            if (prev != null) {
                s.line(prev, p, w, false, argb(mix(ca, cb, (Math.cos(ang) + 1) * 0.5), alpha), false);
            }
            prev = p;
        }
    }

    private static void drawTrail(VoidDraw.Sink s, PState st, long gt, int ca, int cb, double alpha) {
        double[] prev = null;
        for (double[] p : st.trail) {
            double age = gt - p[3];
            if (age > 24) {
                prev = null;
                continue;
            }
            if (prev != null) {
                double f = 1 - age / 24.0;
                s.line(new Vec3(prev[0], prev[1] + 0.9, prev[2]), new Vec3(p[0], p[1] + 0.9, p[2]),
                        0.28f, false, argb(mix(ca, cb, 1 - f), alpha * 0.6 * f), false);
            }
            prev = p;
        }
    }

    private static double fade(double t) {
        return clamp(Math.min(t / 0.08, (1 - t) / 0.1), 0, 1);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

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
