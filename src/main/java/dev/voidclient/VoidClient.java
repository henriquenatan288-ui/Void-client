package dev.voidclient;

import net.fabricmc.api.ClientModInitializer;
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

    @Override
    public void onInitializeClient() {
        LOGGER.info("{} {} initializing...", NAME, VERSION);

        // TODO: etapa (b) - ModuleManager, EventBus, ConfigManager
        // TODO: etapa (c) - ClickGUI

        LOGGER.info("{} initialized successfully.", NAME);
    }
}
