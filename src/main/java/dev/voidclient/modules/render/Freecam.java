package dev.voidclient.modules.render;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;

/** Detached camera that preserves and restores the real client's player state. */
public class Freecam extends Module {
    private final NumberSetting speed = addSetting(
            new NumberSetting("Speed", "Free camera speed", 0.8, 0.2, 3.0, 0.1));

    private Perspective previousPerspective;
    private ClientPlayerEntity capturedPlayer;
    private ClientWorld capturedWorld;
    private Vec3d previousPosition = Vec3d.ZERO;
    private Vec3d previousVelocity = Vec3d.ZERO;
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
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) {
            setEnabled(false);
            return;
        }

        previousPerspective = mc.options.getPerspective();
        capturedPlayer = player;
        capturedWorld = mc.world;
        previousPosition = new Vec3d(player.getX(), player.getY(), player.getZ());
        previousVelocity = player.getVelocity();
        previousYaw = player.getYaw();
        previousPitch = player.getPitch();
        previousNoGravity = player.hasNoGravity();
        previousOnGround = player.isOnGround();
        playerStateCaptured = true;

        player.setVelocity(Vec3d.ZERO);
        player.setNoGravity(true);
        CameraControl.startFreecam(player);
        mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        VoidClient.LOGGER.info("Freecam enabled; player onGround={}, noGravity={}",
                previousOnGround, previousNoGravity);
    }

    @Override
    protected void onDisable() {
        CameraControl.stopFreecam();
        MinecraftClient mc = MinecraftClient.getInstance();

        if (playerStateCaptured && capturedPlayer != null) {
            // Restore the original entity even when disconnect/death has already replaced mc.player.
            capturedPlayer.setNoGravity(previousNoGravity);
            capturedPlayer.setOnGround(previousOnGround);
            capturedPlayer.setVelocity(previousVelocity);

            // Only rewind position in the same world/session; never teleport a replacement player.
            if (mc.player == capturedPlayer && mc.world == capturedWorld) {
                capturedPlayer.setPosition(previousPosition);
                capturedPlayer.setYaw(previousYaw);
                capturedPlayer.setPitch(previousPitch);
            }
        }

        playerStateCaptured = false;
        capturedPlayer = null;
        capturedWorld = null;
        previousPosition = Vec3d.ZERO;
        previousVelocity = Vec3d.ZERO;
        if (previousPerspective != null) mc.options.setPerspective(previousPerspective);
        previousPerspective = null;
        VoidClient.LOGGER.info("Freecam disabled; player state restored");
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!CameraControl.isFreecamActive()) return;

        // Exit cleanly if death, disconnect, or a world change replaced the player/world.
        if (mc.player == null || mc.world == null
                || mc.player != capturedPlayer || mc.world != capturedWorld) {
            setEnabled(false);
            return;
        }

        // Freeze the real player; the detached camera receives movement separately.
        mc.player.setVelocity(Vec3d.ZERO);
        mc.player.setNoGravity(true);

        double forward = (mc.options.forwardKey.isPressed() ? 1.0 : 0.0)
                - (mc.options.backKey.isPressed() ? 1.0 : 0.0);
        double strafe = (mc.options.leftKey.isPressed() ? 1.0 : 0.0)
                - (mc.options.rightKey.isPressed() ? 1.0 : 0.0);
        double vertical = (mc.options.jumpKey.isPressed() ? 1.0 : 0.0)
                - (mc.options.sneakKey.isPressed() ? 1.0 : 0.0);
        CameraControl.moveCamera(forward, strafe, vertical, speed.getFloat());
    }
}
