package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import java.util.Comparator;
import java.util.List;

/** Player ESP data is cached per client tick; the world renderer draws boxes without particle spam. */
public class PlayerESP extends Module {
    private final NumberSetting range = addSetting(
            new NumberSetting("Range", "Max distance", 64, 16, 128, 1));
    private final ColorSetting color = addSetting(
            new ColorSetting("Color", "ESP color", 0xFF00FFAA));
    private final BooleanSetting nametags = addSetting(
            new BooleanSetting("Nametags", "Show names on HUD", true));
    private List<AbstractClientPlayerEntity> targets = List.of();

    public PlayerESP() {
        super("PlayerESP", "Draw 3D boxes around nearby players.", Category.RENDER);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            targets = List.of();
            return;
        }
        double maxDistanceSquared = range.get() * range.get();
        targets = mc.world.getPlayers().stream()
                .filter(other -> other != mc.player)
                .filter(other -> mc.player.squaredDistanceTo(other) <= maxDistanceSquared)
                .sorted(Comparator.comparingDouble(other -> mc.player.squaredDistanceTo(other)))
                .limit(64)
                .toList();
    }

    @Override
    protected void onDisable() { targets = List.of(); }

    public List<AbstractClientPlayerEntity> getTargets() { return targets; }
    public int getEspColor() { return color.get(); }

    public boolean isTarget(Entity entity) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!isEnabled() || mc.player == null || !(entity instanceof AbstractClientPlayerEntity other)
                || other == mc.player) return false;
        double max = range.get();
        return mc.player.squaredDistanceTo(other) <= max * max;
    }

    public void renderHud(DrawContext ctx, float tickDelta) {
        if (!nametags.get()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.textRenderer == null) return;
        int y = 54;
        for (AbstractClientPlayerEntity other : targets) {
            double distance = Math.sqrt(mc.player.squaredDistanceTo(other));
            String label = other.getName().getString() + " [" + (int) distance + "m]";
            ctx.drawText(mc.textRenderer, label, 4, y, color.get(), true);
            y += 10;
        }
    }
}
