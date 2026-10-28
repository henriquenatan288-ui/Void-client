package dev.voidclient.mixin;

import dev.voidclient.modules.misc.NoHurtCam;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cancel hurt-camera bob when NoHurtCam is on. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V", at = @At("HEAD"), cancellable = true)
    private void voidclient$noHurtCam(CallbackInfo ci) {
        if (NoHurtCam.isActive()) {
            ci.cancel();
        }
    }
}
