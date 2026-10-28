package dev.voidclient.mixin;

import dev.voidclient.freecam.FreecamController;
import dev.voidclient.freelook.FreelookController;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Freecam / Freelook for Minecraft 26.2 (Mojang).
 *
 * Camera no longer has setup(Level, Entity, ZZF). Pose is built in
 * update(DeltaTracker) via alignWithEntity(partialTick). We run after that
 * and overwrite position/rotation (same pattern as WI-Freecam 26.2).
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    private boolean detached;

    @Shadow
    private Vec3 position;

    @Shadow
    private float xRot;

    @Shadow
    private float yRot;

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setPosition(Vec3 pos);

    @Inject(
            method = "update(Lnet/minecraft/client/DeltaTracker;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;alignWithEntity(F)V",
                    shift = At.Shift.AFTER))
    private void voidclient$afterAlign(DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!FreecamController.isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        FreecamController.step(mc);

        detached = true;
        Vec3 p = FreecamController.pos();
        float yaw = FreecamController.yaw();
        float pitch = FreecamController.pitch();

        setPosition(p);
        setRotation(yaw, pitch);

        // belt-and-suspenders if setters are no-op for any reason
        this.position = p;
        this.yRot = yaw;
        this.xRot = pitch;
    }

    @Inject(method = "isDetached()Z", at = @At("HEAD"), cancellable = true)
    private void voidclient$detached(CallbackInfoReturnable<Boolean> cir) {
        if (FreecamController.isActive() || FreelookController.shouldPassThrough()) {
            cir.setReturnValue(true);
        }
    }
}
