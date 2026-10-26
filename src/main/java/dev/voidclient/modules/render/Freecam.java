package dev.voidclient.modules.render;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.NumberSetting;
import dev.voidclient.freecam.FreecamController;
import net.minecraft.client.Minecraft;

/**
 * Free camera using FreecamController — does not touch player physics/packets.
 */
public class Freecam extends Module {
    private final NumberSetting speed = addSetting(
            new NumberSetting("Speed", "Camera speed (blocks/sec)", 20.0, 5.0, 60.0, 1.0));
    private final NumberSetting sprintMult = addSetting(
            new NumberSetting("Sprint Mult", "Speed multiplier while sprinting", 3.0, 1.0, 6.0, 0.5));
    private final BooleanSetting disableOnDamage = addSetting(
            new BooleanSetting("Disable On Damage", "Stop freecam when hurt", true));
    private final BooleanSetting blockInteractions = addSetting(
            new BooleanSetting("Block Interactions", "Block attack/use while freecam", true));

    public Freecam() {
        super("Freecam", "Detached free camera (no player physics).", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            setEnabled(false);
            return;
        }
        // Stop freelook conflict
        if (CameraControl.isFreelookActive() && VoidClient.MODULE_MANAGER != null) {
            Module fl = VoidClient.MODULE_MANAGER.getByName("Freelook");
            if (fl != null && fl.isEnabled()) fl.disable();
        }
        // Stop old CameraControl freecam if any
        CameraControl.stopFreecam();

        FreecamController.baseSpeed = speed.get();
        FreecamController.sprintMult = sprintMult.get();
        FreecamController.disableOnDamage = disableOnDamage.get();
        FreecamController.blockInteractions = blockInteractions.get();
        FreecamController.onAutoStop = () -> {
            if (isEnabled()) setEnabled(false);
        };
        FreecamController.start(mc);
        VoidClient.LOGGER.info("FreecamController started");
    }

    @Override
    protected void onDisable() {
        FreecamController.stop();
        VoidClient.LOGGER.info("FreecamController stopped");
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        FreecamController.baseSpeed = speed.get();
        FreecamController.sprintMult = sprintMult.get();
        FreecamController.disableOnDamage = disableOnDamage.get();
        FreecamController.blockInteractions = blockInteractions.get();
        FreecamController.tick(Minecraft.getInstance());
    }
}
