package dev.voidclient.modules.movement;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/**
 * Cancels fall damage (client packet reset).
 */
public class NoFall extends Module {

    public NoFall() {
        super("NoFall", "No fall damage.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.getConnection() == null) return;

        if (p.fallDistance > 2.5f) {
            // Tell server we are on ground to avoid fall damage
            mc.getConnection().send(
                    new ServerboundMovePlayerPacket.StatusOnly(true, p.horizontalCollision));
            p.fallDistance = 0;
        }
    }
}
