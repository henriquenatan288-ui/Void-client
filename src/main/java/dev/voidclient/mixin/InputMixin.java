package dev.voidclient.mixin;

import dev.voidclient.modules.render.CameraControl;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class InputMixin {
    @Inject(method = "tick()V", at = @At("TAIL"))
    private void voidclient$stopRealPlayerInput(CallbackInfo ci) {
        if (!CameraControl.isFreecamActive()) return;
        InputAccessor accessor = (InputAccessor) (Object) this;
        accessor.voidclient$setMovementVector(Vec2.ZERO);
        accessor.voidclient$setPlayerInput(Input.EMPTY);
    }
}
