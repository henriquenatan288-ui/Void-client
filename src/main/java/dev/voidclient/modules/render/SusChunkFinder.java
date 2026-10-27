package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.NumberSetting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Professional suspicious-chunk scanner: scores loaded chunks by storage + spawners.
 * High score = stash / dungeon / farm candidate. Client-only (what server sends).
 */
public class SusChunkFinder extends Module {
    private final NumberSetting minScore = addSetting(
            new NumberSetting("Min score", "Highlight threshold", 8, 2, 40, 1));
    private final NumberSetting scanInterval = addSetting(
            new NumberSetting("Scan interval", "Ticks between scans", 40, 10, 100, 5));
    private final BooleanSetting tracers = addSetting(
            new BooleanSetting("Tracers", "Line to suspicious chunks", true));
    private final BooleanSetting chests = addSetting(new BooleanSetting("Chests", "Count chests/barrels", true));
    private final BooleanSetting hoppers = addSetting(new BooleanSetting("Hoppers", "Count hoppers", true));
    private final BooleanSetting spawners = addSetting(new BooleanSetting("Spawners", "Count spawners", true));

    public record Hit(int chunkX, int chunkZ, int score, int minY, int maxY) {}

    private final List<Hit> hits = new ArrayList<>();
    private int tick;

    public SusChunkFinder() {
        super("SusChunkFinder", "Find chunks with unusual storage/spawners.", Category.RENDER);
    }

    public List<Hit> getHits() { return Collections.unmodifiableList(hits); }
    public boolean shouldTracers() { return tracers.get(); }

    public int colorFor(int score) {
        if (score >= 25) return 0xFFFF3030;
        if (score >= 15) return 0xFFFFAA00;
        return 0xFFFFFF55;
    }

    @Override
    protected void onDisable() {
        hits.clear();
        tick = 0;
    }

    @Override
    public void onWorldUnload() {
        hits.clear();
        tick = 0;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            hits.clear();
            return;
        }
        if (++tick % Math.max(5, scanInterval.getInt()) != 0) return;

        List<Hit> next = new ArrayList<>();
        ChunkPos playerChunk = mc.player.chunkPosition();
        int radius = 4;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = playerChunk.x() + dx;
                int cz = playerChunk.z() + dz;
                LevelChunk chunk;
                try {
                    chunk = mc.level.getChunk(cx, cz);
                } catch (Throwable t) {
                    continue;
                }
                if (chunk == null) continue;

                int score = 0;
                int minY = Integer.MAX_VALUE;
                int maxY = Integer.MIN_VALUE;

                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be == null) continue;
                    BlockPos p = be.getBlockPos();
                    int add = 0;
                    if (chests.get() && (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity)) add = 2;
                    else if (hoppers.get() && be instanceof HopperBlockEntity) add = 3;
                    else if (spawners.get() && be instanceof SpawnerBlockEntity) add = 12;
                    if (add == 0) continue;
                    score += add;
                    minY = Math.min(minY, p.getY());
                    maxY = Math.max(maxY, p.getY());
                }

                if (score >= minScore.getInt()) {
                    if (minY == Integer.MAX_VALUE) {
                        minY = (int) mc.player.getY() - 16;
                        maxY = (int) mc.player.getY() + 16;
                    }
                    next.add(new Hit(cx, cz, score, minY, maxY));
                }
            }
        }

        next.sort((a, b) -> Integer.compare(b.score(), a.score()));
        hits.clear();
        hits.addAll(next);
    }
}
