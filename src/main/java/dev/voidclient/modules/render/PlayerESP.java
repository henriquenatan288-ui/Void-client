package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.NumberSetting;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;

/** Player ESP data is cached per client tick; the world renderer draws boxes without particle spam. */
public class PlayerESP extends Module {
    private final NumberSetting range = addSetting(
            new NumberSetting("Range", "Max distance", 64, 16, 128, 1));
    private final ColorSetting color = addSetting(
            new ColorSetting("Color", "ESP color", 0xFF00FFAA));
    private final BooleanSetting nametags = addSetting(
            new BooleanSetting("Nametags", "Show names on HUD", true));
    private List<AbstractClientPlayer> targets = List.of();

    public PlayerESP() {
        super("PlayerESP", "Draw 3D boxes around nearby players.", Category.RENDER);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            targets = List.of();
            return;
        }
        double maxDistanceSquared = range.get() * range.get();
        targets = mc.level.players().stream()
                .filter(other -> other != mc.player)
                .filter(other -> mc.player.distanceToSqr(other) <= maxDistanceSquared)
                .sorted(Comparator.comparingDouble(other -> mc.player.distanceToSqr(other)))
                .limit(64)
                .toList();
    }

    @Override
    protected void onDisable() { targets = List.of(); }

    public List<AbstractClientPlayer> getTargets() { return targets; }
    public int getEspColor() { return color.get(); }

    public boolean isTarget(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.player == null || !(entity instanceof AbstractClientPlayer other)
                || other == mc.player) return false;
        double max = range.get();
        return mc.player.distanceToSqr(other) <= max * max;
    }

    public void renderHud(GuiGraphics ctx, float tickDelta) {
        if (!nametags.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.font == null) return;
        int y = 54;
        for (AbstractClientPlayer other : targets) {
            double distance = Math.sqrt(mc.player.distanceToSqr(other));
            String label = other.getName().getString() + " [" + (int) distance + "m]";
            ctx.drawString(mc.font, label, 4, y, color.get(), true);
            y += 10;
        }
    }
}
