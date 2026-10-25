package dev.voidclient.modules.movement;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Walk on water and lava surfaces without repeatedly bouncing the player upward. */
public class Jesus extends Module {
    public Jesus() {
        super("Jesus", "Walk steadily on water and lava.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null || player.isSneaking()) return;

        BlockPos feetPos = player.getBlockPos();
        BlockPos belowPos = feetPos.down();
        FluidState atFeet = mc.world.getFluidState(feetPos);
        FluidState below = mc.world.getFluidState(belowPos);

        boolean feetInWater = atFeet.isIn(FluidTags.WATER);
        boolean feetInLava = atFeet.isIn(FluidTags.LAVA);
        boolean fluidBelow = below.isIn(FluidTags.WATER) || below.isIn(FluidTags.LAVA);
        if (!feetInWater && !feetInLava && !fluidBelow) return;

        Vec3d velocity = player.getVelocity();
        if (feetInWater || feetInLava) {
            // Lift out of the fluid only while submerged; once at the surface, stop vertical motion.
            player.setVelocity(velocity.x, Math.max(0.08, velocity.y), velocity.z);
        } else if (fluidBelow && velocity.y <= 0.0) {
            player.setVelocity(velocity.x, 0.0, velocity.z);
            player.setOnGround(true);
        }

        player.fallDistance = 0;
    }
}
