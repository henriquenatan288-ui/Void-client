package dev.voidclient.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraInvoker {
    @Invoker("setPosition")
    void voidclient$setPos(Vec3 pos);

    @Invoker("setRotation")
    void voidclient$setRotation(float yaw, float pitch);
}
