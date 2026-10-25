package dev.voidclient.gui;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/**
 * Opens the ClickGUI. Default key: Right Shift.
 */
public class ClickGuiModule extends Module {

    public ClickGuiModule() {
        super("ClickGUI", "Opens the client interface.", Category.MISC, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            mc.setScreen(new ClickGuiScreen());
        }
        setEnabled(false);
    }
}
