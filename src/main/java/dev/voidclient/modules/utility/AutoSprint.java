package dev.voidclient.modules.utility;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Sprints automatically when moving forward. */
public class AutoSprint extends Module {
    public AutoSprint() {
        super("AutoSprint", "Sprint automatically while moving forward.", Category.UTILITY);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        if (p.input == null) return;
        boolean forward = p.input.getMoveVector().y > 0.01f
                || (mc.options != null && mc.options.keyUp.isDown());
        if (forward && !p.isSprinting() && !p.isCrouching() && p.getFoodData().getFoodLevel() > 6) {
            p.setSprinting(true);
        }
    }
}
