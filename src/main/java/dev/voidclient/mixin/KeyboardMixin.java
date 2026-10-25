package dev.voidclient.mixin;

import dev.voidclient.VoidClient;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.11: onKey(long window, int action, KeyInput input)
 * Used for module keybinds (not ClickGUI — that uses Fabric KeyBinding).
 */
@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Inject(
            method = "onKey(JILnet/minecraft/client/input/KeyInput;)V",
            at = @At("HEAD")
    )
    private void voidclient$onKey(long window, int action, KeyInput input, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.currentScreen != null) return;
        if (input == null) return;

        // Skip Right Shift — handled by official KeyBinding
        if (input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) return;

        VoidClient.postKeyEvent(input.key(), input.scancode(), action, input.modifiers());
    }
}
