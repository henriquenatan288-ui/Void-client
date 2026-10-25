package dev.voidclient;

import dev.voidclient.core.ModuleManager;
import dev.voidclient.core.config.ConfigManager;
import dev.voidclient.core.event.EventBus;
import dev.voidclient.core.event.events.KeyEvent;
import dev.voidclient.core.event.events.Render2DEvent;
import dev.voidclient.core.event.events.TickEvent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entrypoint of Void Client.
 * Client-side only.
 */
public class VoidClient implements ClientModInitializer {

    public static final String MOD_ID = "voidclient";
    public static final String NAME = "Void Client";
    public static final String VERSION = "1.0.0";

    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    public static EventBus EVENT_BUS;
    public static ModuleManager MODULE_MANAGER;
    public static ConfigManager CONFIG_MANAGER;

    @Override
    public void onInitializeClient() {
        LOGGER.info("{} {} initializing...", NAME, VERSION);

        EVENT_BUS = new EventBus();
        MODULE_MANAGER = new ModuleManager(EVENT_BUS);
        CONFIG_MANAGER = new ConfigManager(MODULE_MANAGER);

        // Load config after modules are registered
        CONFIG_MANAGER.load();

        // Client tick → TickEvent
        ClientTickEvents.END_CLIENT_TICK.register(client -> EVENT_BUS.post(new TickEvent()));

        // HUD render → Render2DEvent
        // Note: HudRenderCallback is deprecated in newer Fabric API but still present in 0.141.x for 1.21.11.
        // Signature: (DrawContext / GuiGraphics, DeltaTracker)
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            float tickDelta = tickCounter.getTickProgress(false);
            EVENT_BUS.post(new Render2DEvent(drawContext, tickDelta));
        });

        LOGGER.info("{} initialized successfully. Modules: {}", NAME, MODULE_MANAGER.getModules().size());
    }

    /**
     * Helper to post a key event from mixins.
     */
    public static void postKeyEvent(int key, int scancode, int action, int modifiers) {
        if (EVENT_BUS != null) {
            EVENT_BUS.post(new KeyEvent(key, scancode, action, modifiers));
        }
    }

    /**
     * Call this when the player leaves the world or on shutdown to persist settings.
     */
    public static void saveConfig() {
        if (CONFIG_MANAGER != null) {
            CONFIG_MANAGER.save();
        }
    }
}
