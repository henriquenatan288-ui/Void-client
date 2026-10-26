package dev.voidclient.modules.movement;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

/** Walk on water and lava surfaces without repeatedly bouncing the player upward. */
public class Jesus extends Module {
    public Jesus() {
        super("Jesus", "Walk steadily on water and lava.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || player.isShiftKeyDown()) return;

        BlockPos feetPos = player.blockPosition();
        BlockPos belowPos = feetPos.below();
        FluidState atFeet = mc.level.getFluidState(feetPos);
        FluidState below = mc.level.getFluidState(belowPos);

        boolean feetInWater = atFeet.is(FluidTags.WATER);
        boolean feetInLava = atFeet.is(FluidTags.LAVA);
        boolean fluidBelow = below.is(FluidTags.WATER) || below.is(FluidTags.LAVA);
        if (!feetInWater && !feetInLava && !fluidBelow) return;

        Vec3 velocity = player.getDeltaMovement();
        if (feetInWater || feetInLava) {
            // Lift out of the fluid only while submerged; once at the surface, stop vertical motion.
            player.setDeltaMovement(velocity.x, Math.max(0.08, velocity.y), velocity.z);
        } else if (fluidBelow && velocity.y <= 0.0) {
            player.setDeltaMovement(velocity.x, 0.0, velocity.z);
            player.setOnGround(true);
        }

        player.fallDistance = 0;
    }
}
