package dev.voidclient.mixin;

import dev.voidclient.freecam.FreecamController;
import dev.voidclient.freelook.FreelookController;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Freecam / Freelook camera override for Minecraft 26.2.
 * Uses @Shadow setPosition(DDD) — Invoker(Vec3) was leaving the camera stuck in the head.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    private static final String UPDATE =
            "update(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;ZZF)V";

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

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

    private void applyFreecam() {
        if (!FreecamController.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        FreecamController.step(mc);
        var p = FreecamController.pos();
        this.setPosition(p.x, p.y, p.z);
        this.setRotation(FreecamController.yaw(), FreecamController.pitch());
    }

    @Inject(method = UPDATE, at = @At("RETURN"), require = 0)
    private void voidclient$afterUpdate(
            Level world, Entity focusedEntity, boolean thirdPerson, boolean inverseView,
            float tickProgress, CallbackInfo ci) {
        applyFreecam();
    }

    @Inject(method = "setup(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;ZZF)V",
            at = @At("RETURN"), require = 0)
    private void voidclient$afterSetup(
            Level world, Entity focusedEntity, boolean thirdPerson, boolean inverseView,
            float tickProgress, CallbackInfo ci) {
        applyFreecam();
    }

    @Inject(method = "isDetached()Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void voidclient$detached(CallbackInfoReturnable<Boolean> cir) {
        if (FreecamController.isActive() || FreelookController.shouldPassThrough()) {
            cir.setReturnValue(true);
        }
    }
}
