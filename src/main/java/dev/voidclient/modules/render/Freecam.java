package dev.voidclient.modules.render;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Detached free camera (26.2 Mojang mappings).
 * Touch look: CameraControl.tickCapturePlayerLook samples player rot delta.
 */
public class Freecam extends Module {
    private final NumberSetting speed = addSetting(
            new NumberSetting("Speed", "Free camera speed", 0.8, 0.2, 3.0, 0.1));

    private CameraType previousPerspective;
    private LocalPlayer capturedPlayer;
    private ClientLevel capturedWorld;
    private Vec3 previousPosition = Vec3.ZERO;
    private Vec3 previousVelocity = Vec3.ZERO;
    private float previousYaw;
    private float previousPitch;
    private boolean previousNoGravity;
    private boolean previousOnGround;
    private boolean playerStateCaptured;

    public Freecam() {
        super("Freecam", "Move a detached camera through blocks.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            setEnabled(false);
            return;
        }

        if (CameraControl.isFreelookActive()) {
            Module fl = VoidClient.MODULE_MANAGER != null
                    ? VoidClient.MODULE_MANAGER.getByName("Freelook") : null;
            if (fl != null && fl.isEnabled()) fl.disable();
        }

        previousPerspective = mc.options.getCameraType();
        capturedPlayer = player;
        capturedWorld = mc.level;
        previousPosition = new Vec3(player.getX(), player.getY(), player.getZ());
        previousVelocity = player.getDeltaMovement();
        previousYaw = player.getYRot();
        previousPitch = player.getXRot();
        previousNoGravity = player.isNoGravity();
        previousOnGround = player.onGround();
        playerStateCaptured = true;

        player.setDeltaMovement(Vec3.ZERO);
        player.setNoGravity(true);
        CameraControl.startFreecam(player);
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        VoidClient.LOGGER.info("Freecam enabled");
    }

    @Override
    protected void onDisable() {
        restoreAll();
    }

    private void restoreAll() {
        CameraControl.stopFreecam();
        Minecraft mc = Minecraft.getInstance();

        if (playerStateCaptured && capturedPlayer != null) {
            try {
                capturedPlayer.setNoGravity(previousNoGravity);
                capturedPlayer.setOnGround(previousOnGround);
                capturedPlayer.setDeltaMovement(previousVelocity);
            } catch (Throwable ignored) {}

            if (mc.player == capturedPlayer && mc.level == capturedWorld) {
                try {
                    capturedPlayer.setPos(previousPosition);
                    capturedPlayer.setYRot(previousYaw);
                    capturedPlayer.setXRot(previousPitch);
                    capturedPlayer.setYHeadRot(previousYaw);
                } catch (Throwable ignored) {}
            }
        }

        playerStateCaptured = false;
        capturedPlayer = null;
        capturedWorld = null;
        previousPosition = Vec3.ZERO;
        previousVelocity = Vec3.ZERO;

        if (previousPerspective != null && mc.options != null) {
            mc.options.setCameraType(previousPerspective);
        }
        previousPerspective = null;
        VoidClient.LOGGER.info("Freecam disabled; restored");
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (!CameraControl.isFreecamActive()) return;

        if (mc.player == null || mc.level == null
                || mc.player != capturedPlayer || mc.level != capturedWorld
                || mc.player.isDeadOrDying()) {
            setEnabled(false);
            return;
        }

        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }

        // Touch / virtual pad look → virtual camera
        CameraControl.tickCapturePlayerLook(mc.player);

        mc.player.setDeltaMovement(Vec3.ZERO);
        mc.player.setNoGravity(true);

        double forward = (mc.options.keyUp.isDown() ? 1.0 : 0.0)
                - (mc.options.keyDown.isDown() ? 1.0 : 0.0);
        double strafe = (mc.options.keyLeft.isDown() ? 1.0 : 0.0)
                - (mc.options.keyRight.isDown() ? 1.0 : 0.0);
        double vertical = (mc.options.keyJump.isDown() ? 1.0 : 0.0)
                - (mc.options.keyShift.isDown() ? 1.0 : 0.0);
        CameraControl.moveCamera(forward, strafe, vertical, speed.getFloat());
    }
}
