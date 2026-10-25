package dev.voidclient.mixin;

import dev.voidclient.modules.render.BreakWaves;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(
            method = "breakBlock(Lnet/minecraft/util/math/BlockPos;)Z",
            at = @At("RETURN")
    )
    private void voidclient$onBlockBroken(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            BreakWaves.onClientBlockBreak(pos);
        }
    }
}
