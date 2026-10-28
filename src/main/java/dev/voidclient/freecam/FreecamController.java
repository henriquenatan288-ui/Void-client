package dev.voidclient.freecam;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Freecam v2 — once per frame; smooth look; sprint ramp; optional collision. */
public final class FreecamController {
    private FreecamController() {}

    public static double baseSpeed = 20.0;
    public static double sprintMult = 3.0;
    public static double accel = 12.0;
    public static boolean verticalLock = false;
    public static boolean disableOnDamage = true;
    public static boolean blockInteractions = true;
    public static boolean smoothLook = true;
    public static double lookSmoothing = 22.0;
    public static boolean collide = false;
    public static Runnable onAutoStop = () -> {};

    private static boolean active;
    private static LocalPlayer owner;
    private static double x, y, z;
    private static double vx, vy, vz;
    private static float targetYaw, targetPitch;
    private static float viewYaw, viewPitch;
    private static long lastStepNs;
    private static float lockedYaw, lockedPitch;
    private static boolean bodyLocked;
    private static double boost = 1.0;

    public static boolean isActive() { return active; }
    public static boolean blocksInteraction() { return active && blockInteractions; }
    public static Vec3 pos() { return new Vec3(x, y, z); }
    public static float yaw() { return viewYaw; }
    public static float pitch() { return viewPitch; }

    public static void start(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (active || p == null || mc.level == null) return;
        Vec3 eye = p.getEyePosition();
        x = eye.x; y = eye.y; z = eye.z;
        vx = vy = vz = 0;
        targetYaw = viewYaw = p.getYRot();
        targetPitch = viewPitch = p.getXRot();
        lockedYaw = targetYaw;
        lockedPitch = targetPitch;
        bodyLocked = true;
        boost = 1.0;
        owner = p;
        lastStepNs = System.nanoTime();
        active = true;
    }

    public static void stop() {
        active = false;
        owner = null;
        bodyLocked = false;
        vx = vy = vz = 0;
        boost = 1.0;
    }

    public static void tick(Minecraft mc) {
        if (!active) return;
        LocalPlayer p = mc.player;
        boolean invalid = p == null || mc.level == null || p != owner || !p.isAlive();
        boolean hurt = disableOnDamage && p != null && p.hurtTime > 0;
        if (invalid || hurt) {
            stop();
            try { onAutoStop.run(); } catch (Throwable ignored) {}
            return;
        }
        captureTouchLook(p);
    }

    public static void look(double dx, double dy) {
        if (!active) return;
        float sens = 0.15f;
        try {
            double s = Minecraft.getInstance().options.sensitivity().get();
            sens = (float) (0.15 * (s * 0.6 + 0.2));
        } catch (Throwable ignored) {}
        targetYaw += (float) dx * sens;
        targetPitch = Mth.clamp(targetPitch + (float) dy * sens, -90f, 90f);
        targetYaw = wrap(targetYaw);
    }

    public static void captureTouchLook(LocalPlayer p) {
        if (!active || p == null || !bodyLocked) return;
        float dYaw = wrap(p.getYRot() - lockedYaw);
        float dPitch = p.getXRot() - lockedPitch;
        if (Math.abs(dYaw) > 0.05f || Math.abs(dPitch) > 0.05f) {
            targetYaw = wrap(targetYaw + dYaw);
            targetPitch = Mth.clamp(targetPitch + dPitch, -90f, 90f);
        }
        p.setYRot(lockedYaw);
        p.setXRot(lockedPitch);
        p.setYHeadRot(lockedYaw);
    }

    public static void step(Minecraft mc) {
        if (!active) return;

        long now = System.nanoTime();
        double dt = (now - lastStepNs) / 1.0e9;
        if (dt < 1.0e-4) return;
        lastStepNs = now;
        if (dt > 0.1) dt = 0.1;

        if (smoothLook) {
            double a = 1.0 - Math.exp(-Math.max(4.0, lookSmoothing) * dt);
            viewYaw = wrap(viewYaw + (float) (wrap(targetYaw - viewYaw) * a));
            viewPitch += (float) ((targetPitch - viewPitch) * a);
        } else {
            viewYaw = targetYaw;
            viewPitch = targetPitch;
        }

        boolean guiBlocks = false;
        try { guiBlocks = mc.gui != null && mc.gui.screen() != null; } catch (Throwable ignored) {}

        double f = 0, r = 0, u = 0;
        if (!guiBlocks) {
            f = axis(mc.options.keyUp, mc.options.keyDown);
            r = axis(mc.options.keyRight, mc.options.keyLeft);
            u = verticalLock ? 0.0 : axis(mc.options.keyJump, mc.options.keyShift);
        }

        double yr = Math.toRadians(viewYaw);
        double pr = verticalLock ? 0.0 : Math.toRadians(viewPitch);
        double fx = -Math.sin(yr) * Math.cos(pr);
        double fy = verticalLock ? 0.0 : -Math.sin(pr);
        double fz = Math.cos(yr) * Math.cos(pr);
        double rx = Math.cos(yr), rz = Math.sin(yr);

        double wishX = fx * f + rx * r;
        double wishY = fy * f + u;
        double wishZ = fz * f + rz * r;
        double wishLen = Math.sqrt(wishX * wishX + wishY * wishY + wishZ * wishZ);

        boolean sprint = false;
        try { sprint = !guiBlocks && mc.options.keySprint.isDown(); } catch (Throwable ignored) {}
        double boostTarget = sprint ? sprintMult : 1.0;
        boost += (boostTarget - boost) * (1.0 - Math.exp(-8.0 * dt));

        double maxSpeed = baseSpeed * boost;
        double rate = Math.max(1.0, accel);

        if (wishLen > 1.0e-6) {
            wishX = wishX / wishLen * maxSpeed;
            wishY = wishY / wishLen * maxSpeed;
            wishZ = wishZ / wishLen * maxSpeed;
            double a = 1.0 - Math.exp(-rate * dt);
            vx += (wishX - vx) * a;
            vy += (wishY - vy) * a;
            vz += (wishZ - vz) * a;
        } else {
            double damp = Math.exp(-rate * 0.65 * dt);
            vx *= damp; vy *= damp; vz *= damp;
            if (vx * vx + vy * vy + vz * vz < 1.0e-6) vx = vy = vz = 0;
        }

        double spd = Math.sqrt(vx * vx + vy * vy + vz * vz);
        double cap = maxSpeed * 1.25;
        if (spd > cap && spd > 1.0e-6) {
            double s = cap / spd;
            vx *= s; vy *= s; vz *= s;
        }

        double nx = x + vx * dt, ny = y + vy * dt, nz = z + vz * dt;
        if (collide && mc.level != null) {
            if (!free(mc, nx, y, z)) { nx = x; vx = 0; }
            if (!free(mc, nx, ny, z)) { ny = y; vy = 0; }
            if (!free(mc, nx, ny, nz)) { nz = z; vz = 0; }
        }
        x = nx; y = ny; z = nz;
    }

    private static boolean free(Minecraft mc, double px, double py, double pz) {
        try {
            return mc.level.noCollision(new AABB(px - 0.3, py - 0.3, pz - 0.3, px + 0.3, py + 0.3, pz + 0.3));
        } catch (Throwable t) {
            return true;
        }
    }

    private static float wrap(float a) {
        while (a > 180f) a -= 360f;
        while (a < -180f) a += 360f;
        return a;
    }

    private static double axis(KeyMapping plus, KeyMapping minus) {
        return (plus.isDown() ? 1.0 : 0.0) - (minus.isDown() ? 1.0 : 0.0);
    }
}
