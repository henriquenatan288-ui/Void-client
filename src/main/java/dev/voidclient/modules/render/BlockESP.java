package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.ModeSetting;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Highlights selected blocks nearby (particle outline + tracers).
 */
public class BlockESP extends Module {

    private final ModeSetting preset = addSetting(new ModeSetting(
            "Blocks", "Which blocks to highlight",
            "Ores",
            "Ores", "Chests", "Spawners", "Portals", "All Utility"));

    private final NumberSetting range = addSetting(
            new NumberSetting("Range", "Scan radius", 32, 8, 64, 1));

    private final ColorSetting color = addSetting(
            new ColorSetting("Color", "ESP color", 0xFFAA00FF));

    private final BooleanSetting tracers = addSetting(
            new BooleanSetting("Tracers", "Particle tracers to blocks", true));

    private final List<BlockPos> found = new ArrayList<>();
    private int scanTicker;

    public BlockESP() {
        super("BlockESP", "ESP for chosen blocks + tracers.", Category.RENDER);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        scanTicker++;
        if (scanTicker % 10 != 0) {
            spawnParticles(mc);
            return;
        }

        found.clear();
        int r = range.getInt();
        BlockPos origin = mc.player.getBlockPos();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = origin.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();
                    if (matches(block)) {
                        found.add(pos.toImmutable());
                    }
                }
            }
        }
        spawnParticles(mc);
    }

    private boolean matches(Block block) {
        return switch (preset.get()) {
            case "Ores" -> isOre(block);
            case "Chests" -> block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST
                    || block == Blocks.ENDER_CHEST || block == Blocks.BARREL
                    || block == Blocks.SHULKER_BOX;
            case "Spawners" -> block == Blocks.SPAWNER;
            case "Portals" -> block == Blocks.NETHER_PORTAL || block == Blocks.END_PORTAL
                    || block == Blocks.END_PORTAL_FRAME;
            case "All Utility" -> isOre(block) || block == Blocks.CHEST || block == Blocks.SPAWNER
                    || block == Blocks.ENDER_CHEST || block == Blocks.NETHER_PORTAL;
            default -> false;
        };
    }

    private static boolean isOre(Block b) {
        return b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE
                || b == Blocks.ANCIENT_DEBRIS
                || b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE
                || b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE
                || b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE
                || b == Blocks.LAPIS_ORE || b == Blocks.DEEPSLATE_LAPIS_ORE
                || b == Blocks.REDSTONE_ORE || b == Blocks.DEEPSLATE_REDSTONE_ORE
                || b == Blocks.COAL_ORE || b == Blocks.DEEPSLATE_COAL_ORE;
    }

    private void spawnParticles(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return;

        // Yarn 1.21.11: DustParticleEffect(int colorRgb, float scale)
        int rgb = color.get() & 0x00FFFFFF;
        DustParticleEffect dust = new DustParticleEffect(rgb, 1.0f);

        double eyesX = mc.player.getX();
        double eyesY = mc.player.getEyeY();
        double eyesZ = mc.player.getZ();

        int limit = Math.min(found.size(), 40);
        for (int i = 0; i < limit; i++) {
            BlockPos pos = found.get(i);
            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 0.5;
            double cz = pos.getZ() + 0.5;
            mc.world.addParticleClient(dust, cx, cy, cz, 0, 0, 0);

            if (tracers.get()) {
                for (int t = 1; t <= 4; t++) {
                    double f = t / 5.0;
                    mc.world.addParticleClient(dust,
                            eyesX + (cx - eyesX) * f,
                            eyesY + (cy - eyesY) * f,
                            eyesZ + (cz - eyesZ) * f,
                            0, 0, 0);
                }
            }
        }
    }

    public void renderHud(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.textRenderer == null) return;
        ctx.drawText(mc.textRenderer, "BlockESP: " + found.size() + " (" + preset.get() + ")",
                4, 40, color.get(), true);
    }
}
