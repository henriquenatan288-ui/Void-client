package dev.voidclient.modules.movement;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

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
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null) return;
        if (!p.isOnGround()) return;
        if (p.isSneaking()) return;

        boolean moving = mc.options.forwardKey.isPressed()
                || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed()
                || mc.options.rightKey.isPressed();
        if (!moving) return;

        float m = multiplier.getFloat();
        Vec3d v = p.getVelocity();
        p.setVelocity(v.x * m, v.y, v.z * m);
    }
}
