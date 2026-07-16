package com.collapsingcaves.cavein;

import com.collapsingcaves.config.CollapsingCavesConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

public class CaveInBlockRegistry {
    private static final Set<Block> DEFAULT_BLOCKS = new HashSet<>();
    private static final Set<Block> RESOLVED_WHITELIST = new HashSet<>();
    private static final Set<Block> RESOLVED_BLACKLIST = new HashSet<>();
    private static boolean resolved = false;

    static {
        // Stone and stone variants
        DEFAULT_BLOCKS.add(Blocks.STONE);
        DEFAULT_BLOCKS.add(Blocks.COBBLESTONE);
        DEFAULT_BLOCKS.add(Blocks.MOSSY_COBBLESTONE);
        DEFAULT_BLOCKS.add(Blocks.GRANITE);
        DEFAULT_BLOCKS.add(Blocks.POLISHED_GRANITE);
        DEFAULT_BLOCKS.add(Blocks.DIORITE);
        DEFAULT_BLOCKS.add(Blocks.POLISHED_DIORITE);
        DEFAULT_BLOCKS.add(Blocks.ANDESITE);
        DEFAULT_BLOCKS.add(Blocks.POLISHED_ANDESITE);
        DEFAULT_BLOCKS.add(Blocks.DEEPSLATE);
        DEFAULT_BLOCKS.add(Blocks.COBBLED_DEEPSLATE);
        DEFAULT_BLOCKS.add(Blocks.POLISHED_DEEPSLATE);
        DEFAULT_BLOCKS.add(Blocks.CALCITE);
        DEFAULT_BLOCKS.add(Blocks.TUFF);
        // Polished Tuff was added in Minecraft 1.21 and does not exist in 1.20.1.
        DEFAULT_BLOCKS.add(Blocks.DRIPSTONE_BLOCK);
        DEFAULT_BLOCKS.add(Blocks.BASALT);
        DEFAULT_BLOCKS.add(Blocks.SMOOTH_BASALT);
        DEFAULT_BLOCKS.add(Blocks.BLACKSTONE);
        DEFAULT_BLOCKS.add(Blocks.POLISHED_BLACKSTONE);
        DEFAULT_BLOCKS.add(Blocks.NETHERRACK);
        DEFAULT_BLOCKS.add(Blocks.SANDSTONE);
        DEFAULT_BLOCKS.add(Blocks.RED_SANDSTONE);

        // Terracotta (generates naturally in badlands)
        DEFAULT_BLOCKS.add(Blocks.TERRACOTTA);
        DEFAULT_BLOCKS.add(Blocks.WHITE_TERRACOTTA);
        DEFAULT_BLOCKS.add(Blocks.ORANGE_TERRACOTTA);
        DEFAULT_BLOCKS.add(Blocks.MAGENTA_TERRACOTTA);
        DEFAULT_BLOCKS.add(Blocks.LIGHT_BLUE_TERRACOTTA);
        DEFAULT_BLOCKS.add(Blocks.YELLOW_TERRACOTTA);
        DEFAULT_BLOCKS.add(Blocks.LIME_TERRACOTTA);
        DEFAULT_BLOCKS.add(Blocks.PINK_TERRACOTTA);
        DEFAULT_BLOCKS.add(Blocks.GRAY_TERRACOTTA);
        DEFAULT_BLOCKS.add(Blocks.LIGHT_GRAY_TERRACOTTA);
        DEFAULT_BLOCKS.add(Blocks.CYAN_TERRACOTTA);
        DEFAULT_BLOCKS.add(Blocks.PURPLE_TERRACOTTA);
        DEFAULT_BLOCKS.add(Blocks.BLUE_TERRACOTTA);
        DEFAULT_BLOCKS.add(Blocks.BROWN_TERRACOTTA);
        DEFAULT_BLOCKS.add(Blocks.GREEN_TERRACOTTA);
        DEFAULT_BLOCKS.add(Blocks.RED_TERRACOTTA);
        DEFAULT_BLOCKS.add(Blocks.BLACK_TERRACOTTA);

        // Ores
        DEFAULT_BLOCKS.add(Blocks.COAL_ORE);
        DEFAULT_BLOCKS.add(Blocks.DEEPSLATE_COAL_ORE);
        DEFAULT_BLOCKS.add(Blocks.IRON_ORE);
        DEFAULT_BLOCKS.add(Blocks.DEEPSLATE_IRON_ORE);
        DEFAULT_BLOCKS.add(Blocks.GOLD_ORE);
        DEFAULT_BLOCKS.add(Blocks.DEEPSLATE_GOLD_ORE);
        DEFAULT_BLOCKS.add(Blocks.DIAMOND_ORE);
        DEFAULT_BLOCKS.add(Blocks.DEEPSLATE_DIAMOND_ORE);
        DEFAULT_BLOCKS.add(Blocks.EMERALD_ORE);
        DEFAULT_BLOCKS.add(Blocks.DEEPSLATE_EMERALD_ORE);
        DEFAULT_BLOCKS.add(Blocks.LAPIS_ORE);
        DEFAULT_BLOCKS.add(Blocks.DEEPSLATE_LAPIS_ORE);
        DEFAULT_BLOCKS.add(Blocks.REDSTONE_ORE);
        DEFAULT_BLOCKS.add(Blocks.DEEPSLATE_REDSTONE_ORE);
        DEFAULT_BLOCKS.add(Blocks.COPPER_ORE);
        DEFAULT_BLOCKS.add(Blocks.DEEPSLATE_COPPER_ORE);
        DEFAULT_BLOCKS.add(Blocks.NETHER_GOLD_ORE);
        DEFAULT_BLOCKS.add(Blocks.NETHER_QUARTZ_ORE);

        // Dirt and gravel-like blocks
        DEFAULT_BLOCKS.add(Blocks.DIRT);
        DEFAULT_BLOCKS.add(Blocks.COARSE_DIRT);
        DEFAULT_BLOCKS.add(Blocks.ROOTED_DIRT);
        DEFAULT_BLOCKS.add(Blocks.GRAVEL);
        DEFAULT_BLOCKS.add(Blocks.SAND);
        DEFAULT_BLOCKS.add(Blocks.RED_SAND);
        DEFAULT_BLOCKS.add(Blocks.CLAY);
        DEFAULT_BLOCKS.add(Blocks.GRASS_BLOCK);
        DEFAULT_BLOCKS.add(Blocks.PODZOL);
        DEFAULT_BLOCKS.add(Blocks.MYCELIUM);
        DEFAULT_BLOCKS.add(Blocks.MUD);
        DEFAULT_BLOCKS.add(Blocks.PACKED_MUD);
        DEFAULT_BLOCKS.add(Blocks.SOUL_SAND);
        DEFAULT_BLOCKS.add(Blocks.SOUL_SOIL);
        DEFAULT_BLOCKS.add(Blocks.MOSS_BLOCK);
    }

    public static void resolve() {
        CollapsingCavesConfig config = CollapsingCavesConfig.get();
        RESOLVED_WHITELIST.clear();
        RESOLVED_BLACKLIST.clear();

        for (String id : config.whitelistedBlocks) {
            ResourceLocation loc = ResourceLocation.tryParse(id);
            if (loc != null) {
                Block block = BuiltInRegistries.BLOCK.get(loc);
                if (block != null && block != Blocks.AIR) {
                    RESOLVED_WHITELIST.add(block);
                }
            }
        }

        for (String id : config.blacklistedBlocks) {
            ResourceLocation loc = ResourceLocation.tryParse(id);
            if (loc != null) {
                Block block = BuiltInRegistries.BLOCK.get(loc);
                if (block != null && block != Blocks.AIR) {
                    RESOLVED_BLACKLIST.add(block);
                }
            }
        }

        resolved = true;
    }

    public static boolean isAffected(BlockState state) {
        if (!resolved) {
            resolve();
        }
        Block block = state.getBlock();
        if (RESOLVED_BLACKLIST.contains(block)) {
            return false;
        }
        return DEFAULT_BLOCKS.contains(block) || RESOLVED_WHITELIST.contains(block);
    }
}
