package dev.voidclient.freecam;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Independent free camera with acceleration (no player physics mutation). */
public final class FreecamController {
    private FreecamController() {}

    public static double baseSpeed = 20.0;
    public static double sprintMult = 3.0;
    public static double accel = 12.0;
    public static boolean verticalLock = false;
    public static boolean disableOnDamage = true;
    public static boolean blockInteractions = true;
    public static Runnable onAutoStop = () -> {};

    private static boolean active;
    private static LocalPlayer owner;
    private static double x, y, z;
    private static double vx, vy, vz;
    private static float yaw, pitch;
    private static long lastNs;
    private static float lockedYaw, lockedPitch;
    private static boolean bodyLocked;

    public static boolean isActive() { return active; }
    public static boolean blocksInteraction() { return active && blockInteractions; }
    public static Vec3 pos() { return new Vec3(x, y, z); }
    public static float yaw() { return yaw; }
    public static float pitch() { return pitch; }

    public static void start(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (active || p == null || mc.level == null) return;
        Vec3 eye = p.getEyePosition();
        x = eye.x; y = eye.y; z = eye.z;
        vx = vy = vz = 0;
        yaw = p.getYRot();
        pitch = p.getXRot();
        lockedYaw = yaw;
        lockedPitch = pitch;
        bodyLocked = true;
        owner = p;
        lastNs = System.nanoTime();
        active = true;
    }

    public static void stop() {
        active = false;
        owner = null;
        bodyLocked = false;
        vx = vy = vz = 0;
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
        yaw += (float) dx * 0.15f;
        pitch = Mth.clamp(pitch + (float) dy * 0.15f, -90f, 90f);
    }

    public static void captureTouchLook(LocalPlayer p) {
        if (!active || p == null || !bodyLocked) return;
        float py = p.getYRot();
        float pp = p.getXRot();
        float dYaw = py - lockedYaw;
        float dPitch = pp - lockedPitch;
        while (dYaw > 180f) dYaw -= 360f;
        while (dYaw < -180f) dYaw += 360f;
        if (Math.abs(dYaw) > 0.001f || Math.abs(dPitch) > 0.001f) {
            yaw += dYaw;
            pitch = Mth.clamp(pitch + dPitch, -90f, 90f);
        }
        p.setYRot(lockedYaw);
        p.setXRot(lockedPitch);
        p.setYHeadRot(lockedYaw);
    }

    public static void step(Minecraft mc) {
        if (!active) return;
        long now = System.nanoTime();
        double dt = Math.min((now - lastNs) / 1.0e9, 0.05);
        lastNs = now;

        try {
            if (mc.gui != null && mc.gui.screen() != null) return;
        } catch (Throwable ignored) {}

        double f = axis(mc.options.keyUp, mc.options.keyDown);
        double r = axis(mc.options.keyRight, mc.options.keyLeft);
        double u = verticalLock ? 0 : axis(mc.options.keyJump, mc.options.keyShift);

        double yr = Math.toRadians(yaw);
        double pr = verticalLock ? 0 : Math.toRadians(pitch);
        double fx = -Math.sin(yr) * Math.cos(pr);
        double fy = verticalLock ? 0 : -Math.sin(pr);
        double fz = Math.cos(yr) * Math.cos(pr);
        double rx = Math.cos(yr);
        double rz = Math.sin(yr);

        double wishX = fx * f + rx * r;
        double wishY = fy * f + u;
        double wishZ = fz * f + rz * r;
        double wishLen = Math.sqrt(wishX * wishX + wishY * wishY + wishZ * wishZ);
        double maxSpeed = baseSpeed * (mc.options.keySprint.isDown() ? sprintMult : 1.0);

        if (wishLen > 1.0e-6) {
            wishX = wishX / wishLen * maxSpeed;
            wishY = wishY / wishLen * maxSpeed;
            wishZ = wishZ / wishLen * maxSpeed;
            double a = Math.min(1.0, accel * dt);
            vx += (wishX - vx) * a;
            vy += (wishY - vy) * a;
            vz += (wishZ - vz) * a;
        } else {
            double damp = Math.exp(-accel * 0.6 * dt);
            vx *= damp;
            vy *= damp;
            vz *= damp;
        }

        x += vx * dt;
        y += vy * dt;
        z += vz * dt;
    }

    private static double axis(KeyMapping plus, KeyMapping minus) {
        return (plus.isDown() ? 1 : 0) - (minus.isDown() ? 1 : 0);
    }
}
