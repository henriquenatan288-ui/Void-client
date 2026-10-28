package dev.voidclient.mixin;

import dev.voidclient.freecam.FreecamController;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Block attack/use while freecam has Block Interactions enabled. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "startAttack()Z", at = @At("HEAD"), cancellable = true)
    private void voidclient$noAttackInFreecam(CallbackInfoReturnable<Boolean> cir) {
        if (FreecamController.blocksInteraction()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "startUseItem()V", at = @At("HEAD"), cancellable = true)
    private void voidclient$noUseInFreecam(CallbackInfo ci) {
        if (FreecamController.blocksInteraction()) {
            ci.cancel();
        }
    }

    @Inject(method = "continueAttack(Z)V", at = @At("HEAD"), cancellable = true)
    private void voidclient$noContinueAttackInFreecam(boolean leftClick, CallbackInfo ci) {
        if (FreecamController.blocksInteraction()) {
            ci.cancel();
        }
    }
}
