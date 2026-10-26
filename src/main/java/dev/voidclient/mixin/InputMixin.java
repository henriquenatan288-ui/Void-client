package dev.voidclient.mixin;

import dev.voidclient.modules.render.CameraControl;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Input.class)
public abstract class InputMixin {
    @Shadow protected Vec2f movementVector;
    @Shadow public PlayerInput playerInput;

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void voidclient$stopPlayerInput(CallbackInfo ci) {
        if (!CameraControl.isFreecamActive()) return;
        this.movementVector = Vec2f.ZERO;
        this.playerInput = new PlayerInput(false, false, false, false, false, false, false);
    }
}
