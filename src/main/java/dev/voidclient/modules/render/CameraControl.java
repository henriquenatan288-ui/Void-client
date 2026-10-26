package dev.voidclient.modules.render;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class CameraControl {
    private static boolean freelookActive;
    private static float freelookYaw;
    private static float freelookPitch;
    private static boolean freecamActive;
    private static Vec3d previousCameraPos = Vec3d.ZERO;
    private static Vec3d cameraPos = Vec3d.ZERO;
    private static float cameraYaw;
    private static float cameraPitch;

    private CameraControl() {}
    public static boolean isFreelookActive() { return freelookActive; }
    public static boolean isFreecamActive() { return freecamActive; }
    public static float getYaw() { return freecamActive ? cameraYaw : freelookYaw; }
    public static float getPitch() { return freecamActive ? cameraPitch : freelookPitch; }
    public static Vec3d getCameraPos() { return cameraPos; }

    public static void startFreelook(ClientPlayerEntity player) {
        freelookYaw = player.getYaw();
        freelookPitch = player.getPitch();
        freelookActive = true;
    }
    public static void stopFreelook() { freelookActive = false; }

    public static void startFreecam(ClientPlayerEntity player) {
        cameraPos = player.getCameraPosVec(1.0F);
        previousCameraPos = cameraPos;
        cameraYaw = player.getYaw();
        cameraPitch = player.getPitch();
        freecamActive = true;
    }
    public static void stopFreecam() { freecamActive = false; }

    public static void updateLook(double deltaX, double deltaY) {
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
        Vec3d look = new Vec3d(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3d right = new Vec3d(Math.cos(yaw), 0.0, Math.sin(yaw));
        Vec3d movement = look.multiply(forward).add(right.multiply(strafe)).add(0.0, vertical, 0.0);
        if (movement.lengthSquared() > 1.0) movement = movement.normalize();
        cameraPos = cameraPos.add(movement.multiply(speed));
    }

    public static Vec3d interpolatedPosition(float tickDelta) {
        return previousCameraPos.lerp(cameraPos, Math.max(0.0F, Math.min(1.0F, tickDelta)));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
