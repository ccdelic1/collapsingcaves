package com.collapsingcaves.cavein;

import com.collapsingcaves.CollapsingCaves;
import com.collapsingcaves.config.CollapsingCavesConfig;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class CaveInBlockRegistry {
   private static final Set<Block> DEFAULT_BLOCKS = new HashSet<>();
   private static final Set<Block> WHITELIST = new HashSet<>();
   private static final Set<Block> BLACKLIST = new HashSet<>();
   private static boolean resolved = false;
   // Blocks that only some versions of the game have. Naming the constants would tie the
   // mod to the versions that declare them, so they are looked up by id instead and are
   // simply absent where the game does not have them.
   private static final String[] OPTIONAL_BLOCK_IDS = new String[]{"sulfur", "potent_sulfur", "cinnabar"};

   private CaveInBlockRegistry() {
   }

   public static void resolve() {
      CollapsingCavesConfig config = CollapsingCavesConfig.get();
      WHITELIST.clear();
      BLACKLIST.clear();

      for (String id : config.whitelistedBlocks) {
         Block block = findBlock(id, "whitelistedBlocks");
         if (block != null) {
            WHITELIST.add(block);
         }
      }

      for (String id : config.blacklistedBlocks) {
         Block block = findBlock(id, "blacklistedBlocks");
         if (block != null) {
            BLACKLIST.add(block);
         }
      }

      for (String id : OPTIONAL_BLOCK_IDS) {
         Block block = (Block)BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(id));
         if (block != null && block != Blocks.AIR) {
            DEFAULT_BLOCKS.add(block);
         }
      }

      resolved = true;
   }

   private static Block findBlock(String id, String setting) {
      Identifier location = Identifier.tryParse(id);
      if (location == null) {
         CollapsingCaves.LOGGER.warn("The block id \"{}\" in {} is not correct.", id, setting);
         return null;
      }

      Block block = (Block)BuiltInRegistries.BLOCK.getValue(location);
      if (block != null && block != Blocks.AIR) {
         return block;
      }

      CollapsingCaves.LOGGER.warn("The block id \"{}\" in {} does not exist.", id, setting);
      return null;
   }

   public static boolean isAffected(BlockState state) {
      if (!resolved) {
         resolve();
      }

      Block block = state.getBlock();
      return BLACKLIST.contains(block) ? false : DEFAULT_BLOCKS.contains(block) || WHITELIST.contains(block);
   }

   static {
      DEFAULT_BLOCKS.add(Blocks.STONE);
      DEFAULT_BLOCKS.add(Blocks.COBBLESTONE);
      DEFAULT_BLOCKS.add(Blocks.GRANITE);
      DEFAULT_BLOCKS.add(Blocks.DIORITE);
      DEFAULT_BLOCKS.add(Blocks.ANDESITE);
      DEFAULT_BLOCKS.add(Blocks.TUFF);
      DEFAULT_BLOCKS.add(Blocks.CALCITE);
      DEFAULT_BLOCKS.add(Blocks.DRIPSTONE_BLOCK);
      DEFAULT_BLOCKS.add(Blocks.BASALT);
      DEFAULT_BLOCKS.add(Blocks.SMOOTH_BASALT);
      DEFAULT_BLOCKS.add(Blocks.BLACKSTONE);
      DEFAULT_BLOCKS.add(Blocks.NETHERRACK);
      DEFAULT_BLOCKS.add(Blocks.SANDSTONE);
      DEFAULT_BLOCKS.add(Blocks.RED_SANDSTONE);
      DEFAULT_BLOCKS.add(Blocks.DEEPSLATE);
      DEFAULT_BLOCKS.add(Blocks.COBBLED_DEEPSLATE);
      DEFAULT_BLOCKS.add(Blocks.COAL_ORE);
      DEFAULT_BLOCKS.add(Blocks.DEEPSLATE_COAL_ORE);
      DEFAULT_BLOCKS.add(Blocks.IRON_ORE);
      DEFAULT_BLOCKS.add(Blocks.DEEPSLATE_IRON_ORE);
      DEFAULT_BLOCKS.add(Blocks.COPPER_ORE);
      DEFAULT_BLOCKS.add(Blocks.DEEPSLATE_COPPER_ORE);
      DEFAULT_BLOCKS.add(Blocks.GOLD_ORE);
      DEFAULT_BLOCKS.add(Blocks.DEEPSLATE_GOLD_ORE);
      DEFAULT_BLOCKS.add(Blocks.REDSTONE_ORE);
      DEFAULT_BLOCKS.add(Blocks.DEEPSLATE_REDSTONE_ORE);
      DEFAULT_BLOCKS.add(Blocks.LAPIS_ORE);
      DEFAULT_BLOCKS.add(Blocks.DEEPSLATE_LAPIS_ORE);
      DEFAULT_BLOCKS.add(Blocks.EMERALD_ORE);
      DEFAULT_BLOCKS.add(Blocks.DEEPSLATE_EMERALD_ORE);
      DEFAULT_BLOCKS.add(Blocks.DIAMOND_ORE);
      DEFAULT_BLOCKS.add(Blocks.DEEPSLATE_DIAMOND_ORE);
      DEFAULT_BLOCKS.add(Blocks.NETHER_GOLD_ORE);
      DEFAULT_BLOCKS.add(Blocks.NETHER_QUARTZ_ORE);
      DEFAULT_BLOCKS.add(Blocks.DIRT);
      DEFAULT_BLOCKS.add(Blocks.COARSE_DIRT);
      DEFAULT_BLOCKS.add(Blocks.ROOTED_DIRT);
      DEFAULT_BLOCKS.add(Blocks.GRASS_BLOCK);
      DEFAULT_BLOCKS.add(Blocks.PODZOL);
      DEFAULT_BLOCKS.add(Blocks.MYCELIUM);
      DEFAULT_BLOCKS.add(Blocks.CLAY);
      DEFAULT_BLOCKS.add(Blocks.MUD);
      DEFAULT_BLOCKS.add(Blocks.GRAVEL);
   }
}
