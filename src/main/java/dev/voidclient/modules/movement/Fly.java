package dev.voidclient.modules.movement;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.getAbilities().mayfly = true;
        mc.player.getAbilities().flying = true;
        mc.player.onUpdateAbilities();
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!mc.player.isCreative() && !mc.player.isSpectator()) {
            mc.player.getAbilities().mayfly = false;
            mc.player.getAbilities().flying = false;
            mc.player.onUpdateAbilities();
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;

        p.getAbilities().mayfly = true;
        p.getAbilities().flying = true;

        float s = speed.getFloat();
        Vec3 look = p.getLookAngle();
        Vec3 vel = Vec3.ZERO;

        if (mc.options.keyUp.isDown()) vel = vel.add(look.scale(s));
        if (mc.options.keyDown.isDown()) vel = vel.add(look.scale(-s));
        if (mc.options.keyLeft.isDown()) {
            Vec3 left = new Vec3(look.z, 0, -look.x).normalize().scale(s);
            vel = vel.add(left);
        }
        if (mc.options.keyRight.isDown()) {
            Vec3 right = new Vec3(-look.z, 0, look.x).normalize().scale(s);
            vel = vel.add(right);
        }
        if (mc.options.keyJump.isDown()) vel = vel.add(0, s, 0);
        if (mc.options.keyShift.isDown()) vel = vel.add(0, -s, 0);

        p.setDeltaMovement(vel);
    }
}
