package dev.voidclient.mixin;

import dev.voidclient.modules.render.CameraControl;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents the detached camera from sending vanilla player movement packets. */
@Mixin(LocalPlayer.class)
public abstract class ClientPlayerFreecamMixin {
    @Inject(method = "sendPosition()V", at = @At("HEAD"), cancellable = true)
    private void voidclient$cancelFreecamMovementPackets(CallbackInfo ci) {
        if (CameraControl.isFreecamActive()) ci.cancel();
    }
}
