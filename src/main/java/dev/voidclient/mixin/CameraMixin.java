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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Freecam: owns position + rotation.
 * Freelook: owns rotation only (body stays).
 * Uses ModifyVariable on setRotation so it works regardless of update/setup name.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    private static final String UPDATE =
            "update(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;ZZF)V";

    @ModifyVariable(method = "setRotation(FF)V", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private float voidclient$modYaw(float yaw) {
        if (FreecamController.isActive()) return FreecamController.yaw();
        if (FreelookController.isActive()) return FreelookController.yaw();
        return yaw;
    }

    @ModifyVariable(method = "setRotation(FF)V", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private float voidclient$modPitch(float pitch) {
        if (FreecamController.isActive()) return FreecamController.pitch();
        if (FreelookController.isActive()) return FreelookController.pitch();
        return pitch;
    }

    @Inject(method = UPDATE, at = @At("RETURN"), require = 0)
    private void voidclient$afterUpdate(
            Level world,
            Entity focusedEntity,
            boolean thirdPerson,
            boolean inverseView,
            float tickProgress,
            CallbackInfo ci) {
        if (!FreecamController.isActive()) return;
        CameraInvoker invoker = (CameraInvoker) (Object) this;
        FreecamController.step(Minecraft.getInstance());
        Vec3 p = FreecamController.pos();
        invoker.voidclient$setPos(p);
        invoker.voidclient$setRotation(FreecamController.yaw(), FreecamController.pitch());
    }

    @Inject(method = "isDetached()Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void voidclient$freecamDetached(CallbackInfoReturnable<Boolean> cir) {
        if (FreecamController.isActive()) {
            cir.setReturnValue(true);
        }
    }
}
