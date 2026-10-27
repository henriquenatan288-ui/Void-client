package dev.voidclient.modules.visual;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.ModeSetting;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

/**
 * Low-overhead ambient particle patterns adapted to the native 1.21.11 client particle API.
 * Minecraft owns each particle's animation and fade-out lifecycle.
 */
public final class WorldFx extends Module {
    private final ModeSetting style = addSetting(new ModeSetting(
            "Style", "Ambient effect", "Fireflies",
            "Fireflies", "Embers", "Leaves", "Ash", "Pollen", "Wind"));
    private final NumberSetting density = addSetting(
            new NumberSetting("Density", "Particles emitted per update", 6, 2, 14, 1));
    private final NumberSetting radius = addSetting(
            new NumberSetting("Radius", "Area around the player", 2.8, 1.0, 6.0, 0.2));
    private final NumberSetting speed = addSetting(
            new NumberSetting("Speed", "Particle movement speed", 0.7, 0.1, 2.0, 0.1));

    private int tick;
    private long randomState = 0x6A09E667F3BCC909L;

    public WorldFx() {
        super("WorldFx", "Smooth ambient particles around the player.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        tick = 0;
        randomState = 0x6A09E667F3BCC909L ^ System.nanoTime();
    }

    @Override
    protected void onDisable() {
        tick = 0;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (++tick % 2 != 0) return;

        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        double r = radius.get();
        double velocity = speed.get();
        int count = density.getInt();
        String selected = style.get();

        for (int i = 0; i < count; i++) {
            double angle = nextDouble() * Math.PI * 2.0;
            double distance = Math.sqrt(nextDouble()) * r;
            double x = px + Math.cos(angle) * distance;
            double y = py + 0.35 + nextDouble() * 2.1;
            double z = pz + Math.sin(angle) * distance;
            double vx = (nextDouble() - 0.5) * 0.025 * velocity;
            double vy = (0.006 + nextDouble() * 0.018) * velocity;
            double vz = (nextDouble() - 0.5) * 0.025 * velocity;

            switch (selected) {
                case "Embers" -> emit(mc, ParticleTypes.FLAME, x, y, z, vx, vy + 0.015, vz);
                case "Leaves" -> emit(mc, ParticleTypes.COMPOSTER, x, y, z, vx, vy * 0.45, vz);
                case "Ash" -> emit(mc, ParticleTypes.ASH, x, y, z, vx * 0.7, vy * 0.4, vz * 0.7);
                case "Pollen" -> emit(mc, ParticleTypes.HAPPY_VILLAGER, x, y, z, vx, vy, vz);
                case "Wind" -> emit(mc, ParticleTypes.CLOUD, x, y, z, vx * 2.5, vy * 0.25, vz * 2.5);
                default -> {
                    int color = (i % 3 == 0) ? 0xFF51FF9B : (i % 3 == 1 ? 0xFF65DFFF : 0xFFD18BFF);
                    emit(mc, new DustParticleEffect(color, 0.72f), x, y, z, vx, vy, vz);
                }
            }
        }
    }

    private void emit(MinecraftClient mc, ParticleEffect type, double x, double y, double z,
                      double vx, double vy, double vz) {
        mc.world.addParticleClient(type, true, true, x, y, z, vx, vy, vz);
    }

    private double nextDouble() {
        randomState ^= randomState << 13;
        randomState ^= randomState >>> 7;
        randomState ^= randomState << 17;
        return (randomState >>> 11) * 0x1.0p-53;
    }
}