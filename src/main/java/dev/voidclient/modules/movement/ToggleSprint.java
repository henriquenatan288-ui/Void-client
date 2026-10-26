package dev.voidclient.modules.movement;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Keeps the player sprinting while the module is enabled.
 *
 * Confiança: ALTA
 * - ClientPlayerEntity#setSprinting(boolean) existe e é o método correto em Yarn 1.21.11.
 * - Verificamos null de player a cada tick.
 */
public class ToggleSprint extends Module {

    public ToggleSprint() {
        super("ToggleSprint", "Automatically sprints while moving.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        LocalPlayer player = mc.player;
        if (player == null) return;

        if (!player.isSprinting()) {
            player.setSprinting(true);
        }
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        mc.player.setSprinting(false);
    }
}
