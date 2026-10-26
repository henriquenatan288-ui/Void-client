package dev.voidclient.modules.utility;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;

/** Automatically respawns on the death screen. */
public class AutoRespawn extends Module {
    public AutoRespawn() {
        super("AutoRespawn", "Respawn automatically after death.", Category.UTILITY);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui == null) return;
        if (mc.gui.screen() instanceof DeathScreen) {
            mc.player.respawn();
            mc.gui.setScreen(null);
        }
    }
}
