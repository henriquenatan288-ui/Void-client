package dev.voidclient.modules.movement;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Faster ground movement.
 */
public class Speed extends Module {

    private final NumberSetting multiplier = addSetting(
            new NumberSetting("Speed", "Speed multiplier", 1.5, 1.0, 3.0, 0.1));

    public Speed() {
        super("Speed", "Move faster on ground.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        if (!p.onGround()) return;
        if (p.isShiftKeyDown()) return;

        boolean moving = mc.options.keyUp.isDown()
                || mc.options.keyDown.isDown()
                || mc.options.keyLeft.isDown()
                || mc.options.keyRight.isDown();
        if (!moving) return;

        float m = multiplier.getFloat();
        Vec3 v = p.getDeltaMovement();
        p.setDeltaMovement(v.x * m, v.y, v.z * m);
    }
}
