package dev.voidclient.modules.render;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class Freelook extends Module {
    private CameraType previous;

    public Freelook() {
        super("Freelook", "Rotate camera without turning the player.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            setEnabled(false);
            return;
        }
        if (CameraControl.isFreecamActive()) {
            setEnabled(false);
            return;
        }
        previous = mc.options.getCameraType();
        CameraControl.startFreelook(player);
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        VoidClient.LOGGER.info("Freelook enabled");
    }

    @Override
    protected void onDisable() {
        CameraControl.stopFreelook();
        Minecraft mc = Minecraft.getInstance();
        if (previous != null && mc.options != null) {
            mc.options.setCameraType(previous);
        }
        previous = null;
        VoidClient.LOGGER.info("Freelook disabled");
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.player.isDeadOrDying()) {
            if (isEnabled()) setEnabled(false);
            return;
        }
        if (!CameraControl.isFreelookActive()) return;

        if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        CameraControl.tickCapturePlayerLook(mc.player);
    }
}
