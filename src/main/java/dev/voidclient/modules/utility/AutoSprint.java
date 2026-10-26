package dev.voidclient.modules.utility;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Sprints automatically when moving. */
public class AutoSprint extends Module {
    public AutoSprint() {
        super("AutoSprint", "Sprint automatically while moving forward.", Category.UTILITY);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        if (p.isCrouching()) return;
        if (p.getFoodData().getFoodLevel() <= 6) return;

        boolean forward = false;
        try {
            if (mc.options != null && mc.options.keyUp.isDown()) forward = true;
        } catch (Throwable ignored) {}
        try {
            if (p.input != null && p.input.getMoveVector().y > 0.01f) forward = true;
        } catch (Throwable ignored) {}

        if (forward && !p.isSprinting()) {
            p.setSprinting(true);
        }
    }
}
