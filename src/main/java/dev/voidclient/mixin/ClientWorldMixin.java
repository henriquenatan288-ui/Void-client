package dev.voidclient.mixin;

import dev.voidclient.modules.render.BreakWaves;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observes the actual client-side confirmation that a tracked block became air. */
@Mixin(ClientLevel.class)
public abstract class ClientWorldMixin {
    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD")
    )
    private void voidclient$onBlockSetToAir(
            BlockPos pos, BlockState newState, int flags, int maxUpdateDepth,
            CallbackInfoReturnable<Boolean> cir) {
        ClientLevel world = (ClientLevel) (Object) this;
        BlockState oldState = world.getBlockState(pos);
        boolean blockWasRemoved = !oldState.isAir()
                && (newState.isAir() || oldState.getBlock() != newState.getBlock());
        if (blockWasRemoved) {
            BreakWaves.onClientBlockRemoved(pos);
        }
    }
}
