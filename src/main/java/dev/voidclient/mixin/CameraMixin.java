package dev.voidclient.mixin;

import dev.voidclient.freecam.FreecamController;
import dev.voidclient.freelook.FreelookController;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.2: Camera.update(Level, Entity, boolean, boolean, float)
 * Freecam owns position+rotation; Freelook owns rotation only.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    private static final String UPDATE =
            "update(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;ZZF)V";

    @Redirect(
            method = UPDATE,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"),
            require = 0
    )
    private void voidclient$redirectCameraRotation(Camera camera, float vanillaYaw, float vanillaPitch) {
        CameraInvoker invoker = (CameraInvoker) (Object) camera;
        if (FreecamController.isActive()) {
            invoker.voidclient$setRotation(FreecamController.yaw(), FreecamController.pitch());
        } else if (FreelookController.isActive()) {
            invoker.voidclient$setRotation(FreelookController.yaw(), FreelookController.pitch());
        } else {
            invoker.voidclient$setRotation(vanillaYaw, vanillaPitch);
        }
    }

    @Inject(method = UPDATE, at = @At("RETURN"), require = 0)
    private void voidclient$overrideDetachedCamera(
            Level world,
            Entity focusedEntity,
            boolean thirdPerson,
            boolean inverseView,
            float tickProgress,
            CallbackInfo ci) {
        CameraInvoker invoker = (CameraInvoker) (Object) this;

        if (FreecamController.isActive()) {
            FreecamController.step(Minecraft.getInstance());
            Vec3 p = FreecamController.pos();
            invoker.voidclient$setPos(p);
            invoker.voidclient$setRotation(FreecamController.yaw(), FreecamController.pitch());
            return;
        }

        if (FreelookController.isActive()) {
            // Keep vanilla third-person position; only own rotation
            invoker.voidclient$setRotation(FreelookController.yaw(), FreelookController.pitch());
        }
    }

    @Inject(method = "isDetached()Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void voidclient$freecamDetached(CallbackInfoReturnable<Boolean> cir) {
        if (FreecamController.isActive()) {
            cir.setReturnValue(true);
        }
    }
}
