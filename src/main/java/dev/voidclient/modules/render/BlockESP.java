package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BlockListSetting;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.NumberSetting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

/** Cached block scan with an exact nearest-first result set; the 3D renderer draws boxes/lines. */
public class BlockESP extends Module {
    private static final int MAX_STORED_BLOCKS = 160;
    private static final int SCAN_BUDGET_PER_TICK = 2800;
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
    private final BooleanSetting exposedOnly = addSetting(
            new BooleanSetting("Exposed only",
                    "Only show selected blocks touching air or fluid; filters some Anti-Xray decoys but cannot reveal hidden ores.",
                    false));

    private final List<BlockPos> found = new ArrayList<>(MAX_STORED_BLOCKS);
    private final PriorityQueue<ScanHit> nearest = new PriorityQueue<>(
            Comparator.comparingDouble(ScanHit::distanceSquared).reversed());
    private final BlockPos.MutableBlockPos scanMutable = new BlockPos.MutableBlockPos();
    private Set<Block> scanSelectedBlocks = Set.of();
    private int scanCursor;
    private List<ScanRow> scanRows = List.of();
    private int scanRowCursor;
    private int scanXCursor;
    private int scanRange = -1;
    private String scanSelection = "";
    private boolean scanExposedOnly;
    private int scanDelay;
    private BlockPos scanOrigin;

    public BlockESP() {
        super("BlockESP", "Highlight exact selected blocks with 3D boxes.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        scanCursor = 0;
        scanRows = List.of();
        scanRowCursor = 0;
        scanXCursor = 0;
        scanDelay = 0;
        scanOrigin = null;
    }

    @Override
    protected void onDisable() {
        resetScan();
    }

    @Override
    public void onWorldUnload() {
        resetScan();
    }

    private void resetScan() {
        scanCursor = 0;
        scanRows = List.of();
        scanRowCursor = 0;
        scanXCursor = 0;
        scanOrigin = null;
        nearest.clear();
        found.clear();
        scanSelectedBlocks = Set.of();
        scanRange = -1;
        scanSelection = "";
        scanDelay = 0;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            found.clear();
            nearest.clear();
            scanCursor = 0;
            scanRows = List.of();
            scanRowCursor = 0;
            scanXCursor = 0;
            scanOrigin = null;
            return;
        }

        int radius = range.getInt();
        String selected = blocks.get();
        boolean onlyExposed = exposedOnly.get();
        if (radius != scanRange || !selected.equals(scanSelection)
                || onlyExposed != scanExposedOnly) {
            scanRange = radius;
            scanSelection = selected;
            scanExposedOnly = onlyExposed;
            found.clear();
            scanCursor = 0;
            scanRows = List.of();
            scanRowCursor = 0;
            scanXCursor = 0;
            scanOrigin = null;
            scanDelay = 0;
            nearest.clear();
        }

        if (scanOrigin != null) {
            BlockPos playerPos = player.blockPosition();
            double dx = playerPos.getX() - scanOrigin.getX();
            double dy = playerPos.getY() - scanOrigin.getY();
            double dz = playerPos.getZ() - scanOrigin.getZ();
            double restartDistance = Math.max(4.0, radius / 2.0);
            if (dx * dx + dy * dy + dz * dz > restartDistance * restartDistance) {
                scanCursor = 0;
                scanRows = List.of();
                scanRowCursor = 0;
                scanXCursor = 0;
                scanOrigin = null;
                scanDelay = 0;
                nearest.clear();
            }
        }

        if (scanCursor == 0 && scanDelay > 0) {
            scanDelay--;
            return;
        }

        if (scanCursor == 0) {
            nearest.clear();
            scanOrigin = player.blockPosition().immutable();

            Set<String> selectedIds = blocks.getSelectedIds();
            Set<Block> selectedBlocks = new HashSet<>();
            for (BlockCatalog.Option option : BlockCatalog.OPTIONS) {
                if (selectedIds.contains(option.id())) selectedBlocks.add(option.block());
            }
            scanSelectedBlocks = selectedBlocks;
            scanRows = buildScanRows(radius);
            scanRowCursor = 0;
            scanXCursor = 0;

            if (scanSelectedBlocks.isEmpty()) {
                found.clear();
                scanCursor = 0;
                scanOrigin = null;
                scanRows = List.of();
                scanDelay = RESCAN_DELAY_TICKS;
                return;
            }
        }

        int processed = 0;
        while (scanRowCursor < scanRows.size() && processed < SCAN_BUDGET_PER_TICK) {
            ScanRow row = scanRows.get(scanRowCursor);
            int x = scanXCursor - row.maxX();
            scanMutable.set(scanOrigin.getX() + x,
                    scanOrigin.getY() + row.y(),
                    scanOrigin.getZ() + row.z());
            Block block = mc.level.getBlockState(scanMutable).getBlock();
            if (scanSelectedBlocks.contains(block)
                    && (!scanExposedOnly || isExposed(mc.level, scanMutable))) {
                double distanceSquared = squaredDistance(scanMutable, scanOrigin);
                if (nearest.size() < MAX_STORED_BLOCKS) {
                    nearest.add(new ScanHit(scanMutable.immutable(), distanceSquared));
                } else if (distanceSquared < nearest.peek().distanceSquared()) {
                    nearest.poll();
                    nearest.add(new ScanHit(scanMutable.immutable(), distanceSquared));
                }
            }

            scanCursor++;
            processed++;
            scanXCursor++;
            if (scanXCursor >= row.length()) {
                scanRowCursor++;
                scanXCursor = 0;
            }
        }

        if (scanRowCursor >= scanRows.size()) {
            found.clear();
            for (ScanHit hit : nearest) found.add(hit.pos());
            BlockPos currentPlayerPos = player.blockPosition();
            found.sort(Comparator.comparingDouble(pos -> squaredDistance(pos, currentPlayerPos)));
            scanCursor = 0;
            scanOrigin = null;
            scanRows = List.of();
            scanRowCursor = 0;
            scanXCursor = 0;
            scanDelay = RESCAN_DELAY_TICKS;
        }
    }

    private static boolean isExposed(ClientLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            neighbor.setWithOffset(pos, direction);
            if (!level.hasChunkAt(neighbor)) continue;
            var neighborState = level.getBlockState(neighbor);
            if (neighborState.isAir() || !neighborState.getFluidState().isEmpty()) return true;
        }
        return false;
    }

    private static List<ScanRow> buildScanRows(int radius) {
        List<ScanRow> rows = new ArrayList<>();
        int radiusSquared = radius * radius;
        for (int y = -radius; y <= radius; y++) {
            int ySquared = y * y;
            for (int z = -radius; z <= radius; z++) {
                int maxXSquared = radiusSquared - ySquared - z * z;
                if (maxXSquared < 0) continue;
                int maxX = (int) Math.floor(Math.sqrt(maxXSquared));
                rows.add(new ScanRow(y, z, maxX));
            }
        }
        return List.copyOf(rows);
    }

    private static double squaredDistance(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private record ScanRow(int y, int z, int maxX) {
        int length() { return maxX * 2 + 1; }
    }

    private record ScanHit(BlockPos pos, double distanceSquared) {}

    public List<BlockPos> getFoundBlocks() { return found; }
    public int getEspColor() { return color.get(); }
    public boolean shouldDrawTracers() { return tracers.get(); }

    @Override
    public void onRender2D(GuiGraphicsExtractor ctx, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.font == null) return;
        ctx.text(mc.font,
                "BlockESP: " + found.size() + " found / " + blocks.getSelectedIds().size() + " selected"
                        + (exposedOnly.get() ? " / exposed only" : ""),
                4, 40, color.get(), true);
    }
}
