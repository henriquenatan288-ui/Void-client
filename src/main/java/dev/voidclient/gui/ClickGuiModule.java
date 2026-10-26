package dev.voidclient.gui;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Opens the ClickGUI. Default key handled by Fabric KeyBinding (Right Shift).
 * Module still exists so it shows in the module list / config.
 */
public class ClickGuiModule extends Module {

    public ClickGuiModule() {
        // keybind -1: opening is done by VoidClient.OPEN_GUI_KEY
        super("ClickGUI", "Opens the client interface.", Category.MISC, -1);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && !(mc.screen instanceof ClickGuiScreen)) {
            mc.setScreen(new ClickGuiScreen());
        }
        setEnabled(false);
    }
}
