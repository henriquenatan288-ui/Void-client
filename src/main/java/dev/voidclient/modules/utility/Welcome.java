package dev.voidclient.modules.utility;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Sends a welcome message when joining a world/server.
 */
public class Welcome extends Module {

    private final BooleanSetting oncePerSession = addSetting(
            new BooleanSetting("Once", "Only once per session", true));

    private boolean sentThisSession;
    private static boolean listenerRegistered;

    public Welcome() {
        super("Welcome", "Welcome to void (nick) on join.", Category.UTILITY);
        if (!listenerRegistered) {
            listenerRegistered = true;
            ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
                // Fired for any join; module decides if it should speak
                Module self = dev.voidclient.VoidClient.MODULE_MANAGER != null
                        ? dev.voidclient.VoidClient.MODULE_MANAGER.getByName("Welcome")
                        : null;
                if (self instanceof Welcome w) {
                    w.onJoin(client);
                }
            });
        }
        // Enabled by default so users see it
        setEnabled(true);
    }

    private void onJoin(MinecraftClient client) {
        if (!isEnabled()) return;
        if (oncePerSession.get() && sentThisSession) return;
        if (client == null || client.player == null) return;

        String nick = client.player.getName().getString();
        client.player.sendMessage(Text.literal("§7[§fVoid§7] §fWelcome to void §b" + nick),
                false);
        sentThisSession = true;
    }

    @Override
    protected void onDisable() {
        // keep session flag
    }
}
