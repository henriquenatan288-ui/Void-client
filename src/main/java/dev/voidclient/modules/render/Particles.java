package dev.voidclient.modules.render;

import dev.voidclient.VoidClient;
import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.ModeSetting;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

/** Cosmetic particle patterns around the local player, including on Minimal particle settings. */
public class Particles extends Module {
    private final ModeSetting mode = addSetting(new ModeSetting(
            "Style", "Particle style", "Crown", "Crown", "Sword", "Totem", "Mix", "Money"));
    private final NumberSetting moneyRadius = addSetting(
            new NumberSetting("Money Radius", "Orbit radius for the neon banknote", 0.85, 0.4, 1.8, 0.05));
    private final NumberSetting moneySpeed = addSetting(
            new NumberSetting("Money Speed", "Floating animation speed", 0.8, 0.2, 2.0, 0.1));
    private final NumberSetting moneyDensity = addSetting(
            new NumberSetting("Money Detail", "Number of points on the banknote outline", 3, 2, 6, 1));
    private int tick;
    private boolean loggedFirstSpawn;

    public Particles() {
        super("Particles", "Show a refreshed particle pattern around your player.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        tick = 0;
        loggedFirstSpawn = false;
        VoidClient.LOGGER.info("Particles enabled; style={}", mode.get());
    }

    @Override
    protected void onDisable() {
        loggedFirstSpawn = false;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;
        if (player == null || mc.world == null) return;
        if (++tick % 2 != 0) return;

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        switch (mode.get()) {
            case "Crown" -> crown(mc, x, y, z);
            case "Sword" -> sword(mc, x, y, z);
            case "Totem" -> totem(mc, x, y, z);
            case "Money" -> money(mc, x, y, z);
            case "Mix" -> {
                switch ((tick / 2) % 3) {
                    case 0 -> crown(mc, x, y, z);
                    case 1 -> sword(mc, x, y, z);
                    default -> totem(mc, x, y, z);
                }
            }
            default -> crown(mc, x, y, z);
        }
    }

    private void crown(MinecraftClient mc, double x, double y, double z) {
        double phase = tick * 0.22;
        // Wider than the player's head so some motes remain visible around the first-person view.
        for (int i = 0; i < 8; i++) {
            double angle = phase + i * (Math.PI * 2.0 / 8.0);
            emit(mc, ParticleTypes.END_ROD,
                    x + Math.cos(angle) * 0.62,
                    y + 1.48 + Math.sin(angle * 2.0) * 0.12,
                    z + Math.sin(angle) * 0.62,
                    0, 0.015, 0);
        }
    }

    private void sword(MinecraftClient mc, double x, double y, double z) {
        double phase = tick * 0.18;
        for (int i = 0; i < 6; i++) {
            double angle = phase + i * (Math.PI * 2.0 / 6.0);
            emit(mc, ParticleTypes.CRIT,
                    x + Math.cos(angle) * 0.55,
                    y + 0.95 + i * 0.1,
                    z + Math.sin(angle) * 0.55,
                    0, 0.04, 0);
        }
        emit(mc, ParticleTypes.ENCHANTED_HIT, x, y + 1.15, z, 0, 0.04, 0);
    }

    private void totem(MinecraftClient mc, double x, double y, double z) {
        for (int i = 0; i < 8; i++) {
            double angle = tick * 0.16 + i * (Math.PI * 2.0 / 8.0);
            emit(mc, ParticleTypes.TOTEM_OF_UNDYING,
                    x + Math.cos(angle) * 0.65,
                    y + 0.45 + (i % 3) * 0.35,
                    z + Math.sin(angle) * 0.65,
                    0, 0.12, 0);
        }
    }

    private void money(MinecraftClient mc, double x, double y, double z) {
        double phase = tick * 0.055 * moneySpeed.get();
        double orbit = moneyRadius.get();
        double cx = x + Math.cos(phase) * orbit;
        double cy = y + 1.15 + Math.sin(phase * 1.7) * 0.24;
        double cz = z + Math.sin(phase) * orbit;
        double rotation = phase * 1.8 + Math.PI * 0.15;
        double rightX = Math.cos(rotation), rightZ = Math.sin(rotation);
        double halfW = 0.25, halfH = 0.15;
        int detail = moneyDensity.getInt();
        DustParticleEffect neonGreen = new DustParticleEffect(0x53FF9D, 0.58f);

        // Four glowing edges of a small rectangular banknote.
        for (int edge = 0; edge < 4; edge++) {
            double[][] corners = {
                    {-halfW, -halfH}, {halfW, -halfH},
                    {halfW, halfH}, {-halfW, halfH}
            };
            double[] a = corners[edge], b = corners[(edge + 1) % 4];
            for (int point = 0; point < detail; point++) {
                double t = point / (double) detail;
                double localX = a[0] + (b[0] - a[0]) * t;
                double localY = a[1] + (b[1] - a[1]) * t;
                emit(mc, neonGreen,
                        cx + rightX * localX, cy + localY, cz + rightZ * localX,
                        -rightX * 0.008, 0.008, -rightZ * 0.008);
            }
        }

        // Minimal center emblem and inner stripe to make the silhouette read as a banknote.
        for (int point = 0; point < 4; point++) {
            double angle = Math.PI * 0.5 * point + rotation;
            emit(mc, neonGreen,
                    cx + Math.cos(angle) * 0.075, cy, cz + Math.sin(angle) * 0.075,
                    0, 0.006, 0);
        }
        for (int point = 0; point < detail; point++) {
            double t = point / (double) Math.max(1, detail - 1);
            emit(mc, neonGreen,
                    cx + rightX * ((t - 0.5) * 0.25), cy + halfH * 0.55,
                    cz + rightZ * ((t - 0.5) * 0.25), 0, 0.004, 0);
        }
    }

    private void emit(MinecraftClient mc, ParticleEffect effect,
                      double x, double y, double z,
                      double velocityX, double velocityY, double velocityZ) {
        // Yarn 1.21.11 supports (effect, force, canSpawnOnMinimal, ...).
        // These cosmetic effects should not disappear solely because particles are set to Minimal.
        mc.world.addParticleClient(effect, true, true,
                x, y, z, velocityX, velocityY, velocityZ);
        if (!loggedFirstSpawn) {
            loggedFirstSpawn = true;
            VoidClient.LOGGER.info("Particles emitted first effect={} at {},{},{}",
                    effect.getType(), x, y, z);
        }
    }
}
