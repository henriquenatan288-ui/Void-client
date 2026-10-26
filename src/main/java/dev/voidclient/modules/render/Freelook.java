package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class Freelook extends Module {
    private CameraType previous;

    public Freelook() {
        super("Freelook", "Rotate the third-person camera independently from the player.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        previous = mc.options.getCameraType();
        CameraControl.startFreelook(player);
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    @Override
    protected void onDisable() {
        CameraControl.stopFreelook();
        Minecraft mc = Minecraft.getInstance();
        if (previous != null) mc.options.setCameraType(previous);
        previous = null;
    }
}
