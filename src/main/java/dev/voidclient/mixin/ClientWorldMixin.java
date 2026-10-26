package dev.voidclient.mixin;

import dev.voidclient.modules.render.BreakWaves;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observes the actual client-side confirmation that a tracked block became air. */
@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
    @Inject(
            method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z",
            at = @At("HEAD")
    )
    private void voidclient$onBlockSetToAir(
            BlockPos pos, BlockState newState, int flags, int maxUpdateDepth,
            CallbackInfoReturnable<Boolean> cir) {
        if (!newState.isAir()) return;

        ClientWorld world = (ClientWorld) (Object) this;
        if (!world.getBlockState(pos).isAir()) {
            BreakWaves.onClientBlockChangedToAir(pos);
        }
    }
}
