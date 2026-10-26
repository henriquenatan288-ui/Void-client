package dev.voidclient.modules.render;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.NumberSetting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Hologram rings (through walls) when the player breaks a block downward.
 * Drawn by VoidWorldRenderer using the no-depth line pipeline.
 */
public class BreakWaves extends Module {
    private final NumberSetting waves = addSetting(
            new NumberSetting("Waves", "Number of expanding rings", 3, 1, 6, 1));
    private final NumberSetting maxAge = addSetting(
            new NumberSetting("Duration", "How long each wave lives (ticks)", 18, 8, 40, 1));
    private final ColorSetting color = addSetting(
            new ColorSetting("Color", "Hologram color", 0xAA00FFCC));

    private final List<Wave> active = new ArrayList<>();
    private static BlockPos trackedTarget;
    private static ClientLevel trackedWorld;
    private static long targetTrackedAt = Long.MIN_VALUE;
    private static boolean trackedDownward;

    public BreakWaves() {
        super("BreakWaves", "Hologram rings when you break a block downward.", Category.RENDER);
    }

    /** Called while mining. Only keep targets broken roughly downward. */
    public static void trackBlockBreakTarget(BlockPos pos) {
        if (pos == null || VoidClient.MODULE_MANAGER == null) return;
        Module module = VoidClient.MODULE_MANAGER.getByName("BreakWaves");
        if (!(module instanceof BreakWaves breakWaves) || !breakWaves.isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // Downward = block is at or below the player's feet / looking down
        boolean downward = pos.getY() <= mc.player.blockPosition().getY()
                || mc.player.getXRot() > 25.0f;

        BlockPos immutablePos = pos.immutable();
        trackedTarget = immutablePos;
        trackedWorld = mc.level;
        targetTrackedAt = mc.level.getGameTime();
        trackedDownward = downward;
    }

    /** Also called from the interaction mixin with face direction when available. */
    public static void trackBlockBreakTarget(BlockPos pos, Direction face) {
        if (pos == null || VoidClient.MODULE_MANAGER == null) return;
        Module module = VoidClient.MODULE_MANAGER.getByName("BreakWaves");
        if (!(module instanceof BreakWaves breakWaves) || !breakWaves.isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        boolean downward = face == Direction.DOWN
                || pos.getY() <= mc.player.blockPosition().getY()
                || mc.player.getXRot() > 25.0f;

        trackedTarget = pos.immutable();
        trackedWorld = mc.level;
        targetTrackedAt = mc.level.getGameTime();
        trackedDownward = downward;
    }

    public static void onClientBlockRemoved(BlockPos pos) {
        if (pos == null || trackedTarget == null || !trackedTarget.equals(pos)) return;
        if (!trackedDownward) {
            clearTrackedTarget();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level != trackedWorld) {
            clearTrackedTarget();
            return;
        }

        long age = mc.level.getGameTime() - targetTrackedAt;
        if (age < 0 || age > 40) {
            clearTrackedTarget();
            return;
        }

        Module module = VoidClient.MODULE_MANAGER == null
                ? null : VoidClient.MODULE_MANAGER.getByName("BreakWaves");
        if (module instanceof BreakWaves breakWaves && breakWaves.isEnabled()) {
            breakWaves.spawn(pos);
        }
        clearTrackedTarget();
    }

    private static void clearTrackedTarget() {
        trackedTarget = null;
        trackedWorld = null;
        targetTrackedAt = Long.MIN_VALUE;
        trackedDownward = false;
    }

    private void spawn(BlockPos pos) {
        if (active.size() >= 6) active.remove(0);
        active.add(new Wave(pos.getX() + 0.5, pos.getY() + 0.05, pos.getZ() + 0.5));
        VoidClient.LOGGER.info("BreakWaves hologram at {}", pos);
    }

    @Override
    protected void onEnable() {
        active.clear();
        clearTrackedTarget();
    }

    @Override
    protected void onDisable() {
        active.clear();
        clearTrackedTarget();
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            active.clear();
            clearTrackedTarget();
            return;
        }

        if (trackedTarget != null && (trackedWorld != mc.level
                || mc.level.getGameTime() - targetTrackedAt > 40)) {
            clearTrackedTarget();
        }

        int life = maxAge.getInt();
        Iterator<Wave> it = active.iterator();
        while (it.hasNext()) {
            Wave wave = it.next();
            wave.age++;
            if (wave.age > life) it.remove();
        }
    }

    /** Snapshot for the 3D renderer (hologram rings). */
    public List<Wave> getActiveWaves() {
        return Collections.unmodifiableList(active);
    }

    public int getWaveColor() {
        return color.get();
    }

    public int getWaveCount() {
        return waves.getInt();
    }

    public int getMaxAge() {
        return maxAge.getInt();
    }

    public static final class Wave {
        public final double x, y, z;
        public int age;

        Wave(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
