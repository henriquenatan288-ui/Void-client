package dev.voidclient.modules.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Shared camera state for Freelook and Freecam. All positions are client-side only. */
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
    public static Vec3d getPreviousCameraPos() { return previousCameraPos; }
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
        float yaw = (float) deltaX * 0.15F;
        float pitch = (float) deltaY * 0.15F;
        if (freecamActive) {
            cameraYaw += yaw;
            cameraPitch = clamp(cameraPitch + pitch, -90.0F, 90.0F);
        } else if (freelookActive) {
            freelookYaw += yaw;
            freelookPitch = clamp(freelookPitch + pitch, -90.0F, 90.0F);
        }
    }

    public static void moveCamera(double forward, double strafe, double vertical, float speed) {
        if (!freecamActive) return;
        previousCameraPos = cameraPos;
        double yawRad = Math.toRadians(cameraYaw);
        Vec3d forwardVec = new Vec3d(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3d rightVec = new Vec3d(Math.cos(yawRad), 0, Math.sin(yawRad));
        Vec3d move = forwardVec.multiply(forward).add(rightVec.multiply(strafe));
        if (move.lengthSquared() > 1.0) move = move.normalize();
        cameraPos = cameraPos.add(move.x * speed, vertical * speed, move.z * speed);
    }

    public static Vec3d interpolatedPosition(float tickDelta) {
        return previousCameraPos.lerp(cameraPos, Math.max(0.0F, Math.min(1.0F, tickDelta)));
    }

    public static void syncLookToPlayer(ClientPlayerEntity player) {
        if (freecamActive || freelookActive) return;
        freelookYaw = player.getYaw();
        freelookPitch = player.getPitch();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
