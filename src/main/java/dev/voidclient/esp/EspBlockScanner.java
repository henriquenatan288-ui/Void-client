package dev.voidclient.esp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/** Chunk-queued block ESP scanner for mobile (low chunksPerTick). */
public final class EspBlockScanner {

    public record Hit(BlockPos pos, int color) {}

    public boolean filter = false;
    public boolean scanAllChunks = false;
    public boolean realOnly = false;
    public int radius = 24;
    public int chunksPerTick = 1;
    public int rescanTicks = 40;
    public int maxHits = 256;

    public final Set<Block> selected = new HashSet<>();
    public final Map<Block, Integer> colors = new HashMap<>();

    private final Map<Long, List<Hit>> byChunk = new HashMap<>();
    private final ArrayDeque<Long> queue = new ArrayDeque<>();
    private final Set<Long> verified = new HashSet<>();
    private int timer = 0;
    private volatile List<Hit> snapshot = List.of();

    public EspBlockScanner() {
        colors.put(Blocks.DIAMOND_ORE, 0xFF33E6E6);
        colors.put(Blocks.DEEPSLATE_DIAMOND_ORE, 0xFF33E6E6);
        colors.put(Blocks.GOLD_ORE, 0xFFFFD700);
        colors.put(Blocks.DEEPSLATE_GOLD_ORE, 0xFFFFD700);
        colors.put(Blocks.NETHER_GOLD_ORE, 0xFFFFD700);
        colors.put(Blocks.IRON_ORE, 0xFFD8AF93);
        colors.put(Blocks.DEEPSLATE_IRON_ORE, 0xFFD8AF93);
        colors.put(Blocks.COAL_ORE, 0xFF444444);
        colors.put(Blocks.DEEPSLATE_COAL_ORE, 0xFF444444);
        colors.put(Blocks.COPPER_ORE, 0xFFE07A3F);
        colors.put(Blocks.DEEPSLATE_COPPER_ORE, 0xFFE07A3F);
        colors.put(Blocks.REDSTONE_ORE, 0xFFFF2222);
        colors.put(Blocks.DEEPSLATE_REDSTONE_ORE, 0xFFFF2222);
        colors.put(Blocks.LAPIS_ORE, 0xFF2255FF);
        colors.put(Blocks.DEEPSLATE_LAPIS_ORE, 0xFF2255FF);
        colors.put(Blocks.EMERALD_ORE, 0xFF22DD55);
        colors.put(Blocks.DEEPSLATE_EMERALD_ORE, 0xFF22DD55);
        colors.put(Blocks.NETHER_QUARTZ_ORE, 0xFFF0F0F0);
        colors.put(Blocks.ANCIENT_DEBRIS, 0xFF8B4A2B);
    }

    public List<Hit> hits() { return snapshot; }

    public void clear() {
        byChunk.clear();
        queue.clear();
        verified.clear();
        snapshot = List.of();
        timer = 0;
    }

    public void onServerBlockUpdate(BlockPos pos, BlockState state) {
        if (verified.size() > 200_000) verified.clear();
        verified.add(pos.asLong());
        long key = chunkKey(pos.getX() >> 4, pos.getZ() >> 4);
        if (!queue.contains(key)) queue.addFirst(key);
    }

    public void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            clear();
            return;
        }

        BlockPos center = mc.player.blockPosition();

        if (timer-- <= 0) {
            timer = rescanTicks;
            fillQueue(mc, level, center);
        }

        int done = 0;
        while (done < chunksPerTick && !queue.isEmpty()) {
            scanChunk(level, queue.poll());
            done++;
        }

        byChunk.keySet().removeIf(k -> !level.hasChunk(cx(k), cz(k)));
        rebuildSnapshot(center);
    }

    private void fillQueue(Minecraft mc, ClientLevel level, BlockPos center) {
        int range;
        try {
            range = scanAllChunks ? mc.options.renderDistance().get() : (radius >> 4) + 1;
        } catch (Throwable t) {
            range = (radius >> 4) + 1;
        }
        int pcx = center.getX() >> 4;
        int pcz = center.getZ() >> 4;

        List<long[]> list = new ArrayList<>();
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                if (!level.hasChunk(pcx + dx, pcz + dz)) continue;
                list.add(new long[]{chunkKey(pcx + dx, pcz + dz), (long) dx * dx + (long) dz * dz});
            }
        }
        list.sort(Comparator.comparingLong(a -> a[1]));
        queue.clear();
        for (long[] e : list) queue.add(e[0]);
    }

    private void scanChunk(ClientLevel level, long key) {
        int cx = cx(key), cz = cz(key);
        if (!level.hasChunk(cx, cz)) {
            byChunk.remove(key);
            return;
        }

        LevelChunk chunk = level.getChunk(cx, cz);
        LevelChunkSection[] sections = chunk.getSections();
        int minY;
        try {
            minY = level.getMinY();
        } catch (Throwable t) {
            minY = level.dimensionType().minY();
        }
        List<Hit> out = new ArrayList<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection sec = sections[i];
            if (sec == null || sec.hasOnlyAir()) continue;
            try {
                if (!sec.maybeHas(st -> colorFor(st.getBlock()) != null)) continue;
            } catch (Throwable ignored) {}

            int baseY = minY + (i << 4);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        Block b = sec.getBlockState(x, y, z).getBlock();
                        Integer color = colorFor(b);
                        if (color == null) continue;
                        p.set((cx << 4) + x, baseY + y, (cz << 4) + z);
                        if (realOnly && !verified.contains(p.asLong()) && !isExposed(level, p)) continue;
                        out.add(new Hit(p.immutable(), color));
                    }
                }
            }
        }
        byChunk.put(key, out);
    }

    private Integer colorFor(Block b) {
        if (filter) {
            return selected.contains(b) ? colors.getOrDefault(b, 0xFFFFFFFF) : null;
        }
        return colors.get(b);
    }

    private boolean isExposed(ClientLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos n = new BlockPos.MutableBlockPos();
        for (Direction d : Direction.values()) {
            n.setWithOffset(pos, d);
            if (!level.hasChunkAt(n)) continue;
            BlockState s = level.getBlockState(n);
            if (s.isAir() || !s.getFluidState().isEmpty()) return true;
        }
        return false;
    }

    private void rebuildSnapshot(BlockPos c) {
        long r2 = (long) radius * radius;
        List<Hit> all = new ArrayList<>();
        for (List<Hit> l : byChunk.values()) {
            for (Hit h : l) {
                if (!scanAllChunks && h.pos().distSqr(c) > r2) continue;
                all.add(h);
            }
        }
        if (all.size() > maxHits) {
            all.sort(Comparator.comparingDouble(h -> h.pos().distSqr(c)));
            all = new ArrayList<>(all.subList(0, maxHits));
        }
        snapshot = all;
    }

    private static long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static int cx(long k) { return (int) (k >> 32); }
    private static int cz(long k) { return (int) k; }
}
