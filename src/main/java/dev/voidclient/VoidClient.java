package dev.voidclient;

import dev.voidclient.core.ModuleManager;
import dev.voidclient.core.config.ConfigManager;
import dev.voidclient.core.event.EventBus;
import dev.voidclient.core.event.events.KeyEvent;
import dev.voidclient.core.event.events.Render2DEvent;
import dev.voidclient.core.event.events.TickEvent;
import dev.voidclient.gui.ClickGuiScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoidClient implements ClientModInitializer {

    public static final String MOD_ID = "voidclient";
    public static final String NAME = "Void Client";
    public static final String VERSION = "1.0.0";

    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    public static EventBus EVENT_BUS;
    public static ModuleManager MODULE_MANAGER;
    public static ConfigManager CONFIG_MANAGER;

    /** Official Fabric keybind — Right Shift opens ClickGUI */
    public static KeyBinding OPEN_GUI_KEY;

    @Override
    public void onInitializeClient() {
        LOGGER.info("{} {} initializing...", NAME, VERSION);

        EVENT_BUS = new EventBus();
        MODULE_MANAGER = new ModuleManager(EVENT_BUS);
        CONFIG_MANAGER = new ConfigManager(MODULE_MANAGER);
        CONFIG_MANAGER.load();

        // ── KeyBinding oficial (Yarn 1.21.11) ─────────────────────────────
        KeyBinding.Category category = KeyBinding.Category.register(
                Identifier.of(MOD_ID, "main")
        );

        OPEN_GUI_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.voidclient.clickgui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            EVENT_BUS.post(new TickEvent());

            while (OPEN_GUI_KEY.wasPressed()) {
                toggleClickGui(client);
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            float tickDelta = tickCounter.getTickProgress(false);
            EVENT_BUS.post(new Render2DEvent(drawContext, tickDelta));
        });

        LOGGER.info("{} initialized. Modules: {}. Press Right Shift for GUI.",
                NAME, MODULE_MANAGER.getModules().size());
    }

    private static void toggleClickGui(MinecraftClient client) {
        if (client == null) return;

        if (client.currentScreen instanceof ClickGuiScreen) {
            client.currentScreen.close();
            return;
        }

        if (client.currentScreen == null) {
            client.setScreen(new ClickGuiScreen());
            LOGGER.info("ClickGUI opened");
        }
    }

    public static void postKeyEvent(int key, int scancode, int action, int modifiers) {
        if (EVENT_BUS != null) {
            EVENT_BUS.post(new KeyEvent(key, scancode, action, modifiers));
        }
    }

    public static void saveConfig() {
        if (CONFIG_MANAGER != null) {
            CONFIG_MANAGER.save();
        }
    }
}
