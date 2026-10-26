package dev.voidclient.mixin;

import dev.voidclient.modules.render.BreakWaves;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tracks all local mining paths; survival block-state confirmation is handled by ClientWorldMixin. */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(
            method = "attackBlock(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z",
            at = @At("HEAD")
    )
    private void voidclient$trackInitialBlockAttack(
            BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        BreakWaves.trackBlockBreakTarget(pos);
    }

    @Inject(
            method = "updateBlockBreakingProgress(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z",
            at = @At("HEAD")
    )
    private void voidclient$trackContinuousBlockBreaking(
            BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        BreakWaves.trackBlockBreakTarget(pos);
    }

    @Inject(
            method = "breakBlock(Lnet/minecraft/util/math/BlockPos;)Z",
            at = @At("HEAD")
    )
    private void voidclient$trackImmediateBlockBreak(
            BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // Creative and other immediate break paths may skip the continuous progress method.
        BreakWaves.trackBlockBreakTarget(pos);
    }

    @Inject(method = "cancelBlockBreaking()V", at = @At("HEAD"))
    private void voidclient$noteBlockBreakingCancelled(CallbackInfo ci) {
        // Keep the short-lived target until the server's block-state confirmation arrives.
        // BreakWaves expires it automatically if no matching update arrives.
    }
}
