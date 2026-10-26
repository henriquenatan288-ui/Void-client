package dev.voidclient.mixin;

import dev.voidclient.modules.render.CameraControl;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
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
        accessor.voidclient$setMovementVector(Vec2f.ZERO);
        accessor.voidclient$setPlayerInput(PlayerInput.DEFAULT);
    }
}
