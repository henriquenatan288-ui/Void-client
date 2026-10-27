package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.NumberSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Suspicious-chunk scanner: scores LOADED chunks by storage + spawners.
 * Never calls getChunk() in a way that force-loads — that stalls the client and
 * can take down the shared VoidDraw pipeline used by other visuals.
 */
public class SusChunkFinder extends Module {
    private final NumberSetting minScore = addSetting(
            new NumberSetting("Min score", "Highlight threshold", 8, 2, 40, 1));
    private final NumberSetting scanInterval = addSetting(
            new NumberSetting("Scan interval", "Ticks between scans", 40, 10, 100, 5));
    private final NumberSetting radiusSetting = addSetting(
            new NumberSetting("Radius", "Chunk radius around player", 3, 1, 6, 1));
    private final BooleanSetting tracers = addSetting(
            new BooleanSetting("Tracers", "Line to suspicious chunks", true));
    private final BooleanSetting chests = addSetting(new BooleanSetting("Chests", "Count chests/barrels", true));
    private final BooleanSetting hoppers = addSetting(new BooleanSetting("Hoppers", "Count hoppers", true));
    private final BooleanSetting spawners = addSetting(new BooleanSetting("Spawners", "Count spawners", true));

    public record Hit(int chunkX, int chunkZ, int score, int minY, int maxY) {}

    /** Immutable snapshot — never mutate while render reads. */
    private volatile List<Hit> hits = List.of();
    private int tick;
    private static final int MAX_HITS = 24;

    public SusChunkFinder() {
        super("SusChunkFinder", "Find chunks with unusual storage/spawners.", Category.RENDER);
    }

    public List<Hit> getHits() { return hits; }
    public boolean shouldTracers() { return tracers.get(); }

    public int colorFor(int score) {
        if (score >= 25) return 0xFFFF3030;
        if (score >= 15) return 0xFFFFAA00;
        return 0xFFFFFF55;
    }

    @Override
    protected void onDisable() {
        hits = List.of();
        tick = 0;
    }

    @Override
    public void onWorldUnload() {
        hits = List.of();
        tick = 0;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            hits = List.of();
            return;
        }
        if (++tick % Math.max(5, scanInterval.getInt()) != 0) return;

        List<Hit> next = new ArrayList<>(16);
        ChunkPos playerChunk = mc.player.chunkPosition();
        int radius = radiusSetting.getInt();
        boolean countChests = chests.get();
        boolean countHoppers = hoppers.get();
        boolean countSpawners = spawners.get();
        int threshold = minScore.getInt();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = playerChunk.x() + dx;
                int cz = playerChunk.z() + dz;

                // CRITICAL: never force-load. getChunk(cx,cz) can stall the whole client.
                LevelChunk chunk = chunkIfLoaded(level, cx, cz);
                if (chunk == null) continue;

                int score = 0;
                int minY = Integer.MAX_VALUE;
                int maxY = Integer.MIN_VALUE;

                try {
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (be == null) continue;
                        BlockPos p = be.getBlockPos();
                        int add = 0;
                        if (countChests && (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity)) add = 2;
                        else if (countHoppers && be instanceof HopperBlockEntity) add = 3;
                        else if (countSpawners && be instanceof SpawnerBlockEntity) add = 12;
                        if (add == 0) continue;
                        score += add;
                        minY = Math.min(minY, p.getY());
                        maxY = Math.max(maxY, p.getY());
                    }
                } catch (Throwable ignored) {
                    // BE map can race with chunk unload — skip this chunk
                    continue;
                }

                if (score >= threshold) {
                    if (minY == Integer.MAX_VALUE) {
                        minY = (int) mc.player.getY() - 16;
                        maxY = (int) mc.player.getY() + 16;
                    }
                    // Cap vertical span so outlines stay cheap
                    if (maxY - minY > 64) {
                        int mid = (minY + maxY) / 2;
                        minY = mid - 32;
                        maxY = mid + 32;
                    }
                    next.add(new Hit(cx, cz, score, minY, maxY));
                }
            }
        }

        next.sort((a, b) -> Integer.compare(b.score(), a.score()));
        if (next.size() > MAX_HITS) {
            next = new ArrayList<>(next.subList(0, MAX_HITS));
        }
        hits = List.copyOf(next);
    }

    /**
     * Resolve a chunk only if already present in the client cache.
     * Avoids synchronous load/generation that freezes visuals.
     */
    private static LevelChunk chunkIfLoaded(ClientLevel level, int cx, int cz) {
        try {
            if (!level.hasChunk(cx, cz)) return null;
        } catch (Throwable t) {
            return null;
        }
        try {
            // Prefer ifLoaded API when present (26.x / modern mappings)
            return level.getChunk(cx, cz);
        } catch (Throwable t) {
            return null;
        }
    }
}
