package dev.voidclient.modules.render;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.NumberSetting;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;

/** Expanding rings triggered when the locally tracked block changes to air. */
public class BreakWaves extends Module {
    private final NumberSetting waves = addSetting(
            new NumberSetting("Waves", "Number of expanding rings", 3, 1, 6, 1));
    private final ColorSetting color = addSetting(
            new ColorSetting("Color", "Wave color", 0xFFFFFFFF));

    private final List<Wave> active = new ArrayList<>();
    private static BlockPos trackedTarget;
    private static ClientLevel trackedWorld;
    private static long targetTrackedAt = Long.MIN_VALUE;

    public BreakWaves() {
        super("BreakWaves", "Show expanding rings when you break a block.", Category.RENDER);
    }

    /** Called by interaction-manager hooks while the local player is mining a block. */
    public static void trackBlockBreakTarget(BlockPos pos) {
        if (pos == null || VoidClient.MODULE_MANAGER == null) return;
        Module module = VoidClient.MODULE_MANAGER.getByName("BreakWaves");
        if (!(module instanceof BreakWaves breakWaves) || !breakWaves.isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        BlockPos immutablePos = pos.immutable();
        if (!immutablePos.equals(trackedTarget) || trackedWorld != mc.level) {
            VoidClient.LOGGER.info("BreakWaves tracking mining target {}", immutablePos);
        }
        trackedTarget = immutablePos;
        trackedWorld = mc.level;
        targetTrackedAt = mc.level.getGameTime();
    }

    /**
     * Called before ClientWorld applies a block-state update. This catches survival mining
     * too, where the server confirms the break asynchronously rather than in breakBlock().
     */
    public static void onClientBlockRemoved(BlockPos pos) {
        if (pos == null || trackedTarget == null || !trackedTarget.equals(pos)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level != trackedWorld) {
            clearTrackedTarget();
            return;
        }

        long age = mc.level.getGameTime() - targetTrackedAt;
        VoidClient.LOGGER.info("BreakWaves saw air update at tracked block {}, age={} ticks", pos, age);
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
    }

    private void spawn(BlockPos pos) {
        if (active.size() >= 4) active.remove(0);
        active.add(new Wave(pos.getX() + 0.5, pos.getY() + 0.05, pos.getZ() + 0.5));
        VoidClient.LOGGER.info("BreakWaves: created wave at {}", pos);
    }

    @Override
    protected void onEnable() {
        active.clear();
        clearTrackedTarget();
        VoidClient.LOGGER.info("BreakWaves enabled");
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
        if (active.isEmpty()) return;

        DustParticleOptions dust = new DustParticleOptions(color.getRgb(), 0.9f);
        int maxWaves = waves.getInt();
        Iterator<Wave> iterator = active.iterator();
        while (iterator.hasNext()) {
            Wave wave = iterator.next();
            wave.age++;

            for (int ring = 0; ring < maxWaves; ring++) {
                int age = wave.age - ring * 3;
                if (age < 0 || age > 12) continue;
                double radius = age * 0.32;
                int points = 14;
                for (int i = 0; i < points; i++) {
                    double angle = Math.PI * 2.0 * i / points;
                    mc.level.addParticle(dust, true, true,
                            wave.x + Math.cos(angle) * radius,
                            wave.y + 0.03,
                            wave.z + Math.sin(angle) * radius,
                            0, 0.005, 0);
                }
            }
            if (wave.age > 12 + maxWaves * 3) iterator.remove();
        }
    }

    private static final class Wave {
        final double x, y, z;
        int age;
        Wave(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
