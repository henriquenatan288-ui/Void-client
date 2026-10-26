package dev.voidclient.mixin;

import dev.voidclient.modules.render.CameraControl;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityLookMixin {
    @Inject(method = "turn(DD)V", at = @At("HEAD"), cancellable = true)
    private void voidclient$redirectCameraLook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || (Object) this != client.player) return;
        if (!CameraControl.isFreecamActive() && !CameraControl.isFreelookActive()) return;
        CameraControl.updateLook(cursorDeltaX, cursorDeltaY);
        ci.cancel();
    }
}
