package dev.voidclient.mixin;

import dev.voidclient.VoidClient;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures key presses so modules can be toggled by keybind.
 *
 * Confiança: ALTA
 * - Keyboard.onKey é o método padrão usado por quase todos os clients Fabric.
 * - Assinatura em 1.21.11 (Yarn): onKey(long window, int key, int scancode, int action, int modifiers)
 */
@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Inject(method = "onKey", at = @At("HEAD"))
    private void voidclient$onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        // Ignore when a screen (chat, inventory, ClickGUI later) is open
        if (mc != null && mc.currentScreen != null) return;

        VoidClient.postKeyEvent(key, scancode, action, modifiers);
    }
}
