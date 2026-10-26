package dev.voidclient.mixin;

import dev.voidclient.VoidClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.11: onKey(long window, int action, KeyInput input)
 * Used for module keybinds (not ClickGUI — that uses Fabric KeyBinding).
 */
@Mixin(KeyboardHandler.class)
public class KeyboardMixin {

    @Inject(
            method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V",
            at = @At("HEAD")
    )
    private void voidclient$onKey(long window, int action, KeyEvent input, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gui.screen() != null) return;
        if (input == null) return;

        // Skip Right Shift — handled by official KeyBinding
        if (input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) return;

        VoidClient.postKeyEvent(input.key(), input.scancode(), action, input.modifiers());
    }
}
