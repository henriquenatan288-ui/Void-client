package dev.voidclient.modules.render;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.NumberSetting;
import dev.voidclient.freecam.FreecamController;
import dev.voidclient.freelook.FreelookController;
import net.minecraft.client.Minecraft;

/** Freecam v2: smooth look, sprint ramp, optional collision. */
public class Freecam extends Module {
    private final NumberSetting speed = addSetting(
            new NumberSetting("Speed", "Max camera speed (blocks/sec)", 20.0, 5.0, 80.0, 1.0));
    private final NumberSetting sprintMult = addSetting(
            new NumberSetting("Sprint Mult", "Speed while sprinting", 3.0, 1.0, 8.0, 0.5));
    private final NumberSetting accel = addSetting(
            new NumberSetting("Accel", "Acceleration (higher = snappier)", 12.0, 3.0, 30.0, 1.0));
    private final BooleanSetting smoothLook = addSetting(
            new BooleanSetting("Smooth Look", "Soften touch/mouse look (no stepping)", true));
    private final NumberSetting lookSmoothing = addSetting(
            new NumberSetting("Look Smoothing", "Higher = follows your finger tighter", 22.0, 8.0, 40.0, 1.0));
    private final BooleanSetting collide = addSetting(
            new BooleanSetting("Collision", "Camera stops at blocks (off = noclip)", false));
    private final BooleanSetting verticalLock = addSetting(
            new BooleanSetting("Vertical Lock", "Keep altitude (no up/down from look)", false));
    private final BooleanSetting disableOnDamage = addSetting(
            new BooleanSetting("Disable On Damage", "Stop freecam when hurt", true));
    private final BooleanSetting blockInteractions = addSetting(
            new BooleanSetting("Block Interactions", "Block attack/use while freecam", true));

    public Freecam() {
        super("Freecam", "Smooth detached free camera.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            setEnabled(false);
            return;
        }
        if (FreelookController.isActive() && VoidClient.MODULE_MANAGER != null) {
            Module fl = VoidClient.MODULE_MANAGER.getByName("Freelook");
            if (fl != null && fl.isEnabled()) fl.disable();
        }
        applySettings();
        FreecamController.onAutoStop = () -> {
            if (isEnabled()) setEnabled(false);
        };
        FreecamController.start(mc);
    }

    @Override
    protected void onDisable() {
        FreecamController.stop();
    }

    @Override
    public void onWorldUnload() {
        if (isEnabled()) disable();
        else FreecamController.stop();
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        applySettings();
        FreecamController.tick(Minecraft.getInstance());
    }

    private void applySettings() {
        FreecamController.baseSpeed = speed.get();
        FreecamController.sprintMult = sprintMult.get();
        FreecamController.accel = accel.get();
        FreecamController.smoothLook = smoothLook.get();
        FreecamController.lookSmoothing = lookSmoothing.get();
        FreecamController.collide = collide.get();
        FreecamController.verticalLock = verticalLock.get();
        FreecamController.disableOnDamage = disableOnDamage.get();
        FreecamController.blockInteractions = blockInteractions.get();
    }
}
