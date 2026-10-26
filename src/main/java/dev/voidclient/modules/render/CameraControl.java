package dev.voidclient.modules.render;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Detached camera state for Freecam / Freelook.
 *
 * Mobile (Pojav): touch often writes yaw/pitch straight onto the player instead of
 * calling Entity.turn. We sample player rotation each tick, feed the delta into
 * the virtual camera, then snap the player body back.
 */
public final class CameraControl {
    private static boolean freelookActive;
    private static float freelookYaw;
    private static float freelookPitch;

    private static boolean freecamActive;
    private static Vec3 previousCameraPos = Vec3.ZERO;
    private static Vec3 cameraPos = Vec3.ZERO;
    private static float cameraYaw;
    private static float cameraPitch;

    /** Body rotation frozen while detached look is active. */
    private static float lockedBodyYaw;
    private static float lockedBodyPitch;
    private static boolean bodyLocked;

    private CameraControl() {}

    public static boolean isFreelookActive() { return freelookActive; }
    public static boolean isFreecamActive() { return freecamActive; }
    public static boolean isDetachedLook() { return freecamActive || freelookActive; }

    public static float getYaw() { return freecamActive ? cameraYaw : freelookYaw; }
    public static float getPitch() { return freecamActive ? cameraPitch : freelookPitch; }
    public static Vec3 getCameraPos() { return cameraPos; }

    public static void startFreelook(LocalPlayer player) {
        freelookYaw = player.getYRot();
        freelookPitch = player.getXRot();
        lockBody(player);
        freelookActive = true;
    }

    public static void stopFreelook() {
        freelookActive = false;
        if (!freecamActive) bodyLocked = false;
    }

    public static void startFreecam(LocalPlayer player) {
        cameraPos = player.getEyePosition(1.0F);
        previousCameraPos = cameraPos;
        cameraYaw = player.getYRot();
        cameraPitch = player.getXRot();
        lockBody(player);
        freecamActive = true;
    }

    public static void stopFreecam() {
        freecamActive = false;
        if (!freelookActive) bodyLocked = false;
    }

    private static void lockBody(LocalPlayer player) {
        lockedBodyYaw = player.getYRot();
        lockedBodyPitch = player.getXRot();
        bodyLocked = true;
    }

    /** From EntityLookMixin when turn() is used (desktop / some loaders). */
    public static void updateLook(double deltaX, double deltaY) {
        float yawDelta = (float) deltaX * 0.15F;
        float pitchDelta = (float) deltaY * 0.15F;
        applyLookDelta(yawDelta, pitchDelta);
    }

    private static void applyLookDelta(float yawDelta, float pitchDelta) {
        if (freecamActive) {
            cameraYaw += yawDelta;
            cameraPitch = clamp(cameraPitch + pitchDelta, -90.0F, 90.0F);
        } else if (freelookActive) {
            freelookYaw += yawDelta;
            freelookPitch = clamp(freelookPitch + pitchDelta, -90.0F, 90.0F);
        }
    }

    /**
     * Call every tick while detached. Captures touch-driven player rotation,
     * moves virtual camera, restores body facing.
     */
    public static void tickCapturePlayerLook(LocalPlayer player) {
        if (!isDetachedLook() || player == null || !bodyLocked) return;

        float py = player.getYRot();
        float pp = player.getXRot();
        float dyaw = py - lockedBodyYaw;
        float dpitch = pp - lockedBodyPitch;

        // Normalize yaw delta to [-180, 180]
        while (dyaw > 180f) dyaw -= 360f;
        while (dyaw < -180f) dyaw += 360f;

        if (Math.abs(dyaw) > 0.001f || Math.abs(dpitch) > 0.001f) {
            applyLookDelta(dyaw, dpitch);
        }

        player.setYRot(lockedBodyYaw);
        player.setXRot(lockedBodyPitch);
        player.setYHeadRot(lockedBodyYaw);
        try {
            player.setYBodyRot(lockedBodyYaw);
        } catch (Throwable ignored) {}
    }

    public static void moveCamera(double forward, double strafe, double vertical, float speed) {
        if (!freecamActive) return;
        previousCameraPos = cameraPos;

        double yaw = Math.toRadians(cameraYaw);
        double pitch = Math.toRadians(cameraPitch);
        double lx = -Math.sin(yaw) * Math.cos(pitch);
        double ly = -Math.sin(pitch);
        double lz = Math.cos(yaw) * Math.cos(pitch);
        Vec3 look = new Vec3(lx, ly, lz);
        Vec3 right = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));

        Vec3 movement = look.scale(forward).add(right.scale(strafe)).add(0.0, vertical, 0.0);
        if (movement.lengthSqr() > 1.0e-8) {
            if (movement.lengthSqr() > 1.0) movement = movement.normalize();
            cameraPos = cameraPos.add(movement.scale(speed));
        }
    }

    public static Vec3 interpolatedPosition(float tickDelta) {
        float t = Math.max(0.0F, Math.min(1.0F, tickDelta));
        return previousCameraPos.lerp(cameraPos, t);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
