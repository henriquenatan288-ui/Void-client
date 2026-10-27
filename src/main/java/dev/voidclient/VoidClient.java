package dev.voidclient;

import dev.voidclient.core.ModuleManager;
import dev.voidclient.core.config.ConfigManager;
import dev.voidclient.core.event.EventBus;
import dev.voidclient.core.event.events.KeyEvent;
import dev.voidclient.core.event.events.Render2DEvent;
import dev.voidclient.core.event.events.TickEvent;
import dev.voidclient.gui.ClickGuiScreen;
import dev.voidclient.render.EspExtractors;
import dev.voidclient.render.VoidDraw;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
    public static KeyMapping OPEN_GUI_KEY;

    private static boolean welcomePending;

    @Override
    public void onInitializeClient() {
        LOGGER.info("{} {} initializing...", NAME, VERSION);

        // Single world-render path: ESP modules feed VoidDraw and are submitted once.
        VoidDraw.init();
        EspExtractors.register();

        EVENT_BUS = new EventBus();
        MODULE_MANAGER = new ModuleManager(EVENT_BUS);
        CONFIG_MANAGER = new ConfigManager(MODULE_MANAGER);
        CONFIG_MANAGER.load();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        OPEN_GUI_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.voidclient.clickgui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> welcomePending = true);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> welcomePending = false);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            EVENT_BUS.post(new TickEvent());

            if (welcomePending && client.player != null && client.level == null) {
                // no-op guard
            }
            if (welcomePending && client.player != null && client.level != null) {
                String nick = client.player.getName().getString();
                client.player.sendSystemMessage(
                        Component.literal("§7[§fVoid§7] §fWelcome to Void §b" + nick));
                welcomePending = false;
            }

            while (OPEN_GUI_KEY.consumeClick()) {
                toggleClickGui(client);
            }
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "hud"), (drawContext, tickCounter) -> {
            float tickDelta = tickCounter.getGameTimeDeltaPartialTick(false);
            EVENT_BUS.post(new Render2DEvent(drawContext, tickDelta));
        });

        LOGGER.info("{} initialized. Modules: {}. Press Right Shift for GUI.",
                NAME, MODULE_MANAGER.getModules().size());
    }

    private static void toggleClickGui(Minecraft client) {
        if (client == null) return;
        if (client.gui.screen() instanceof ClickGuiScreen) {
            client.gui.screen().onClose();
            return;
        }
        if (client.gui.screen() == null) {
            client.gui.setScreen(new ClickGuiScreen());
            LOGGER.info("ClickGUI opened");
        }
    }

    public static void postKeyEvent(int key, int scancode, int action, int modifiers) {
        if (EVENT_BUS != null) {
            EVENT_BUS.post(new KeyEvent(key, scancode, action, modifiers));
        }
    }

    public static void saveConfig() {
        if (CONFIG_MANAGER != null) CONFIG_MANAGER.save();
    }
}
