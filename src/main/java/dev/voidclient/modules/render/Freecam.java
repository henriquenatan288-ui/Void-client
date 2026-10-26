package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.network.ClientPlayerEntity;

/** Detached camera: the real player is not teleported or moved. */
public class Freecam extends Module {
    private final NumberSetting speed = addSetting(
            new NumberSetting("Speed", "Free camera speed", 0.8, 0.2, 3.0, 0.1));
    private Perspective previousPerspective;

    public Freecam() {
        super("Freecam", "Move a detached camera through blocks.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) return;
        previousPerspective = mc.options.getPerspective();
        CameraControl.startFreecam(player);
        mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
    }

    @Override
    protected void onDisable() {
        CameraControl.stopFreecam();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (previousPerspective != null) mc.options.setPerspective(previousPerspective);
        previousPerspective = null;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!CameraControl.isFreecamActive() || mc.player == null || mc.world == null) return;
        double forward = (mc.options.forwardKey.isPressed() ? 1.0 : 0.0)
                - (mc.options.backKey.isPressed() ? 1.0 : 0.0);
        double strafe = (mc.options.leftKey.isPressed() ? 1.0 : 0.0)
                - (mc.options.rightKey.isPressed() ? 1.0 : 0.0);
        double vertical = (mc.options.jumpKey.isPressed() ? 1.0 : 0.0)
                - (mc.options.sneakKey.isPressed() ? 1.0 : 0.0);
        CameraControl.moveCamera(forward, strafe, vertical, speed.getFloat());
    }
}
