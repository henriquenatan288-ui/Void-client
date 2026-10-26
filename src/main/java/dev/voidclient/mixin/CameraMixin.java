package dev.voidclient.mixin;

import dev.voidclient.modules.render.CameraControl;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import dev.voidclient.mixin.CameraInvoker;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyArgs(
            method = "update(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;ZZF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"),
            require = 0
    )
    private void voidclient$overrideRotation(Args args) {
        if (CameraControl.isFreecamActive() || CameraControl.isFreelookActive()) {
            args.set(0, CameraControl.getYaw());
            args.set(1, CameraControl.getPitch());
        }
    }

    @Inject(
            method = "update(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;ZZF)V",
            at = @At("RETURN")
    )
    private void voidclient$overrideFreecamPosition(World area, Entity focusedEntity,
                                                     boolean thirdPerson, boolean inverseView,
                                                     float tickProgress, CallbackInfo ci) {
        if (!CameraControl.isFreecamActive()) return;
        Vec3d pos = CameraControl.interpolatedPosition(tickProgress);
        Camera camera = (Camera) (Object) this;
        ((CameraInvoker) camera).voidclient$setPos(pos);
        ((CameraInvoker) camera).voidclient$setRotation(CameraControl.getYaw(), CameraControl.getPitch());
    }
}
