package dev.voidclient.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraInvoker {
    @Invoker("setPos")
    void voidclient$setPos(Vec3d pos);

    @Invoker("setRotation")
    void voidclient$setRotation(float yaw, float pitch);
}
