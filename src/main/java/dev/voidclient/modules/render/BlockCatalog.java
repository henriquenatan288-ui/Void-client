package dev.voidclient.modules.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Blocks exposed in the BlockESP selection screen. IDs are Minecraft registry paths. */
public final class BlockCatalog {
    public record Option(String id, String label, String group, Block block) {}

    public static final List<Option> OPTIONS = List.of(
            new Option("diamond_ore", "Diamond Ore", "Ores", Blocks.DIAMOND_ORE),
            new Option("deepslate_diamond_ore", "Deepslate Diamond Ore", "Ores", Blocks.DEEPSLATE_DIAMOND_ORE),
            new Option("emerald_ore", "Emerald Ore", "Ores", Blocks.EMERALD_ORE),
            new Option("deepslate_emerald_ore", "Deepslate Emerald Ore", "Ores", Blocks.DEEPSLATE_EMERALD_ORE),
            new Option("gold_ore", "Gold Ore", "Ores", Blocks.GOLD_ORE),
            new Option("deepslate_gold_ore", "Deepslate Gold Ore", "Ores", Blocks.DEEPSLATE_GOLD_ORE),
            new Option("iron_ore", "Iron Ore", "Ores", Blocks.IRON_ORE),
            new Option("deepslate_iron_ore", "Deepslate Iron Ore", "Ores", Blocks.DEEPSLATE_IRON_ORE),
            new Option("lapis_ore", "Lapis Lazuli Ore", "Ores", Blocks.LAPIS_ORE),
            new Option("deepslate_lapis_ore", "Deepslate Lapis Ore", "Ores", Blocks.DEEPSLATE_LAPIS_ORE),
            new Option("redstone_ore", "Redstone Ore", "Ores", Blocks.REDSTONE_ORE),
            new Option("deepslate_redstone_ore", "Deepslate Redstone Ore", "Ores", Blocks.DEEPSLATE_REDSTONE_ORE),
            new Option("coal_ore", "Coal Ore", "Ores", Blocks.COAL_ORE),
            new Option("deepslate_coal_ore", "Deepslate Coal Ore", "Ores", Blocks.DEEPSLATE_COAL_ORE),
            new Option("copper_ore", "Copper Ore", "Ores", Blocks.COPPER_ORE),
            new Option("deepslate_copper_ore", "Deepslate Copper Ore", "Ores", Blocks.DEEPSLATE_COPPER_ORE),
            new Option("nether_quartz_ore", "Nether Quartz Ore", "Ores", Blocks.NETHER_QUARTZ_ORE),
            new Option("nether_gold_ore", "Nether Gold Ore", "Ores", Blocks.NETHER_GOLD_ORE),
            new Option("ancient_debris", "Ancient Debris", "Ores", Blocks.ANCIENT_DEBRIS),

            new Option("chest", "Chest", "Containers", Blocks.CHEST),
            new Option("trapped_chest", "Trapped Chest", "Containers", Blocks.TRAPPED_CHEST),
            new Option("ender_chest", "Ender Chest", "Containers", Blocks.ENDER_CHEST),
            new Option("barrel", "Barrel", "Containers", Blocks.BARREL),
            new Option("shulker_box", "Shulker Box", "Containers", Blocks.SHULKER_BOX),
            new Option("spawner", "Mob Spawner", "Containers", Blocks.SPAWNER),

            new Option("obsidian", "Obsidian", "Utility", Blocks.OBSIDIAN),
            new Option("crying_obsidian", "Crying Obsidian", "Utility", Blocks.CRYING_OBSIDIAN),
            new Option("nether_portal", "Nether Portal", "Utility", Blocks.NETHER_PORTAL),
            new Option("end_portal", "End Portal", "Utility", Blocks.END_PORTAL),
            new Option("end_portal_frame", "End Portal Frame", "Utility", Blocks.END_PORTAL_FRAME),
            new Option("beacon", "Beacon", "Utility", Blocks.BEACON),
            new Option("enchanting_table", "Enchanting Table", "Utility", Blocks.ENCHANTING_TABLE),
            new Option("respawn_anchor", "Respawn Anchor", "Utility", Blocks.RESPAWN_ANCHOR),
            new Option("lodestone", "Lodestone", "Utility", Blocks.LODESTONE),
            new Option("diamond_block", "Diamond Block", "Utility", Blocks.DIAMOND_BLOCK),
            new Option("emerald_block", "Emerald Block", "Utility", Blocks.EMERALD_BLOCK),
            new Option("gold_block", "Gold Block", "Utility", Blocks.GOLD_BLOCK),
            new Option("iron_block", "Iron Block", "Utility", Blocks.IRON_BLOCK),
            new Option("netherite_block", "Netherite Block", "Utility", Blocks.NETHERITE_BLOCK),
            new Option("anvil", "Anvil", "Utility", Blocks.ANVIL),
            new Option("crafting_table", "Crafting Table", "Utility", Blocks.CRAFTING_TABLE)
    );

    private BlockCatalog() {}

    public static Option byId(String id) {
        if (id == null) return null;
        for (Option option : OPTIONS) {
            if (option.id().equals(id)) return option;
        }
        return null;
    }

    public static List<String> idsInCatalogOrder(Iterable<String> ids) {
        java.util.Set<String> selected = new java.util.HashSet<>();
        for (String id : ids) {
            if (byId(id) != null) selected.add(id);
        }
        List<String> ordered = new ArrayList<>();
        for (Option option : OPTIONS) {
            if (selected.contains(option.id())) ordered.add(option.id());
        }
        return ordered;
    }
}
