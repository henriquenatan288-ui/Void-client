package dev.voidclient.freelook;

import dev.voidclient.freecam.FreecamController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public final class FreelookController {
    private FreelookController() {}

    public static float sensitivity = 0.15f;
    /** When true, camera ignores wall collision in 3rd person (looks through blocks). */
    public static boolean passThrough = false;
    public static Runnable onAutoStop = () -> {};

    private static boolean active;
    private static LocalPlayer owner;
    private static float yaw, pitch;
    private static float lockedYaw, lockedPitch;
    private static boolean bodyLocked;

    public static boolean isActive() {
        return active && !FreecamController.isActive();
    }

    public static boolean shouldPassThrough() {
        return isActive() && passThrough;
    }

    public static float yaw() { return yaw; }
    public static float pitch() { return pitch; }

    public static void start(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (active || p == null) return;
        if (FreecamController.isActive()) return;

        yaw = p.getYRot();
        pitch = p.getXRot();
        lockedYaw = yaw;
        lockedPitch = pitch;
        bodyLocked = true;
        owner = p;
        active = true;
    }

    public static void stop() {
        active = false;
        owner = null;
        bodyLocked = false;
    }

    public static void tick(Minecraft mc) {
        if (!active) return;
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || p != owner || !p.isAlive() || FreecamController.isActive()) {
            stop();
            try { onAutoStop.run(); } catch (Throwable ignored) {}
            return;
        }
        captureTouchLook(p);
    }

    public static void look(double dx, double dy) {
        if (!isActive()) return;
        yaw += (float) dx * sensitivity;
        pitch = Mth.clamp(pitch + (float) dy * sensitivity, -90f, 90f);
    }

    public static void captureTouchLook(LocalPlayer p) {
        if (!isActive() || p == null || !bodyLocked) return;
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
}
