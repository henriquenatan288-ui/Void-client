package dev.voidclient.modules.utility;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;

/** Automatically clicks respawn on the death screen. */
public class AutoRespawn extends Module {
    public AutoRespawn() {
        super("AutoRespawn", "Respawn automatically after death.", Category.UTILITY);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen instanceof DeathScreen) {
            mc.player.respawn();
            mc.setScreen(null);
        }
    }
}
