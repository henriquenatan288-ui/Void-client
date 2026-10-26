package dev.voidclient.mixin;

import dev.voidclient.modules.render.CameraControl;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.2: Camera.update(Level, Entity, boolean thirdPerson, boolean inverseView, float tickProgress)
 *
 * Certainty: apply rotation + freecam position AFTER vanilla finishes (RETURN),
 * otherwise third-person offset or vanilla setRotation wins the frame.
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
        if (CameraControl.isDetachedLook()) {
            invoker.voidclient$setRotation(CameraControl.getYaw(), CameraControl.getPitch());
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

        if (CameraControl.isFreecamActive()) {
            Vec3 pos = CameraControl.interpolatedPosition(tickProgress);
            invoker.voidclient$setPos(pos);
            invoker.voidclient$setRotation(CameraControl.getYaw(), CameraControl.getPitch());
            return;
        }

        if (CameraControl.isFreelookActive()) {
            // Keep vanilla third-person position; only own the rotation
            invoker.voidclient$setRotation(CameraControl.getYaw(), CameraControl.getPitch());
        }
    }
}
