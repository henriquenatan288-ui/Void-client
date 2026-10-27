package dev.voidclient.modules.render;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * High-quality freecam + freelook controller.
 * Freecam uses velocity + acceleration + friction for smooth movement.
 * Positions are interpolated with partialTick for zero stutter.
 */
public final class CameraControl {
    private static boolean freelookActive;
    private static float freelookYaw;
    private static float freelookPitch;
    private static boolean freelookPassThrough;

    private static boolean freecamActive;
    private static Vec3d previousCameraPos = Vec3d.ZERO;
    private static Vec3d cameraPos = Vec3d.ZERO;
    private static float cameraYaw;
    private static float cameraPitch;

    // Velocity-based movement (units per tick)
    private static double vx, vy, vz;
    private static final double FRICTION = 0.86;
    private static final double ACCEL = 0.28;
    private static final double MAX_SPEED_MULT = 2.4;

    private CameraControl() {}

    public static boolean isFreelookActive() { return freelookActive; }
    public static boolean isFreecamActive() { return freecamActive; }
    public static boolean isPassThrough() { return freelookActive && freelookPassThrough; }
    public static float getYaw() { return freecamActive ? cameraYaw : freelookYaw; }
    public static float getPitch() { return freecamActive ? cameraPitch : freelookPitch; }
    public static Vec3d getCameraPos() { return cameraPos; }

    public static void startFreelook(ClientPlayerEntity player, boolean passThrough) {
        freelookYaw = player.getYaw();
        freelookPitch = player.getPitch();
        freelookPassThrough = passThrough;
        freelookActive = true;
    }

    public static void stopFreelook() {
        freelookActive = false;
        freelookPassThrough = false;
    }

    public static void setPassThrough(boolean value) {
        freelookPassThrough = value;
    }

    public static void startFreecam(ClientPlayerEntity player) {
        cameraPos = player.getCameraPosVec(1.0F);
        previousCameraPos = cameraPos;
        cameraYaw = player.getYaw();
        cameraPitch = player.getPitch();
        vx = vy = vz = 0.0;
        freecamActive = true;
    }

    public static void stopFreecam() {
        freecamActive = false;
        vx = vy = vz = 0.0;
    }

    public static void updateLook(double deltaX, double deltaY) {
        float yawDelta = (float) deltaX * 0.15F;
        float pitchDelta = (float) deltaY * 0.15F;
        if (freecamActive) {
            cameraYaw += yawDelta;
            cameraPitch = MathHelper.clamp(cameraPitch + pitchDelta, -90.0F, 90.0F);
        } else if (freelookActive) {
            freelookYaw += yawDelta;
            freelookPitch = MathHelper.clamp(freelookPitch + pitchDelta, -90.0F, 90.0F);
        }
    }

    /**
     * Called every client tick while freecam is active.
     * Applies acceleration toward the desired direction and friction.
     */
    public static void moveCamera(double forward, double strafe, double vertical, float speed) {
        if (!freecamActive) return;

        previousCameraPos = cameraPos;

        double yawRad = Math.toRadians(cameraYaw);
        double pitchRad = Math.toRadians(cameraPitch);

        // Look vector (forward)
        double lx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double ly = -Math.sin(pitchRad);
        double lz =  Math.cos(yawRad) * Math.cos(pitchRad);

        // Right vector (strafe)
        double rx =  Math.cos(yawRad);
        double rz =  Math.sin(yawRad);

        // Desired direction (normalized)
        double dx = lx * forward + rx * strafe;
        double dy = ly * forward + vertical;
        double dz = lz * forward + rz * strafe;

        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 1.0e-6) {
            dx /= len;
            dy /= len;
            dz /= len;
        } else {
            dx = dy = dz = 0.0;
        }

        // Accelerate
        double accel = ACCEL * speed;
        vx += dx * accel;
        vy += dy * accel;
        vz += dz * accel;

        // Soft speed limit
        double maxSpeed = speed * MAX_SPEED_MULT;
        double speedSq = vx * vx + vy * vy + vz * vz;
        if (speedSq > maxSpeed * maxSpeed) {
            double scale = maxSpeed / Math.sqrt(speedSq);
            vx *= scale;
            vy *= scale;
            vz *= scale;
        }

        // Apply friction when no input
        if (len < 1.0e-4) {
            vx *= FRICTION;
            vy *= FRICTION;
            vz *= FRICTION;
            if (Math.abs(vx) < 0.001) vx = 0;
            if (Math.abs(vy) < 0.001) vy = 0;
            if (Math.abs(vz) < 0.001) vz = 0;
        }

        cameraPos = cameraPos.add(vx, vy, vz);
    }

    /** Smooth position for the current frame (call from CameraMixin / renderer). */
    public static Vec3d interpolatedPosition(float tickDelta) {
        float t = MathHelper.clamp(tickDelta, 0.0F, 1.0F);
        return previousCameraPos.lerp(cameraPos, t);
    }
}
