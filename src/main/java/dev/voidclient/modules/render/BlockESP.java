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
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Cached block scan with nearest-first ordering; boxes are rendered in the 3D world pass. */
public class BlockESP extends Module {
    private static final int MAX_STORED_BLOCKS = 160;
    private static final int MAX_SCAN_CANDIDATES = 2048;
    private static final int SCAN_BUDGET_PER_TICK = 5000;
    private static final int RESCAN_DELAY_TICKS = 10;
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
            new ColorSetting("Color", "ESP box and tracer color", 0xFFAA00FF));
    private final BooleanSetting tracers = addSetting(
            new BooleanSetting("Tracers", "Draw a line from the camera to selected blocks", true));

    private final List<BlockPos> found = new ArrayList<>();
    private final List<BlockPos> scanResults = new ArrayList<>();
    private final BlockPos.Mutable scanMutable = new BlockPos.Mutable();
    private Set<Block> scanSelectedBlocks = Set.of();
    private int scanCursor;
    private int scanRange = -1;
    private String scanSelection = "";
    private int scanDelay;
    private BlockPos scanOrigin;

    public BlockESP() {
        super("BlockESP", "Highlight exact selected blocks with 3D boxes.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        scanCursor = 0;
        scanDelay = 0;
        scanOrigin = null;
    }

    @Override
    protected void onDisable() {
        scanCursor = 0;
        scanOrigin = null;
        scanResults.clear();
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

        int radius = range.getInt();
        String selected = blocks.get();
        if (radius != scanRange || !selected.equals(scanSelection)) {
            scanRange = radius;
            scanSelection = selected;
            scanCursor = 0;
            scanOrigin = null;
            scanDelay = 0;
            scanResults.clear();
        }

        if (scanOrigin != null) {
            BlockPos playerPos = player.getBlockPos();
            double dx = playerPos.getX() - scanOrigin.getX();
            double dy = playerPos.getY() - scanOrigin.getY();
            double dz = playerPos.getZ() - scanOrigin.getZ();
            double restartDistance = Math.max(4.0, radius / 2.0);
            if (dx * dx + dy * dy + dz * dz > restartDistance * restartDistance) {
                scanCursor = 0;
                scanOrigin = null;
                scanDelay = 0;
                scanResults.clear();
            }
        }

        if (scanCursor == 0 && scanDelay > 0) {
            scanDelay--;
            return;
        }

        int side = radius * 2 + 1;
        int total = side * side * side;
        if (scanCursor == 0) {
            scanResults.clear();
            scanOrigin = player.getBlockPos().toImmutable();
            Set<String> selectedIds = blocks.getSelectedIds();
            Set<Block> selectedBlocks = new HashSet<>();
            for (BlockCatalog.Option option : BlockCatalog.OPTIONS) {
                if (selectedIds.contains(option.id())) selectedBlocks.add(option.block());
            }
            scanSelectedBlocks = selectedBlocks;
        }

        int processed = 0;
        while (scanCursor < total && processed < SCAN_BUDGET_PER_TICK) {
            int cursor = scanCursor++;
            processed++;
            int x = cursor % side - radius;
            int y = (cursor / side) % side - radius;
            int z = cursor / (side * side) - radius;
            if (x * x + y * y + z * z > radius * radius) continue;

            scanMutable.set(scanOrigin.getX() + x, scanOrigin.getY() + y, scanOrigin.getZ() + z);
            Block block = mc.world.getBlockState(scanMutable).getBlock();
            if (scanSelectedBlocks.contains(block) && scanResults.size() < MAX_SCAN_CANDIDATES) {
                scanResults.add(scanMutable.toImmutable());
            }
        }

        if (scanCursor >= total) {
            BlockPos currentPlayerPos = player.getBlockPos();
            scanResults.sort(Comparator.comparingDouble(pos -> squaredDistance(pos, currentPlayerPos)));
            found.clear();
            int keep = Math.min(MAX_STORED_BLOCKS, scanResults.size());
            found.addAll(scanResults.subList(0, keep));
            scanResults.clear();
            scanCursor = 0;
            scanOrigin = null;
            scanDelay = RESCAN_DELAY_TICKS;
        }
    }

    private static double squaredDistance(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    public List<BlockPos> getFoundBlocks() { return found; }
    public int getEspColor() { return color.get(); }
    public boolean shouldDrawTracers() { return tracers.get(); }

    public void renderHud(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.textRenderer == null) return;
        ctx.drawText(mc.textRenderer,
                "BlockESP: " + found.size() + " found / " + blocks.getSelectedIds().size() + " selected",
                4, 40, color.get(), true);
    }
}
