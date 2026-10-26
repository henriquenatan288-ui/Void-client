package dev.voidclient.mixin;

import dev.voidclient.modules.render.BreakWaves;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tracks all local mining paths; survival block-state confirmation is handled by ClientWorldMixin. */
@Mixin(MultiPlayerGameMode.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(
            method = "startDestroyBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z",
            at = @At("HEAD")
    )
    private void voidclient$trackInitialBlockAttack(
            BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        BreakWaves.trackBlockBreakTarget(pos);
    }

    @Inject(
            method = "continueDestroyBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z",
            at = @At("HEAD")
    )
    private void voidclient$trackContinuousBlockBreaking(
            BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        BreakWaves.trackBlockBreakTarget(pos);
    }

    @Inject(
            method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z",
            at = @At("HEAD")
    )
    private void voidclient$trackImmediateBlockBreak(
            BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // Creative and other immediate break paths may skip the continuous progress method.
        BreakWaves.trackBlockBreakTarget(pos);
    }

}
