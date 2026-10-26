package dev.voidclient.mixin;

import dev.voidclient.freecam.FreecamController;
import dev.voidclient.modules.utility.AutoEat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class InputMixin {
    @Inject(method = "tick()V", at = @At("TAIL"))
    private void voidclient$afterInputTick(CallbackInfo ci) {
        InputAccessor accessor = (InputAccessor) (Object) this;

        // Freecam: freeze real player movement (keys still read by FreecamController.step)
        if (FreecamController.isActive()) {
            accessor.voidclient$setMovementVector(Vec2.ZERO);
            accessor.voidclient$setPlayerInput(Input.EMPTY);
            return;
        }

        // AutoEat no-slow: restore movement length while eating
        if (AutoEat.shouldCancelUseSlowdown()) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) return;
            Vec2 move = accessor.voidclient$getMovementVector();
            if (move != null && move.lengthSquared() > 1.0e-6f && move.lengthSquared() < 0.9f) {
                float len = move.length();
                if (len > 1.0e-6f) {
                    accessor.voidclient$setMovementVector(new Vec2(move.x / len, move.y / len));
                }
            }
        }
    }
}
