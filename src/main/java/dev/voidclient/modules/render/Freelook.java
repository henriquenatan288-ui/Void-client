package dev.voidclient.modules.render;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.NumberSetting;
import dev.voidclient.freecam.FreecamController;
import dev.voidclient.freelook.FreelookController;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

public class Freelook extends Module {
    private final NumberSetting sensitivity = addSetting(
            new NumberSetting("Sensitivity", "Look sensitivity", 0.15, 0.05, 0.40, 0.01));
    private final BooleanSetting passThrough = addSetting(
            new BooleanSetting("Pass through", "Camera goes through walls (3rd person)", true));

    private CameraType previousPerspective;

    public Freelook() {
        super("Freelook", "Rotate camera without turning the player body.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            setEnabled(false);
            return;
        }
        if (FreecamController.isActive()) {
            setEnabled(false);
            return;
        }

        previousPerspective = mc.options.getCameraType();
        if (previousPerspective == CameraType.FIRST_PERSON) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }

        FreelookController.sensitivity = sensitivity.getFloat();
        FreelookController.passThrough = passThrough.get();
        FreelookController.onAutoStop = () -> {
            if (isEnabled()) setEnabled(false);
        };
        CameraControl.stopFreelook();
        FreelookController.start(mc);
    }

    @Override
    protected void onDisable() {
        FreelookController.stop();
        Minecraft mc = Minecraft.getInstance();
        if (previousPerspective != null && mc.options != null) {
            mc.options.setCameraType(previousPerspective);
        }
        previousPerspective = null;
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        FreelookController.sensitivity = sensitivity.getFloat();
        FreelookController.passThrough = passThrough.get();
        FreelookController.tick(Minecraft.getInstance());
    }
}
