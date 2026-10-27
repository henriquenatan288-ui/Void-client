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

/** Expanding hologram waves — age is game-time based for smooth partialTick animation. */
public class BreakWaves extends Module {
    private final NumberSetting waves = addSetting(
            new NumberSetting("Waves", "Ring layers", 4, 1, 8, 1));
    private final NumberSetting maxAge = addSetting(
            new NumberSetting("Duration", "Life ticks", 22, 8, 50, 1));
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
            breakWaves.spawn(pos, mc.level.getGameTime());
        }
        clearTrackedTarget();
    }

    private static void clearTrackedTarget() {
        trackedTarget = null;
        trackedWorld = null;
        targetTrackedAt = Long.MIN_VALUE;
    }

    private void spawn(BlockPos pos, long now) {
        if (active.size() >= 8) active.remove(0);
        active.add(new Wave(pos.getX() + 0.5, pos.getY() + 0.08, pos.getZ() + 0.5, now));
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
    public void onWorldUnload() {
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
        long now = mc.level.getGameTime();
        int life = maxAge.getInt();
        Iterator<Wave> it = active.iterator();
        while (it.hasNext()) {
            Wave wave = it.next();
            if (now - wave.born > life) it.remove();
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
        public final long born;
        Wave(double x, double y, double z, long born) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.born = born;
        }
    }
}
