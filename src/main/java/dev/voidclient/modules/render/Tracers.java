package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.NumberSetting;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;

/** Player tracers are rendered as interpolated 3D lines; no particles are spawned. */
public class Tracers extends Module {
    private final NumberSetting range = addSetting(
            new NumberSetting("Range", "Maximum target distance", 64, 8, 128, 4));
    private final ColorSetting color = addSetting(
            new ColorSetting("Color", "Base tracer color for nearby targets", 0xFF00FFAA));
    private final BooleanSetting distanceColors = addSetting(
            new BooleanSetting("Distance Colors", "Blend from the selected color nearby to red at maximum range", true));
    private List<AbstractClientPlayer> targets = List.of();

    public Tracers() {
        super("Tracers", "Draw smooth 3D lines from the camera to nearby players.", Category.RENDER);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            targets = List.of();
            return;
        }
        double maxSquared = range.get() * range.get();
        targets = mc.level.players().stream()
                .filter(other -> other != mc.player)
                .filter(other -> mc.player.distanceToSqr(other) <= maxSquared)
                .sorted(Comparator.comparingDouble(other -> mc.player.distanceToSqr(other)))
                .limit(32)
                .toList();
    }

    @Override
    protected void onDisable() { targets = List.of(); }

    public List<AbstractClientPlayer> getTargets() { return targets; }
    public int getTracerColor() { return color.get(); }

    public int getTracerColor(AbstractClientPlayer target) {
        int base = color.get();
        Minecraft mc = Minecraft.getInstance();
        if (!distanceColors.isEnabled() || mc.player == null || target == null) return base;

        double maxDistance = Math.max(1.0, range.get());
        double distance = Math.sqrt(mc.player.distanceToSqr(target));
        double blend = Math.max(0.0, Math.min(1.0, distance / maxDistance));

        int alpha = (base >>> 24) & 0xFF;
        int red = blend((base >>> 16) & 0xFF, 255, blend);
        int green = blend((base >>> 8) & 0xFF, 64, blend);
        int blue = blend(base & 0xFF, 64, blend);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int blend(int start, int end, double amount) {
        return (int) Math.round(start + (end - start) * amount);
    }
}
