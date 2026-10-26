package dev.voidclient.freelook;

import dev.voidclient.freecam.FreecamController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * Independent camera yaw/pitch. Player body keeps its own facing.
 * Does not mutate player state. Freecam has priority when both would be active.
 */
public final class FreelookController {
    private FreelookController() {}

    public static float sensitivity = 0.15f;

    /** Fired when freelook stops itself (death, world change). */
    public static Runnable onAutoStop = () -> {};

    private static boolean active;
    private static LocalPlayer owner;
    private static float yaw, pitch;

    /** Locked body rotation so mobile touch that writes onto the player can be redirected. */
    private static float lockedYaw, lockedPitch;
    private static boolean bodyLocked;

    public static boolean isActive() {
        return active && !FreecamController.isActive();
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

    /** Mobile path: touch may write rotation onto the player instead of Entity.turn. */
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
