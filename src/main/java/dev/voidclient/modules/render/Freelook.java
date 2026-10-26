package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.network.ClientPlayerEntity;

public class Freelook extends Module {
    private Perspective previous;

    public Freelook() {
        super("Freelook", "Rotate the third-person camera independently from the player.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;
        previous = mc.options.getPerspective();
        CameraControl.startFreelook(player);
        mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
    }

    @Override
    protected void onDisable() {
        CameraControl.stopFreelook();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (previous != null) mc.options.setPerspective(previous);
        previous = null;
    }
}
