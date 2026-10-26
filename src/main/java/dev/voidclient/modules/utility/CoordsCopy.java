package dev.voidclient.modules.utility;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Copies current coordinates to the system clipboard when toggled on. */
public class CoordsCopy extends Module {
    public CoordsCopy() {
        super("CoordsCopy", "Copy your XYZ to the clipboard when enabled.", Category.UTILITY);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) {
            setEnabled(false);
            return;
        }
        String text = String.format("%.1f %.1f %.1f", p.getX(), p.getY(), p.getZ());
        mc.keyboardHandler.setClipboard(text);
        VoidClient.LOGGER.info("Copied coords: {}", text);
        // One-shot: turn off after copy
        setEnabled(false);
    }
}
