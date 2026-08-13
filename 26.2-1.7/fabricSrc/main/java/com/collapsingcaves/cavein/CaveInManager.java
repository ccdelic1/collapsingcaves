package com.collapsingcaves.cavein;

import com.collapsingcaves.CollapsingCaves;
import com.collapsingcaves.config.CollapsingCavesConfig;
import com.collapsingcaves.enchantment.GentleEnchantment;
import com.collapsingcaves.network.CaveInNetworking;
import com.collapsingcaves.tracking.PlacedBlockTracker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public class CaveInManager {
   private static final Map<ServerLevel, CaveInManager> MANAGERS = new LinkedHashMap<>();
   private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();
   private static final Map<UUID, ForcedCaveIn> FORCED_CAVE_INS = new HashMap<>();
   private static final List<FallingBlockEntity> FALLING_BLOCKS = new ArrayList<>();
   private static int pruneCountdown;
   public static final int SEA_LEVEL_MARGIN = 15;
   private final ServerLevel level;
   private final List<CaveInEvent> activeCaveIns = new ArrayList<>();
   private final Random random = new Random();

   private CaveInManager(ServerLevel level) {
      this.level = level;
   }

   public static CaveInManager get(ServerLevel level) {
      return MANAGERS.computeIfAbsent(level, CaveInManager::new);
   }

   public static void clearAll() {
      MANAGERS.clear();
      COOLDOWNS.clear();
      FORCED_CAVE_INS.clear();
      FALLING_BLOCKS.clear();
   }

   // The issuer is remembered so a later miss can be reported back to whoever asked for
   // the cave-in. It is null when the command came from the console or a command block.
   public static void setForcedCaveIn(ServerPlayer player, CaveInTier tier, UUID issuer) {
      FORCED_CAVE_INS.put(player.getUUID(), new ForcedCaveIn(tier, issuer));
   }

   public static long getCooldownRemaining(ServerPlayer player) {
      Long end = COOLDOWNS.get(player.getUUID());
      return end == null ? 0L : Math.max(0L, end - player.level().getGameTime());
   }

   public static void setCooldownSeconds(ServerPlayer player, int seconds) {
      if (seconds <= 0) {
         COOLDOWNS.remove(player.getUUID());
      } else {
         COOLDOWNS.put(player.getUUID(), player.level().getGameTime() + seconds * 20L);
      }
   }

   public static void applyLoginGrace(ServerPlayer player) {
      long grace = CollapsingCavesConfig.get().loginGraceTicks();
      if (grace > 0L) {
         long end = player.level().getGameTime() + grace;
         Long current = COOLDOWNS.get(player.getUUID());
         if (current == null || current < end) {
            COOLDOWNS.put(player.getUUID(), end);
         }
      }
   }

   private static int totalActiveCaveIns() {
      int total = 0;

      for (CaveInManager manager : MANAGERS.values()) {
         total += manager.activeCaveIns.size();
      }

      return total;
   }

   private static void pruneFallingBlocks() {
      FALLING_BLOCKS.removeIf(entity -> entity.isRemoved() || !entity.isAlive());
   }

   public int remainingFallingBlockCapacity() {
      pruneFallingBlocks();
      return Math.max(0, CollapsingCavesConfig.get().maxFallingBlocks - FALLING_BLOCKS.size());
   }

   public void registerFallingBlock(FallingBlockEntity entity) {
      FALLING_BLOCKS.add(entity);
   }

   // Landed blocks used to be dropped only when a cave-in asked for capacity, so once
   // the last cave-in ended the list held every dead entity until the next one began.
   // Managers are reclaimed on the same pass: an unloaded dimension is no longer the
   // level the server returns for its own key, and keeping it pins the whole level.
   private void pruneStaleState() {
      if (!FALLING_BLOCKS.isEmpty()) {
         pruneFallingBlocks();
      }

      if (--pruneCountdown <= 0) {
         pruneCountdown = 600;
         MinecraftServer server = this.level.getServer();
         MANAGERS.keySet().removeIf(level -> server.getLevel(level.dimension()) != level);
      }
   }

   public void tick() {
      this.pruneStaleState();
      this.activeCaveIns.removeIf(eventx -> {
         if (eventx.isFinished()) {
            CaveInNetworking.sendCaveInStop(this.level, eventx.getEpicentre());
            return true;
         } else {
            return false;
         }
      });

      for (CaveInEvent event : this.activeCaveIns) {
         event.tick();
      }
   }

   // Written as guard clauses so every way out is visible. A forced cave-in has to clear
   // the same conditions as a natural one, and when a condition turns it away the player
   // is told instead of being left to wonder. Block-level facts stay quiet on purpose:
   // breaking a block you placed, or one a cave-in can never use, is not a failed
   // condition, and reporting those would fire constantly during ordinary building.
   public void onBlockBroken(ServerPlayer player, BlockPos pos, BlockState brokenState) {
      PlacedBlockTracker tracker = PlacedBlockTracker.get(this.level);
      if (tracker.isPlayerPlaced(pos)) {
         tracker.removePlaced(pos);
         return;
      }

      if (!CaveInBlockRegistry.isAffected(brokenState)) {
         return;
      }

      CollapsingCavesConfig config = CollapsingCavesConfig.get();
      if (config.gentleEnchantmentStopsCaveIns && GentleEnchantment.isOn(player.getMainHandItem())) {
         reportForcedMiss(player, pos);
         return;
      }

      BlockPos playerPos = player.blockPosition();
      if (isSkyExposed(this.level, playerPos) || isAboveHeightLimit(this.level, playerPos)) {
         reportForcedMiss(player, pos);
         return;
      }

      // The broken block has to qualify as well, not only the player. It becomes the
      // epicentre, and a cave-in seeded at a block open to the sky is a surface collapse.
      // This is the same test canFall applies to every candidate, so the block that starts
      // a cave-in is held to the rule that decides which blocks may fall. Reading it after
      // the break is correct: isSkyExposed inspects the column above pos, never pos itself.
      if (isSkyExposed(this.level, pos)) {
         reportForcedMiss(player, pos);
         return;
      }

      if (totalActiveCaveIns() >= config.maxConcurrentCaveIns) {
         reportForcedMiss(player, pos);
         return;
      }

      ForcedCaveIn forcedEntry = FORCED_CAVE_INS.remove(player.getUUID());
      boolean forced = forcedEntry != null;
      CaveInTier tier = forced ? forcedEntry.tier() : null;
      if (!forced) {
         Long cooldownEnd = COOLDOWNS.get(player.getUUID());
         if (cooldownEnd != null && this.level.getGameTime() < cooldownEnd) {
            return;
         }

         if (this.random.nextDouble() >= config.caveInProbability()) {
            return;
         }

         tier = this.selectTier();
         if (tier == null) {
            return;
         }

         COOLDOWNS.put(player.getUUID(), this.level.getGameTime() + config.cooldownTicks());
      }

      CaveInEvent event = new CaveInEvent(this.level, tier, pos);
      this.activeCaveIns.add(event);
      CaveInNetworking.sendCaveInStart(this.level, pos, tier);
      CollapsingCaves.LOGGER.info("A {} cave-in starts at {}.", tier.id, pos);
      if (forced) {
         this.reportForcedHit(player, forcedEntry, tier);
      }
   }

   // Speaks up only while a forced cave-in is still waiting, and never consumes it, so the
   // message repeats on every turned-away break until one finally gets through. It goes to
   // whoever ran the command rather than to the player who broke the block. When that issuer
   // was the console or a command block, or has logged off since, it goes to the log instead
   // so the report is never simply lost.
   private void reportForcedMiss(ServerPlayer player, BlockPos brokenPos) {
      ForcedCaveIn forced = FORCED_CAVE_INS.get(player.getUUID());
      if (forced == null) {
         return;
      }

      List<CollapseBlocker> reasons = this.collapseBlockers(player, brokenPos);
      ServerPlayer issuer = this.resolveIssuer(forced);
      if (issuer == null) {
         List<String> plain = new ArrayList<>();

         for (CollapseBlocker reason : reasons) {
            plain.add(reason.label() + " " + reason.otherText());
         }

         CollapsingCaves.LOGGER.info("No cave-in triggered for {}: {}", player.getName().getString(), String.join(" ", plain));
         return;
      }

      issuer.sendSystemMessage(Component.literal("No Cave In Triggered: Ineligible block break conditions.").withStyle(ChatFormatting.RED));

      for (CollapseBlocker reason : reasons) {
         issuer.sendSystemMessage(reason.line(false));
      }
   }

   // The counterpart to a miss: the forced cave-in finally cleared every condition, so the
   // player who asked for it is told it landed.
   private void reportForcedHit(ServerPlayer player, ForcedCaveIn forced, CaveInTier tier) {
      String text = tier.displayName() + " Cave In Triggered on " + player.getName().getString();
      ServerPlayer issuer = this.resolveIssuer(forced);
      if (issuer != null) {
         issuer.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.GREEN));
      } else {
         CollapsingCaves.LOGGER.info(text);
      }
   }

   private ServerPlayer resolveIssuer(ForcedCaveIn forced) {
      return forced.issuer() == null ? null : this.level.getServer().getPlayerList().getPlayer(forced.issuer());
   }

   // The sea level comes from the world's chunk generator. For a noise world that is the
   // sea_level of the active NoiseGeneratorSettings, so a data pack or a mod that moves it
   // moves this limit too, and nothing here assumes 63. getSeaLevel is abstract on
   // ChunkGenerator, so a custom generator answers for itself instead of falling back to a
   // fixed number. The dimension's own vertical bounds are deliberately not consulted:
   // they describe how tall the world is, not where its terrain surface sits.
   public static int caveInHeightLimit(ServerLevel level) {
      return level.getChunkSource().getGenerator().getSeaLevel() + SEA_LEVEL_MARGIN;
   }

   private static boolean isAboveHeightLimit(ServerLevel level, BlockPos pos) {
      return CollapsingCavesConfig.get().seaLevelLimitEnabled && pos.getY() > caveInHeightLimit(level);
   }

   // Reports every reason a cave-in cannot start for this player right now. It reads
   // the same gates as onBlockBroken, in the same order, so the two cannot drift apart.
   // The per-block gates are left out because they depend on which block gets broken,
   // and unlike selectTier this never draws from the random source, so asking changes
   // nothing about what happens next.
   // targetPos is the block under consideration: the one just broken, or the one a player
   // is looking at when /cancollapsecheck asks. Null means no block could be identified,
   // in which case the block conditions are simply not evaluated.
   public List<CollapseBlocker> collapseBlockers(ServerPlayer player, BlockPos targetPos) {
      List<CollapseBlocker> blockers = new ArrayList<>();
      CollapsingCavesConfig config = CollapsingCavesConfig.get();
      if (config.gentleEnchantmentStopsCaveIns && GentleEnchantment.isOn(player.getMainHandItem())) {
         blockers.add(
            new CollapseBlocker(
               false,
               "The tool in your main hand has the Gentle enchantment.",
               "The tool in the target player's main hand has the Gentle enchantment."
            )
         );
      }

      if (isSkyExposed(this.level, player.blockPosition())) {
         blockers.add(
            new CollapseBlocker(
               false,
               "You can see the sky. Only leaves, logs and glass may stand between you and it.",
               "Target player can see the sky. Only leaves, logs and glass may stand between them and the sky."
            )
         );
      }

      if (isAboveHeightLimit(this.level, player.blockPosition())) {
         String above = " above Y " + caveInHeightLimit(this.level) + ", which is the sea level of this world plus " + SEA_LEVEL_MARGIN + ".";
         blockers.add(new CollapseBlocker(false, "You are" + above, "Target player is" + above));
      }

      if (targetPos != null && isSkyExposed(this.level, targetPos)) {
         blockers.add(new CollapseBlocker(true, "Broken block can see the sky.", "Block broken by player can see the sky."));
      }

      if (totalActiveCaveIns() >= config.maxConcurrentCaveIns) {
         String reached = "The limit of " + config.maxConcurrentCaveIns + " cave-ins at the same time is reached.";
         blockers.add(new CollapseBlocker(false, reached, reached));
      }

      if (!FORCED_CAVE_INS.containsKey(player.getUUID())) {
         long remaining = getCooldownRemaining(player);
         if (remaining > 0L) {
            String left = " cooldown has " + describeTicks(remaining) + " left.";
            blockers.add(new CollapseBlocker(false, "Your" + left, "Target player's" + left));
         }

         if (totalTierWeight() <= 0) {
            String zero = "Every cave-in size has a weight of zero.";
            blockers.add(new CollapseBlocker(false, zero, zero));
         }
      }

      return blockers;
   }

   public static boolean hasForcedCaveIn(ServerPlayer player) {
      return FORCED_CAVE_INS.containsKey(player.getUUID());
   }

   private static String describeTicks(long ticks) {
      long seconds = ticks / 20L;
      long minutes = seconds / 60L;
      return minutes > 0L ? minutes + "m " + seconds % 60L + "s" : seconds + "s";
   }

   private static int totalTierWeight() {
      int totalWeight = 0;

      for (CaveInTier tier : CaveInTier.values()) {
         totalWeight += Math.max(0, tier.weight());
      }

      return totalWeight;
   }

   private CaveInTier selectTier() {
      int totalWeight = totalTierWeight();
      if (totalWeight <= 0) {
         return null;
      }

      int roll = this.random.nextInt(totalWeight);

      for (CaveInTier tier : CaveInTier.values()) {
         roll -= Math.max(0, tier.weight());
         if (roll < 0) {
            return tier;
         }
      }

      return CaveInTier.SMALL;
   }

   // Anything that neither blocks motion nor holds fluid is not cover at all. That is
   // checked first because it is the common case when scanning an open column, and it
   // keeps vines, hanging roots, tall grass and torches from reading as a roof. The
   // fluid test keeps deep water counting as cover, as it did before.
   //
   // What remains is cover unless it is exempt: tree material (leaves, logs, mangrove
   // roots, bee nests) and see-through glass. Panes cannot be listed by block constant
   // because 26.2 moved the stained ones into a ColorCollection, so the plain pane is
   // named and the sixteen dyed ones are matched by their shared class.
   static boolean blocksSky(BlockState state) {
      if (!state.blocksMotion() && state.getFluidState().isEmpty()) {
         return false;
      }

      if (state.is(BlockTags.LEAVES)
         || state.is(BlockTags.LOGS)
         || state.is(BlockTags.BEEHIVES)
         || state.is(BlockTags.IMPERMEABLE)
         || state.is(Blocks.MANGROVE_ROOTS)
         || state.is(Blocks.MUDDY_MANGROVE_ROOTS)
         || state.is(Blocks.GLASS_PANE)
         || state.getBlock() instanceof StainedGlassPaneBlock) {
         return false;
      }

      return true;
   }

   // Sky exposure is decided by the blocks above the player, not by the light engine.
   // A skylight level below 15 only means "in shade", which is not the same as "under
   // cover", so it let cave-ins start in the open. Leaves and logs do not count as
   // cover either, so a tree overhead still prevents a cave-in.
   //
   // The scan stops at the MOTION_BLOCKING height because nothing above it can be cover.
   // The bound is inclusive so the result does not depend on whether getHeight reports
   // the highest occupied block or the first free one above it: under the latter reading
   // the extra position can never satisfy blocksSky, so visiting it is harmless.
   static boolean isSkyExposed(ServerLevel level, BlockPos pos) {
      int x = pos.getX();
      int z = pos.getZ();
      int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

      for (int y = pos.getY() + 1; y <= top; y++) {
         if (blocksSky(level.getBlockState(cursor.set(x, y, z)))) {
            return false;
         }
      }

      return true;
   }

   private record ForcedCaveIn(CaveInTier tier, UUID issuer) {
   }

   // Each blocker carries both phrasings because the two commands address different
   // audiences: /cancollapsecheck speaks to a player about themselves, while a forced
   // cave-in miss is reported to whoever ran the command, about somebody else.
   public record CollapseBlocker(boolean blockCondition, String selfText, String otherText) {
      public String label() {
         return this.blockCondition ? "(Block Condition)" : "(Player Condition)";
      }

      // The "- " root carries no style, so the reason names white rather than inheriting it.
      public Component line(boolean self) {
         return Component.literal("- ")
            .append(Component.literal(this.label()).withStyle(ChatFormatting.GRAY))
            .append(Component.literal(" " + (self ? this.selfText : this.otherText)).withStyle(ChatFormatting.WHITE));
      }
   }
}
