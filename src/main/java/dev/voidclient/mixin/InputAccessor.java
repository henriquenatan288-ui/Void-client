package dev.voidclient.mixin;

import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Input.class)
public interface InputAccessor {
    @Accessor("movementVector")
    void voidclient$setMovementVector(Vec2f movementVector);

    @Accessor("playerInput")
    void voidclient$setPlayerInput(PlayerInput playerInput);
}
