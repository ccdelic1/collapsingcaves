package com.collapsingcaves.cavein;

import com.collapsingcaves.config.CollapsingCavesConfig;
import com.collapsingcaves.tracking.PlacedBlockTracker;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CaveInEvent {
   private static final String FALLING_BLOCK_TAG = "collapsingcaves_cavein";
   private static final double USUAL_BLOCK_GRAVITY = 0.04;
   // A candidate at the epicentre is this much more likely to be picked as the seed of
   // a cluster than a candidate at the full radius. 0.3 gives the centre 1.3 chances
   // against 1.0 at the edge.
   private static final double NEAR_SEED_BIAS = 0.3;
   private final ServerLevel level;
   private final CaveInTier tier;
   private final BlockPos epicentre;
   private final Random random = new Random();
   private final Set<BlockPos> candidates = new HashSet<>();
   private final int horizontalRadiusSquared;
   private final int lowestY;
   private final int highestY;
   private boolean scanning = true;
   private Queue<BlockPos> airFrontier = new ArrayDeque<>();
   private LongOpenHashSet visitedAir = new LongOpenHashSet();
   private int ticksRemaining;
   private int ticksToNextCluster;
   private int ticksBeforeFirstDrop;
   private boolean finished;

   public CaveInEvent(ServerLevel level, CaveInTier tier, BlockPos epicentre) {
      this.level = level;
      this.tier = tier;
      this.epicentre = epicentre.immutable();
      int horizontalRadius = tier.horizontalRadius();
      this.horizontalRadiusSquared = horizontalRadius * horizontalRadius;
      this.lowestY = epicentre.getY() - tier.depthBelow();
      this.highestY = epicentre.getY() + tier.heightAbove();
      this.ticksRemaining = tier.durationTicks();
      this.ticksToNextCluster = 1;
      // The rumble and the shake run for this long before the first block moves, giving a
      // player a moment to get clear. Capped at the tier duration so that a warning longer
      // than the cave-in itself cannot leave the event with no time left to drop anything.
      this.ticksBeforeFirstDrop = Math.min(CollapsingCavesConfig.get().warningTicks(), this.ticksRemaining);
      this.seedAirSearch();
   }

   private void seedAirSearch() {
      if (!this.addAirPosition(this.epicentre)) {
         for (Direction direction : Direction.values()) {
            this.addAirPosition(this.epicentre.relative(direction));
         }
      }
   }

   private boolean addAirPosition(BlockPos pos) {
      if (!this.isInRange(pos) || this.level.isOutsideBuildHeight(pos.getY())) {
         return false;
      }

      if (!this.visitedAir.add(pos.asLong())) {
         return false;
      }

      if (!this.level.getBlockState(pos).isAir()) {
         return false;
      }

      this.airFrontier.add(pos.immutable());
      return true;
   }

   public boolean isFinished() {
      return this.finished;
   }

   public BlockPos getEpicentre() {
      return this.epicentre;
   }

   // The tier duration is now spent from the moment the cave-in is announced rather than
   // from the moment the cave has been mapped, so the warning window and the scan both
   // come out of it: a sixty second cave-in lasts sixty seconds from the rumble starting,
   // whatever the cave costs to walk.
   public void tick() {
      if (this.finished) {
         return;
      }

      if (this.ticksRemaining <= 0) {
         this.finished = true;
         return;
      }

      this.ticksRemaining--;
      if (this.ticksBeforeFirstDrop > 0) {
         this.ticksBeforeFirstDrop--;
      }

      if (this.scanning) {
         this.continueScan();
         return;
      }

      if (this.ticksBeforeFirstDrop > 0) {
         return;
      }

      if (--this.ticksToNextCluster <= 0) {
         this.dropBlocks();
         this.ticksToNextCluster = this.nextInterval();
      }
   }

   private void continueScan() {
      int budget = CollapsingCavesConfig.get().scanPositionsPerTick;
      PlacedBlockTracker tracker = PlacedBlockTracker.get(this.level);

      while (!this.airFrontier.isEmpty()) {
         if (budget-- <= 0) {
            return;
         }

         BlockPos air = this.airFrontier.poll();
         BlockPos ceiling = air.above();
         if (this.canFall(ceiling, tracker)) {
            this.candidates.add(ceiling);
         }

         for (Direction direction : Direction.values()) {
            this.addAirPosition(air.relative(direction));
         }
      }

      this.scanning = false;
      this.airFrontier = null;
      this.visitedAir = null;
   }

   private int nextInterval() {
      int interval = this.tier.clusterIntervalTicks();
      int change = Math.max(1, interval / 5);
      return Math.max(1, interval - change + this.random.nextInt(change * 2 + 1));
   }

   private void dropBlocks() {
      CaveInManager manager = CaveInManager.get(this.level);
      int budget = Math.min(this.clusterSize(), manager.remainingFallingBlockCapacity());
      if (budget > 0) {
         PlacedBlockTracker tracker = PlacedBlockTracker.get(this.level);

         while (budget > 0 && !this.candidates.isEmpty()) {
            List<BlockPos> shape = this.growShape(tracker, budget);
            if (shape.isEmpty()) {
               break;
            }

            for (BlockPos pos : shape) {
               this.dropBlock(manager, pos, tracker);
            }

            budget -= shape.size();
         }
      }
   }

   private int clusterSize() {
      int min = this.tier.minClusterBlocks();
      int max = Math.max(min, this.tier.maxClusterBlocks());
      return min + this.random.nextInt(max - min + 1);
   }

   private void dropBlock(CaveInManager manager, BlockPos pos, PlacedBlockTracker tracker) {
      BlockState state = this.level.getBlockState(pos);
      if (!state.isAir() && CaveInBlockRegistry.isAffected(state)) {
         CollapsingCavesConfig config = CollapsingCavesConfig.get();
         FallingBlockEntity entity = FallingBlockEntity.fall(this.level, pos, state);
         entity.setHurtsEntities((float)config.fallDamagePerBlock, config.maxFallDamage);
         entity.addTag(FALLING_BLOCK_TAG);
         entity.dropItem = false;
         entity.setNoGravity(true);
         if (entity instanceof CaveInFallingBlock caveInBlock) {
            caveInBlock.setCaveInGravity((float)(USUAL_BLOCK_GRAVITY * config.fallingBlockSpeed));
         }

         manager.registerFallingBlock(entity);
         BlockPos above = pos.above();
         if (this.isInRange(above) && this.canFall(above, tracker)) {
            this.candidates.add(above);
         }
      }
   }

   private boolean isInRange(BlockPos pos) {
      if (pos.getY() >= this.lowestY && pos.getY() <= this.highestY) {
         int dx = pos.getX() - this.epicentre.getX();
         int dz = pos.getZ() - this.epicentre.getZ();
         return dx * dx + dz * dz <= this.horizontalRadiusSquared;
      } else {
         return false;
      }
   }

   private boolean canFall(BlockPos pos, PlacedBlockTracker tracker) {
      if (this.level.isOutsideBuildHeight(pos.getY())) {
         return false;
      } else {
         BlockState state = this.level.getBlockState(pos);
         if (state.isAir() || !CaveInBlockRegistry.isAffected(state)) {
            return false;
         } else if (tracker.isPlayerPlaced(pos)) {
            return false;
         } else if (!this.level.getBlockState(pos.below()).isAir()) {
            return false;
         } else {
            // Nothing but air, leaves or logs between this block and the sky means it is
            // holding up a surface, so it must not fall. The old canSeeSky(pos) test could
            // never detect that: pos is always a solid block here, and a solid block stores
            // sky light 0 at its own position, so the test read false for every candidate
            // and the guard never fired.
            return !CaveInManager.isSkyExposed(this.level, pos);
         }
      }
   }

   private List<BlockPos> growShape(PlacedBlockTracker tracker, int maxSize) {
      BlockPos seed = this.takeSeed(tracker);
      if (seed == null) {
         return Collections.emptyList();
      }

      List<BlockPos> shape = new ArrayList<>();
      shape.add(seed);
      Queue<BlockPos> frontier = new ArrayDeque<>();
      frontier.add(seed);
      List<Direction> directions = new ArrayList<>(List.of(Direction.values()));

      while (!frontier.isEmpty() && shape.size() < maxSize) {
         BlockPos current = frontier.poll();
         Collections.shuffle(directions, this.random);

         for (Direction direction : directions) {
            if (shape.size() >= maxSize) {
               break;
            }

            BlockPos neighbour = current.relative(direction);
            if (this.candidates.remove(neighbour) && this.canFall(neighbour, tracker)) {
               shape.add(neighbour);
               frontier.add(neighbour);
            }
         }
      }

      return shape;
   }

   private BlockPos takeSeed(PlacedBlockTracker tracker) {
      while (!this.candidates.isEmpty()) {
         BlockPos seed = this.selectWeighted(this.candidates);
         this.candidates.remove(seed);
         if (this.canFall(seed, tracker)) {
            return seed;
         }
      }

      return null;
   }

   // Weight falls evenly with the horizontal distance from the epicentre, because the
   // radius that bounds a cave-in is a horizontal one. A block straight above the player
   // is at distance zero and carries the full weight.
   private double seedWeight(BlockPos pos) {
      int radius = this.tier.horizontalRadius();
      if (radius <= 0) {
         return 1.0;
      }

      int dx = pos.getX() - this.epicentre.getX();
      int dz = pos.getZ() - this.epicentre.getZ();
      double distance = Math.sqrt((double)(dx * dx + dz * dz));
      return 1.0 + NEAR_SEED_BIAS * (1.0 - Math.min(1.0, distance / radius));
   }

   // Weighted selection in one pass: each candidate takes the place of the current choice
   // with the probability of its own weight against the weight seen so far. The result is
   // the same as a full weighted draw, but it keeps the single pass that the old even
   // selection used, which matters because the candidate set can hold many thousands of
   // positions. The first candidate always takes the place of nothing, so a set that is
   // not empty never gives back null.
   private BlockPos selectWeighted(Set<BlockPos> set) {
      BlockPos chosen = null;
      double total = 0.0;

      for (BlockPos pos : set) {
         double weight = this.seedWeight(pos);
         total += weight;
         if (this.random.nextDouble() * total < weight) {
            chosen = pos;
         }
      }

      return chosen;
   }
}
