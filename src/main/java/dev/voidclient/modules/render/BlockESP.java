package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BlockListSetting;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Highlights selected blocks using refreshed corner markers and dotted tracer trails. */
public class BlockESP extends Module {
    private static final int MAX_STORED_BLOCKS = 160;
    private static final int SCAN_BUDGET_PER_TICK = 5000;

    private static final String DEFAULT_BLOCKS =
            "diamond_ore,deepslate_diamond_ore,emerald_ore,deepslate_emerald_ore,"
            + "gold_ore,deepslate_gold_ore,iron_ore,deepslate_iron_ore,"
            + "lapis_ore,deepslate_lapis_ore,redstone_ore,deepslate_redstone_ore,"
            + "coal_ore,deepslate_coal_ore,copper_ore,deepslate_copper_ore,"
            + "nether_quartz_ore,nether_gold_ore,ancient_debris";

    private final BlockListSetting blocks = addSetting(new BlockListSetting(
            "Blocks", "Open the block picker to choose exact blocks", DEFAULT_BLOCKS));
    private final NumberSetting range = addSetting(
            new NumberSetting("Range", "Scan radius in blocks", 16, 4, 32, 1));
    private final ColorSetting color = addSetting(
            new ColorSetting("Color", "ESP marker and tracer color", 0xFFAA00FF));
    private final BooleanSetting tracers = addSetting(
            new BooleanSetting("Tracers", "Draw particle trails from you to blocks", true));

    private final List<BlockPos> found = new ArrayList<>();
    private final List<BlockPos> scanResults = new ArrayList<>();
    private int scanCursor;
    private int scanRange = -1;
    private String scanSelection = "";
    private BlockPos scanOrigin;
    private int visualTicker;

    public BlockESP() {
        super("BlockESP", "Find and mark the specific blocks selected in the picker.", Category.RENDER);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) {
            found.clear();
            scanResults.clear();
            scanCursor = 0;
            scanOrigin = null;
            return;
        }

        int r = range.getInt();
        String selected = blocks.get();
        if (r != scanRange || !selected.equals(scanSelection)) {
            scanRange = r;
            scanSelection = selected;
            scanCursor = 0;
            scanOrigin = null;
            scanResults.clear();
        }

        int side = r * 2 + 1;
        int total = side * side * side;
        if (scanCursor == 0) {
            scanResults.clear();
            scanOrigin = player.getBlockPos().toImmutable();
        }

        int processed = 0;
        while (scanCursor < total && processed < SCAN_BUDGET_PER_TICK) {
            int cursor = scanCursor++;
            processed++;

            int x = cursor % side - r;
            int y = (cursor / side) % side - r;
            int z = cursor / (side * side) - r;
            if ((x * x) + (y * y) + (z * z) > r * r) continue;

            BlockPos pos = scanOrigin.add(x, y, z);
            Block block = mc.world.getBlockState(pos).getBlock();
            if (matches(block) && scanResults.size() < MAX_STORED_BLOCKS) {
                scanResults.add(pos.toImmutable());
            }
        }
        if (scanCursor >= total) {
            found.clear();
            found.addAll(scanResults);
            scanResults.clear();
            scanCursor = 0;
            scanOrigin = null;
        }

        visualTicker++;
        if (visualTicker % 2 == 0) spawnMarkers(mc, player);
    }

    private boolean matches(Block block) {
        for (BlockCatalog.Option option : BlockCatalog.OPTIONS) {
            if (option.block() == block && blocks.isSelected(option.id())) return true;
        }
        return false;
    }

    private void spawnMarkers(MinecraftClient mc, ClientPlayerEntity player) {
        if (mc.world == null || found.isEmpty()) return;

        DustParticleEffect dust = new DustParticleEffect(color.getRgb(), 1.25f);
        double fromX = player.getX();
        double fromY = player.getEyeY();
        double fromZ = player.getZ();

        int count = Math.min(found.size(), 10);
        for (int i = 0; i < count; i++) {
            BlockPos pos = found.get(i);
            double minX = pos.getX() + 0.04;
            double minY = pos.getY() + 0.04;
            double minZ = pos.getZ() + 0.04;
            double maxX = pos.getX() + 0.96;
            double maxY = pos.getY() + 0.96;
            double maxZ = pos.getZ() + 0.96;

            addParticle(mc, dust, minX, minY, minZ);
            addParticle(mc, dust, minX, minY, maxZ);
            addParticle(mc, dust, minX, maxY, minZ);
            addParticle(mc, dust, minX, maxY, maxZ);
            addParticle(mc, dust, maxX, minY, minZ);
            addParticle(mc, dust, maxX, minY, maxZ);
            addParticle(mc, dust, maxX, maxY, minZ);
            addParticle(mc, dust, maxX, maxY, maxZ);

            if (tracers.get()) {
                double targetX = pos.getX() + 0.5;
                double targetY = pos.getY() + 0.5;
                double targetZ = pos.getZ() + 0.5;
                for (int step = 1; step <= 7; step++) {
                    double t = step / 8.0;
                    addParticle(mc, dust,
                            fromX + (targetX - fromX) * t,
                            fromY + (targetY - fromY) * t,
                            fromZ + (targetZ - fromZ) * t);
                }
            }
        }
    }

    private static void addParticle(MinecraftClient mc, DustParticleEffect dust,
                                    double x, double y, double z) {
        mc.world.addParticleClient(dust, x, y, z, 0, 0, 0);
    }

    public void renderHud(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.textRenderer == null) return;
        ctx.drawText(mc.textRenderer,
                "BlockESP: " + found.size() + " found / " + blocks.getSelectedIds().size() + " selected",
                4, 40, color.get(), true);
    }
}
