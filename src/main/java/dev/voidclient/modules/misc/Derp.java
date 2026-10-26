package dev.voidclient.modules.misc;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Spins the player head for fun. */
public class Derp extends Module {
    private final NumberSetting speed = addSetting(
            new NumberSetting("Speed", "Spin speed", 25, 5, 80, 1));
    private float yaw;

    public Derp() {
        super("Derp", "Spin your head randomly.", Category.MISC);
    }

    @Override
    protected void onEnable() {
        LocalPlayer p = Minecraft.getInstance().player;
        yaw = p != null ? p.getYRot() : 0;
    }

    @Override
    public void onTick() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        yaw += speed.getFloat();
        p.setYRot(yaw);
        p.setYHeadRot(yaw);
        p.setXRot((float) (Math.sin(yaw * 0.05) * 30.0));
    }
}
