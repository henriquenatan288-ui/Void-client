package dev.voidclient.modules.render;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Expanding rings triggered when the locally tracked block changes to air.
 *  Uses world time (continuous) instead of discrete tick counters for smooth growth. */
public class BreakWaves extends Module {
    private final NumberSetting waves = addSetting(
            new NumberSetting("Waves", "Number of expanding rings", 3, 1, 6, 1));
    private final ColorSetting color = addSetting(
            new ColorSetting("Color", "Wave color", 0xFFFFFFFF));

    private final List<Wave> active = new ArrayList<>();
    private static BlockPos trackedTarget;
    private static ClientWorld trackedWorld;
    private static long targetTrackedAt = Long.MIN_VALUE;

    public BreakWaves() {
        super("BreakWaves", "Show expanding rings when you break a block.", Category.RENDER);
    }

    public static void trackBlockBreakTarget(BlockPos pos) {
        if (pos == null || VoidClient.MODULE_MANAGER == null) return;
        Module module = VoidClient.MODULE_MANAGER.getByName("BreakWaves");
        if (!(module instanceof BreakWaves breakWaves) || !breakWaves.isEnabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        BlockPos immutablePos = pos.toImmutable();
        if (!immutablePos.equals(trackedTarget) || trackedWorld != mc.world) {
            VoidClient.LOGGER.info("BreakWaves tracking mining target {}", immutablePos);
        }
        trackedTarget = immutablePos;
        trackedWorld = mc.world;
        targetTrackedAt = mc.world.getTime();
    }

    public static void onClientBlockRemoved(BlockPos pos) {
        if (pos == null || trackedTarget == null || !trackedTarget.equals(pos)) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.world != trackedWorld) {
            clearTrackedTarget();
            return;
        }

        long age = mc.world.getTime() - targetTrackedAt;
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
        MinecraftClient mc = MinecraftClient.getInstance();
        long now = mc.world != null ? mc.world.getTime() : 0L;
        active.add(new Wave(pos.getX() + 0.5, pos.getY() + 0.05, pos.getZ() + 0.5, now));
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
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) {
            active.clear();
            clearTrackedTarget();
            return;
        }

        if (trackedTarget != null && (trackedWorld != mc.world
                || mc.world.getTime() - targetTrackedAt > 40)) {
            clearTrackedTarget();
        }
        if (active.isEmpty()) return;

        long now = mc.world.getTime();
        DustParticleEffect dust = new DustParticleEffect(color.getRgb(), 0.9f);
        int maxWaves = waves.getInt();
        Iterator<Wave> iterator = active.iterator();
        while (iterator.hasNext()) {
            Wave wave = iterator.next();
            // Continuous age from world time → smooth radius growth
            float age = (float) (now - wave.bornAt);

            for (int ring = 0; ring < maxWaves; ring++) {
                float ringAge = age - ring * 3.0f;
                if (ringAge < 0 || ringAge > 12) continue;
                double radius = ringAge * 0.32;
                int points = 14;
                for (int i = 0; i < points; i++) {
                    double angle = Math.PI * 2.0 * i / points;
                    mc.world.addParticleClient(dust, true, true,
                            wave.x + Math.cos(angle) * radius,
                            wave.y + 0.03,
                            wave.z + Math.sin(angle) * radius,
                            0, 0.005, 0);
                }
            }
            if (age > 12 + maxWaves * 3) iterator.remove();
        }
    }

    private static final class Wave {
        final double x, y, z;
        final long bornAt;
        Wave(double x, double y, double z, long bornAt) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.bornAt = bornAt;
        }
    }
}
