package dev.voidclient.modules.misc;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Logs death coordinates when you die. */
public class DeathCoords extends Module {
    private boolean wasDead;

    public DeathCoords() {
        super("DeathCoords", "Show coordinates when you die.", Category.MISC);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) {
            wasDead = false;
            return;
        }

        boolean dead = p.isDeadOrDying();
        if (dead && !wasDead) {
            String msg = String.format("Death at %.1f %.1f %.1f", p.getX(), p.getY(), p.getZ());
            VoidClient.LOGGER.info(msg);
            try {
                mc.keyboardHandler.setClipboard(
                        String.format("%.1f %.1f %.1f", p.getX(), p.getY(), p.getZ()));
            } catch (Throwable ignored) {}
        }
        wasDead = dead;
    }
}
