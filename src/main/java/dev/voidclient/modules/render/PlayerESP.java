package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.Box;

/**
 * Highlights other players with particles + optional nametag on HUD.
 */
public class PlayerESP extends Module {

    private final NumberSetting range = addSetting(
            new NumberSetting("Range", "Max distance", 64, 16, 128, 1));

    private final ColorSetting color = addSetting(
            new ColorSetting("Color", "ESP color", 0xFF00FFAA));

    private final BooleanSetting nametags = addSetting(
            new BooleanSetting("Nametags", "Show names on HUD", true));

    public PlayerESP() {
        super("PlayerESP", "Highlight players nearby.", Category.RENDER);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        int rgb = color.get() & 0x00FFFFFF;
        DustParticleEffect dust = new DustParticleEffect(rgb, 1.2f);
        double max = range.get();

        for (PlayerEntity other : mc.world.getPlayers()) {
            if (other == mc.player) continue;
            if (mc.player.squaredDistanceTo(other) > max * max) continue;

            Box box = other.getBoundingBox();
            // corners of the hitbox
            double[] xs = {box.minX, box.maxX};
            double[] ys = {box.minY, box.maxY};
            double[] zs = {box.minZ, box.maxZ};
            for (double x : xs) {
                for (double y : ys) {
                    for (double z : zs) {
                        mc.world.addParticleClient(dust, x, y, z, 0, 0, 0);
                    }
                }
            }
            // center glow
            mc.world.addParticleClient(dust,
                    other.getX(), other.getBodyY(0.5), other.getZ(), 0, 0.02, 0);
        }
    }

    public void renderHud(DrawContext ctx, float tickDelta) {
        if (!nametags.get()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.textRenderer == null) return;

        int y = 54;
        double max = range.get();
        for (PlayerEntity other : mc.world.getPlayers()) {
            if (other == mc.player) continue;
            double dist = Math.sqrt(mc.player.squaredDistanceTo(other));
            if (dist > max) continue;
            String line = other.getName().getString() + " [" + (int) dist + "m]";
            ctx.drawText(mc.textRenderer, line, 4, y, color.get(), true);
            y += 10;
        }
    }
}
