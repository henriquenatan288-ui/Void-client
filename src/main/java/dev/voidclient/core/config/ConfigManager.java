package dev.voidclient.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.voidclient.VoidClient;
import dev.voidclient.core.Module;
import dev.voidclient.core.ModuleManager;
import dev.voidclient.core.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Saves / loads module states and settings to JSON.
 */
public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path configDir;
    private final Path configFile;
    private final ModuleManager moduleManager;

    public ConfigManager(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
        this.configDir = FabricLoader.getInstance().getConfigDir().resolve("voidclient");
        this.configFile = configDir.resolve("config.json");
    }

    public void load() {
        if (!Files.exists(configFile)) {
            VoidClient.LOGGER.info("No config found, using defaults.");
            return;
        }

        try {
            String json = Files.readString(configFile);
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root == null) return;

            for (Module module : moduleManager.getModules()) {
                if (!root.has(module.getName())) continue;
                JsonObject modObj = root.getAsJsonObject(module.getName());

                if (modObj.has("enabled")) {
                    module.setEnabled(modObj.get("enabled").getAsBoolean());
                }
                if (modObj.has("keybind")) {
                    module.setKeybind(modObj.get("keybind").getAsInt());
                }

                if (modObj.has("settings")) {
                    JsonObject settingsObj = modObj.getAsJsonObject("settings");
                    for (Setting<?> setting : module.getSettings()) {
                        if (settingsObj.has(setting.getName())) {
                            JsonElement el = settingsObj.get(setting.getName());
                            if (el.isJsonPrimitive()) {
                                if (el.getAsJsonPrimitive().isBoolean()) {
                                    setting.fromConfigValue(el.getAsBoolean());
                                } else if (el.getAsJsonPrimitive().isNumber()) {
                                    setting.fromConfigValue(el.getAsNumber());
                                } else if (el.getAsJsonPrimitive().isString()) {
                                    setting.fromConfigValue(el.getAsString());
                                }
                            }
                        }
                    }
                }
            }

            VoidClient.LOGGER.info("Config loaded from {}", configFile);
        } catch (Exception e) {
            VoidClient.LOGGER.error("Failed to load config", e);
        }
    }

    public void save() {
        try {
            Files.createDirectories(configDir);

            JsonObject root = new JsonObject();

            for (Module module : moduleManager.getModules()) {
                JsonObject modObj = new JsonObject();
                modObj.addProperty("enabled", module.isEnabled());
                modObj.addProperty("keybind", module.getKeybind());

                JsonObject settingsObj = new JsonObject();
                for (Setting<?> setting : module.getSettings()) {
                    Object val = setting.toConfigValue();
                    if (val instanceof Boolean b) {
                        settingsObj.addProperty(setting.getName(), b);
                    } else if (val instanceof Number n) {
                        settingsObj.addProperty(setting.getName(), n);
                    } else if (val instanceof String s) {
                        settingsObj.addProperty(setting.getName(), s);
                    }
                }
                modObj.add("settings", settingsObj);
                root.add(module.getName(), modObj);
            }

            Files.writeString(configFile, GSON.toJson(root));
            VoidClient.LOGGER.debug("Config saved to {}", configFile);
        } catch (IOException e) {
            VoidClient.LOGGER.error("Failed to save config", e);
        }
    }
}
