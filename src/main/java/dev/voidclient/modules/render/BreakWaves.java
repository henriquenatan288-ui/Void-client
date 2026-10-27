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
 * Expanding hologram waves when a tracked block breaks (survival continuous + creative).
 * Drawn by the single VoidDraw extraction/render pipeline.
 */
public class BreakWaves extends Module {
    private final NumberSetting waves = addSetting(
            new NumberSetting("Waves", "Ring layers", 4, 1, 8, 1));
    private final NumberSetting maxAge = addSetting(
            new NumberSetting("Duration", "Lifetime ticks", 22, 8, 50, 1));
    private final ColorSetting color = addSetting(
            new ColorSetting("Color", "Hologram color", 0xCC00FFCC));

    private final List<Wave> active = new ArrayList<>();
    private static BlockPos trackedTarget;
    private static ClientLevel trackedWorld;
    private static long targetTrackedAt = Long.MIN_VALUE;

    public BreakWaves() {
        super("BreakWaves", "Hologram shockwave when you break blocks.", Category.RENDER);
    }

    public static void trackBlockBreakTarget(BlockPos pos) {
        trackBlockBreakTarget(pos, null);
    }

    public static void trackBlockBreakTarget(BlockPos pos, Direction face) {
        if (pos == null || VoidClient.MODULE_MANAGER == null) return;
        Module module = VoidClient.MODULE_MANAGER.getByName("BreakWaves");
        if (!(module instanceof BreakWaves) || !module.isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        trackedTarget = pos.immutable();
        trackedWorld = mc.level;
        targetTrackedAt = mc.level.getGameTime();
        VoidClient.LOGGER.debug("BreakWaves track {} face={}", trackedTarget, face);
    }

    public static void onClientBlockRemoved(BlockPos pos) {
        if (pos == null || trackedTarget == null || !trackedTarget.equals(pos)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level != trackedWorld) {
            clearTrackedTarget();
            return;
        }

        long age = mc.level.getGameTime() - targetTrackedAt;
        if (age < 0 || age > 50) {
            clearTrackedTarget();
            return;
        }

        Module module = VoidClient.MODULE_MANAGER == null
                ? null : VoidClient.MODULE_MANAGER.getByName("BreakWaves");
        if (module instanceof BreakWaves breakWaves && breakWaves.isEnabled()) {
            breakWaves.spawn(pos);
            VoidClient.LOGGER.info("BreakWaves SPAWN at {} age={}t", pos, age);
        }
        clearTrackedTarget();
    }

    private static void clearTrackedTarget() {
        trackedTarget = null;
        trackedWorld = null;
        targetTrackedAt = Long.MIN_VALUE;
    }

    private void spawn(BlockPos pos) {
        if (active.size() >= 8) active.remove(0);
        active.add(new Wave(pos.getX() + 0.5, pos.getY() + 0.08, pos.getZ() + 0.5));
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
                || mc.level.getGameTime() - targetTrackedAt > 50)) {
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

    public List<Wave> getActiveWaves() {
        return Collections.unmodifiableList(active);
    }

    public int getWaveColor() { return color.get(); }
    public int getWaveCount() { return waves.getInt(); }
    public int getMaxAge() { return maxAge.getInt(); }

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
