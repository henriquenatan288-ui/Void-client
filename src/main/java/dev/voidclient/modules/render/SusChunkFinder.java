package dev.voidclient.modules.render;

import dev.voidclient.core.Category;
import dev.voidclient.core.Module;
import dev.voidclient.core.setting.BooleanSetting;
import dev.voidclient.core.setting.ColorSetting;
import dev.voidclient.core.setting.NumberSetting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Professional suspicious-chunk finder.
 * Scores loaded chunks by valuable block entities (chests, spawners, hoppers…)
 * and optional rare-ore sampling. Top results are drawn as 3D markers.
 */
public class SusChunkFinder extends Module {
    private final NumberSetting rangeChunks = addSetting(
            new NumberSetting("Range (chunks)", "How far from the player to scan", 6, 2, 12, 1));
    private final NumberSetting minScore = addSetting(
            new NumberSetting("Min score", "Minimum score to mark a chunk", 4, 1, 30, 1));
    private final BooleanSetting includeOres = addSetting(
            new BooleanSetting("Sample ores", "Add score from rare ores inside the chunk", true));
    private final ColorSetting color = addSetting(
            new ColorSetting("Marker color", "Color of the chunk marker", 0xFFFF5500));

    private final List<ScoredChunk> results = new ArrayList<>();
    private int scanTick;

    public SusChunkFinder() {
        super("SusChunkFinder", "Find suspicious chunks (chests, spawners, hoppers, rare ores).", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        results.clear();
        scanTick = 0;
    }

    @Override
    protected void onDisable() {
        results.clear();
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        ClientWorld world = mc.world;
        if (player == null || world == null) {
            results.clear();
            return;
        }

        // Rescan every 20 ticks to stay light
        if (++scanTick % 20 != 0) return;

        int radius = rangeChunks.getInt();
        ChunkPos origin = player.getChunkPos();
        List<ScoredChunk> found = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = origin.x + dx;
                int cz = origin.z + dz;
                if (!world.getChunkManager().isChunkLoaded(cx, cz)) continue;

                WorldChunk chunk = world.getChunk(cx, cz);
                int score = scoreChunk(chunk, world);
                if (score >= minScore.getInt()) {
                    found.add(new ScoredChunk(cx, cz, score));
                }
            }
        }

        found.sort(Comparator.comparingInt(ScoredChunk::score).reversed());
        if (found.size() > 32) found = found.subList(0, 32);
        results.clear();
        results.addAll(found);
    }

    private int scoreChunk(WorldChunk chunk, ClientWorld world) {
        int score = 0;

        // Block entities are the strongest signal
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be instanceof ChestBlockEntity || be instanceof TrappedChestBlockEntity) score += 3;
            else if (be instanceof BarrelBlockEntity) score += 3;
            else if (be instanceof ShulkerBoxBlockEntity) score += 5;
            else if (be instanceof MobSpawnerBlockEntity) score += 8;
            else if (be instanceof HopperBlockEntity) score += 4;
            else if (be instanceof FurnaceBlockEntity || be instanceof BlastFurnaceBlockEntity
                    || be instanceof SmokerBlockEntity) score += 2;
            else if (be instanceof BrewingStandBlockEntity) score += 3;
            else if (be instanceof DropperBlockEntity || be instanceof DispenserBlockEntity) score += 2;
            else if (be instanceof EnderChestBlockEntity) score += 4;
        }

        if (includeOres.get()) {
            // Light sampling of the chunk for rare ores (every 4 blocks)
            BlockPos.Mutable mut = new BlockPos.Mutable();
            int baseX = chunk.getPos().getStartX();
            int baseZ = chunk.getPos().getStartZ();
            for (int x = 0; x < 16; x += 4) {
                for (int z = 0; z < 16; z += 4) {
                    for (int y = world.getBottomY(); y < world.getTopYInclusive(); y += 8) {
                        mut.set(baseX + x, y, baseZ + z);
                        BlockState state = chunk.getBlockState(mut);
                        if (state.isOf(Blocks.DIAMOND_ORE) || state.isOf(Blocks.DEEPSLATE_DIAMOND_ORE)) score += 2;
                        else if (state.isOf(Blocks.EMERALD_ORE) || state.isOf(Blocks.DEEPSLATE_EMERALD_ORE)) score += 2;
                        else if (state.isOf(Blocks.ANCIENT_DEBRIS)) score += 4;
                        else if (state.isOf(Blocks.SPAWNER)) score += 6;
                    }
                }
            }
        }

        return score;
    }

    public List<ScoredChunk> getResults() {
        return results;
    }

    public int getMarkerColor() {
        return color.get();
    }

    public void renderHud(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.textRenderer == null) return;
        ctx.drawText(mc.textRenderer,
                "SusChunks: " + results.size() + " marked",
                4, 52, color.get(), true);
        int y = 64;
        for (int i = 0; i < Math.min(5, results.size()); i++) {
            ScoredChunk sc = results.get(i);
            ctx.drawText(mc.textRenderer,
                    String.format("  [%d,%d] score=%d", sc.x(), sc.z(), sc.score()),
                    4, y, 0xFFCCCCCC, true);
            y += 10;
        }
    }

    public record ScoredChunk(int x, int z, int score) {}
}
