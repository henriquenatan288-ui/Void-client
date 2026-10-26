package dev.voidclient.modules.render;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Shared detached camera state for Freecam and Freelook.
 *
 * Certainty: yaw/pitch/pos live here; CameraMixin applies them to net.minecraft.client.Camera.
 * Hypothesis: if look still fails on mobile, touch deltas may not reach Entity.turn — confirm with log in updateLook.
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
        freelookActive = true;
    }

    public static void stopFreelook() {
        freelookActive = false;
    }

    public static void startFreecam(LocalPlayer player) {
        // Eye position = where first-person camera sits
        cameraPos = player.getEyePosition(1.0F);
        previousCameraPos = cameraPos;
        cameraYaw = player.getYRot();
        cameraPitch = player.getXRot();
        freecamActive = true;
    }

    public static void stopFreecam() {
        freecamActive = false;
    }

    /** Called from EntityLookMixin when player turn is cancelled. */
    public static void updateLook(double deltaX, double deltaY) {
        // Vanilla-ish sensitivity; mobile touch often sends larger deltas
        float yawDelta = (float) deltaX * 0.15F;
        float pitchDelta = (float) deltaY * 0.15F;
        if (freecamActive) {
            cameraYaw += yawDelta;
            cameraPitch = clamp(cameraPitch + pitchDelta, -90.0F, 90.0F);
        } else if (freelookActive) {
            freelookYaw += yawDelta;
            freelookPitch = clamp(freelookPitch + pitchDelta, -90.0F, 90.0F);
        }
    }

    public static void moveCamera(double forward, double strafe, double vertical, float speed) {
        if (!freecamActive) return;
        previousCameraPos = cameraPos;

        double yaw = Math.toRadians(cameraYaw);
        double pitch = Math.toRadians(cameraPitch);

        // Look vector from yaw/pitch (MC convention)
        double lx = -Math.sin(yaw) * Math.cos(pitch);
        double ly = -Math.sin(pitch);
        double lz = Math.cos(yaw) * Math.cos(pitch);
        Vec3 look = new Vec3(lx, ly, lz);

        // Horizontal right vector
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
