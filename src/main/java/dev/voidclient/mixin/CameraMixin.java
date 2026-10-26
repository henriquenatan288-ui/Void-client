package dev.voidclient.mixin;

import dev.voidclient.modules.render.CameraControl;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    private static final String UPDATE = "update(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;ZZF)V";

    @Redirect(method = UPDATE,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"),
            require = 1)
    private void voidclient$redirectCameraRotation(Camera camera, float vanillaYaw, float vanillaPitch) {
        CameraInvoker invoker = (CameraInvoker) (Object) camera;
        if (CameraControl.isFreecamActive() || CameraControl.isFreelookActive()) {
            invoker.voidclient$setRotation(CameraControl.getYaw(), CameraControl.getPitch());
        } else {
            invoker.voidclient$setRotation(vanillaYaw, vanillaPitch);
        }
    }

    @Inject(method = UPDATE, at = @At("RETURN"))
    private void voidclient$overrideFreecamPosition(World world, Entity focusedEntity,
                                                     boolean thirdPerson, boolean inverseView,
                                                     float tickProgress, CallbackInfo ci) {
        if (!CameraControl.isFreecamActive()) return;
        Vec3d pos = CameraControl.interpolatedPosition(tickProgress);
        CameraInvoker invoker = (CameraInvoker) (Object) this;
        invoker.voidclient$setPos(pos);
        invoker.voidclient$setRotation(CameraControl.getYaw(), CameraControl.getPitch());
    }
}
