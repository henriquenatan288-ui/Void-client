package dev.voidclient.modules.movement;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Client-side fly (abilities + velocity).
 */
public class Fly extends Module {

    private final NumberSetting speed = addSetting(
            new NumberSetting("Speed", "Fly speed", 1.2, 0.2, 5.0, 0.1));

    public Fly() {
        super("Fly", "Fly like creative.", Category.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        mc.player.getAbilities().allowFlying = true;
        mc.player.getAbilities().flying = true;
        mc.player.sendAbilitiesUpdate();
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (!mc.player.isCreative() && !mc.player.isSpectator()) {
            mc.player.getAbilities().allowFlying = false;
            mc.player.getAbilities().flying = false;
            mc.player.sendAbilitiesUpdate();
        }
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        p.getAbilities().allowFlying = true;
        p.getAbilities().flying = true;

        float s = speed.getFloat();
        Vec3d look = p.getRotationVector();
        Vec3d vel = Vec3d.ZERO;

        if (mc.options.forwardKey.isPressed()) vel = vel.add(look.multiply(s));
        if (mc.options.backKey.isPressed()) vel = vel.add(look.multiply(-s));
        if (mc.options.leftKey.isPressed()) {
            Vec3d left = new Vec3d(look.z, 0, -look.x).normalize().multiply(s);
            vel = vel.add(left);
        }
        if (mc.options.rightKey.isPressed()) {
            Vec3d right = new Vec3d(-look.z, 0, look.x).normalize().multiply(s);
            vel = vel.add(right);
        }
        if (mc.options.jumpKey.isPressed()) vel = vel.add(0, s, 0);
        if (mc.options.sneakKey.isPressed()) vel = vel.add(0, -s, 0);

        p.setVelocity(vel);
    }
}
