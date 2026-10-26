package dev.voidclient.mixin;

import dev.voidclient.modules.render.CameraControl;
import dev.voidclient.modules.utility.AutoEat;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Minecraft;
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
        if (CameraControl.isFreecamActive()) {
            InputAccessor accessor = (InputAccessor) (Object) this;
            accessor.voidclient$setMovementVector(Vec2.ZERO);
            accessor.voidclient$setPlayerInput(Input.EMPTY);
            return;
        }

        // AutoEat: keep full movement while using food (no slowdown)
        if (AutoEat.shouldCancelUseSlowdown()) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) return;
            InputAccessor accessor = (InputAccessor) (Object) this;
            Vec2 move = accessor.voidclient$getMovementVector();
            // If vanilla scaled the vector down while using item, restore toward full length
            if (move != null && move.lengthSquared() > 1.0e-6f && move.lengthSquared() < 0.9f) {
                Vec2 restored = new Vec2(move.x, move.y);
                float len = restored.length();
                if (len > 1.0e-6f) {
                    restored = new Vec2(restored.x / len, restored.y / len);
                    accessor.voidclient$setMovementVector(restored);
                }
            }
        }
    }
}
