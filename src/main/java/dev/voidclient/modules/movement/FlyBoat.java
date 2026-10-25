package dev.voidclient.modules.movement;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Control boat in air / boost speed.
 */
public class FlyBoat extends Module {

    private final NumberSetting speed = addSetting(
            new NumberSetting("Speed", "Boat fly speed", 1.5, 0.3, 6.0, 0.1));

    public FlyBoat() {
        super("FlyBoat", "Fly while riding a boat.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        Entity vehicle = p.getVehicle();
        if (!(vehicle instanceof BoatEntity boat)) return;

        float s = speed.getFloat();
        Vec3d look = p.getRotationVector();
        Vec3d vel = Vec3d.ZERO;

        if (mc.options.forwardKey.isPressed()) vel = vel.add(look.multiply(s));
        if (mc.options.backKey.isPressed()) vel = vel.add(look.multiply(-s * 0.6));
        if (mc.options.jumpKey.isPressed()) vel = vel.add(0, s, 0);
        if (mc.options.sneakKey.isPressed()) vel = vel.add(0, -s, 0);

        if (vel.lengthSquared() > 0) {
            boat.setVelocity(vel);
        }
    }
}
