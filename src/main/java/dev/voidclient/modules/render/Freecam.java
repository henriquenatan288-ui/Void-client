package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Detached camera-style free movement (client fly, no clip feel).
 * Simplified stable version for 1.21.11 — no camera mixin required.
 */
public class Freecam extends Module {

    private final NumberSetting speed = addSetting(
            new NumberSetting("Speed", "Freecam speed", 1.5, 0.2, 8.0, 0.1));

    private Vec3d savedPos;
    private float savedYaw, savedPitch;
    private boolean savedFlying, savedAllowFly;

    public Freecam() {
        super("Freecam", "Free camera movement.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        savedPos = p.getPos();
        savedYaw = p.getYaw();
        savedPitch = p.getPitch();
        savedFlying = p.getAbilities().flying;
        savedAllowFly = p.getAbilities().allowFlying;

        p.getAbilities().allowFlying = true;
        p.getAbilities().flying = true;
        p.noClip = true;
        p.sendAbilitiesUpdate();
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        p.noClip = false;
        if (savedPos != null) {
            p.setPosition(savedPos.x, savedPos.y, savedPos.z);
            p.setYaw(savedYaw);
            p.setPitch(savedPitch);
        }
        if (!p.isCreative() && !p.isSpectator()) {
            p.getAbilities().allowFlying = savedAllowFly;
            p.getAbilities().flying = savedFlying;
        }
        p.setVelocity(Vec3d.ZERO);
        p.sendAbilitiesUpdate();
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        p.noClip = true;
        p.getAbilities().flying = true;
        p.fallDistance = 0;

        float s = speed.getFloat();
        Vec3d look = p.getRotationVector();
        Vec3d vel = Vec3d.ZERO;

        if (mc.options.forwardKey.isPressed()) vel = vel.add(look.multiply(s));
        if (mc.options.backKey.isPressed()) vel = vel.add(look.multiply(-s));
        if (mc.options.leftKey.isPressed()) {
            vel = vel.add(new Vec3d(look.z, 0, -look.x).normalize().multiply(s));
        }
        if (mc.options.rightKey.isPressed()) {
            vel = vel.add(new Vec3d(-look.z, 0, look.x).normalize().multiply(s));
        }
        if (mc.options.jumpKey.isPressed()) vel = vel.add(0, s, 0);
        if (mc.options.sneakKey.isPressed()) vel = vel.add(0, -s, 0);

        p.setVelocity(vel);
    }
}
