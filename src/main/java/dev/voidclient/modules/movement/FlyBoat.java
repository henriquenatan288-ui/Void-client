package dev.voidclient.modules.movement;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.phys.Vec3;

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
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;

        Entity vehicle = p.getVehicle();
        if (!(vehicle instanceof Boat boat)) return;

        float s = speed.getFloat();
        Vec3 look = p.getLookAngle();
        Vec3 vel = Vec3.ZERO;

        if (mc.options.keyUp.isDown()) vel = vel.add(look.scale(s));
        if (mc.options.keyDown.isDown()) vel = vel.add(look.scale(-s * 0.6));
        if (mc.options.keyJump.isDown()) vel = vel.add(0, s, 0);
        if (mc.options.keyShift.isDown()) vel = vel.add(0, -s, 0);

        if (vel.lengthSqr() > 0) {
            boat.setDeltaMovement(vel);
        }
    }
}
