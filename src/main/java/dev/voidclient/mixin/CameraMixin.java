package dev.voidclient.mixin;

import dev.voidclient.freecam.FreecamController;
import dev.voidclient.freelook.FreelookController;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Detach camera when freecam is on (26.2 Mojang).
 * Writes pose at end of Camera.setup — that is the method the game actually calls
 * (Yarn "update" does not exist here; require=0 was hiding a dead inject).
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

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

    /**
     * Mojang 26.2: setup(Level, Entity, thirdPerson, inverseView, partialTick).
     */
    @Inject(
            method = "setup(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;ZZF)V",
            at = @At("RETURN"))
    private void voidclient$freecamAfterSetup(
            Level level,
            Entity focusedEntity,
            boolean thirdPerson,
            boolean inverseView,
            float partialTick,
            CallbackInfo ci) {
        if (!FreecamController.isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        FreecamController.step(mc);
        Vec3 p = FreecamController.pos();
        float yaw = FreecamController.yaw();
        float pitch = FreecamController.pitch();

        // Prefer setters; also write fields so a no-op setter still sticks
        try {
            this.setPosition(p.x, p.y, p.z);
            this.setRotation(yaw, pitch);
        } catch (Throwable ignored) {}
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
