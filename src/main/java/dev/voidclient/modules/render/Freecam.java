package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Detached, client-only camera. The real player remains at the activation position. */
public class Freecam extends Module {
    private final NumberSetting speed = addSetting(
            new NumberSetting("Speed", "Free camera speed", 0.8, 0.2, 3.0, 0.1));

    private double savedX, savedY, savedZ;
    private float savedYaw, savedPitch;
    private OtherClientPlayerEntity cameraEntity;

    public Freecam() {
        super("Freecam", "Move a detached camera through blocks.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) return;

        savedX = player.getX();
        savedY = player.getY();
        savedZ = player.getZ();
        savedYaw = player.getYaw();
        savedPitch = player.getPitch();

        cameraEntity = new OtherClientPlayerEntity(mc.world, player.getGameProfile());
        cameraEntity.setPosition(savedX, savedY, savedZ);
        cameraEntity.setYaw(savedYaw);
        cameraEntity.setPitch(savedPitch);
        cameraEntity.setHeadYaw(savedYaw);
        cameraEntity.setBodyYaw(savedYaw);
        cameraEntity.noClip = true;
        cameraEntity.setInvisible(true);
        mc.setCameraEntity(cameraEntity);
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;

        if (mc.getCameraEntity() == cameraEntity) {
            mc.setCameraEntity(player);
        }

        if (player != null) {
            player.setPosition(savedX, savedY, savedZ);
            player.setYaw(cameraEntity != null ? cameraEntity.getYaw() : savedYaw);
            player.setPitch(cameraEntity != null ? cameraEntity.getPitch() : savedPitch);
            player.setVelocity(Vec3d.ZERO);
            player.fallDistance = 0;
        }

        cameraEntity = null;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        OtherClientPlayerEntity camera = cameraEntity;
        if (player == null || mc.world == null || camera == null) return;

        // Keep the real player fixed, while mouse movement still controls its view angles.
        player.setPosition(savedX, savedY, savedZ);
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0;

        // Mouse look now belongs to the detached camera entity; do not overwrite it
        // with the frozen player's rotation each tick.
        camera.noClip = true;

        Vec3d look = camera.getRotationVector();
        Vec3d flatForward = new Vec3d(look.x, 0, look.z);
        if (flatForward.lengthSquared() > 1.0E-6) flatForward = flatForward.normalize();

        double s = speed.get();
        Vec3d movement = Vec3d.ZERO;
        if (mc.options.forwardKey.isPressed()) movement = movement.add(look.multiply(s));
        if (mc.options.backKey.isPressed()) movement = movement.add(look.multiply(-s));
        if (mc.options.leftKey.isPressed()) movement = movement.add(new Vec3d(flatForward.z, 0, -flatForward.x).multiply(s));
        if (mc.options.rightKey.isPressed()) movement = movement.add(new Vec3d(-flatForward.z, 0, flatForward.x).multiply(s));
        if (mc.options.jumpKey.isPressed()) movement = movement.add(0, s, 0);
        if (mc.options.sneakKey.isPressed()) movement = movement.add(0, -s, 0);

        camera.setPosition(camera.getX() + movement.x, camera.getY() + movement.y, camera.getZ() + movement.z);
        camera.setVelocity(Vec3d.ZERO);
    }
}
