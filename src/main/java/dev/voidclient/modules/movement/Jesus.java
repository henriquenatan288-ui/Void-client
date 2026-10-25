package dev.voidclient.modules.movement;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Walk on water / lava surface.
 */
public class Jesus extends Module {

    public Jesus() {
        super("Jesus", "Walk on water and lava.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;
        if (p.isSneaking()) return;

        BlockPos pos = p.getBlockPos();
        FluidState fluid = mc.world.getFluidState(pos);
        FluidState below = mc.world.getFluidState(pos.down());

        boolean inOrOnFluid = fluid.isIn(FluidTags.WATER) || fluid.isIn(FluidTags.LAVA)
                || below.isIn(FluidTags.WATER) || below.isIn(FluidTags.LAVA);

        if (!inOrOnFluid) return;

        // Keep player on surface
        Vec3d v = p.getVelocity();
        if (v.y < 0.1) {
            p.setVelocity(v.x, 0.08, v.z);
        }
        p.setOnGround(true);
        p.fallDistance = 0;
    }
}
