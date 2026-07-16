package com.collapsingcaves.cavein;

import com.collapsingcaves.tracking.PlacedBlockTracker;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class CaveInEvent {
    private static final float FALL_DAMAGE_PER_DISTANCE = 1.0f;
    private static final int FALL_DAMAGE_MAX = 20;
    private static final int CLUSTER_SIZE = 5;
    // Upper bound on ticks a layer's cluster cascade may take when it has many eligible
    // clusters (large radius, dense terrain) - clusters-per-tick speeds up past the floor
    // below only as needed to still finish within this budget. Tuned so tier.maxLayers *
    // (layerDelayTicks + this) caps gargantuan (5 layers) at ~150s (2.5 min) for a layer
    // large enough to need the speedup, with small/medium/large/enormous capping
    // proportionally lower at 1-4 layers.
    private static final int TARGET_LAYER_TICKS = 588;
    // Floor on release speed: clusters are never released faster than one every this many
    // ticks. The previous version had an implicit floor of "1 cluster per tick", which felt
    // like way too much was falling at once - this explicitly slows that floor down by 6x.
    // A fractional "release credit" accumulates each tick and clusters are only spawned once
    // it reaches a whole number, since a plain integer batch size can't express a rate slower
    // than 1/tick.
    private static final int PACE_DIVISOR = 6;
    // Upper bound on how many candidate positions a single layer scan (see beginLayerScan/
    // continueScan below) may examine per tick. The scan volume is O(radius^3), so a
    // gargantuan-tier cave-in (radius 32, or larger still with caveInSizeMultiplier) scanning
    // its full cube in one synchronous pass could spike a single server tick well past its
    // 50ms budget. Spreading the same scan (same positions, same order, same result) across
    // multiple ticks instead keeps every tick's share of the work cheap, mirroring the
    // adaptive release pacing above which does the same thing for cluster spawning.
    private static final int SCAN_POSITIONS_PER_TICK = 20_000;

    private final ServerLevel level;
    private final CaveInTier tier;
    private final BlockPos originalCenter;
    private final ServerPlayer player;
    private final double sizeMultiplier;
    private final LongOpenHashSet processedBlocks = new LongOpenHashSet();
    private List<List<BlockPos>> currentClusters;
    private int currentClusterIndex = 0;
    private int currentLayer = 0;
    private int tickDelay = 0;
    private double releaseCredit = 0.0;
    private boolean finished = false;

    // Incremental layer-scan state - see beginLayerScan/continueScan. scanX/scanY/scanZ are
    // the next cube offset (relative to originalCenter) to examine, resumed across calls.
    private boolean scanning = false;
    private int scanRadius;
    private int scanRadiusSq;
    private int scanX;
    private int scanY;
    private int scanZ;
    private List<BlockPos> scanEligibleBlocks;

    public CaveInEvent(ServerLevel level, ServerPlayer player, CaveInTier tier, double sizeMultiplier) {
        this.level = level;
        this.player = player;
        this.originalCenter = player.blockPosition();
        this.tier = tier;
        this.sizeMultiplier = sizeMultiplier;
        beginLayerScan();
        // Do as much of the scan as fits in one tick's budget right away: this keeps small/
        // medium/large tiers (whose full cube fits within SCAN_POSITIONS_PER_TICK) finishing
        // synchronously here exactly as before, so isFinished() is still immediately accurate
        // for the common case. Larger scans simply continue across subsequent tick() calls.
        continueScan();
    }

    public boolean isFinished() {
        return finished;
    }

    public BlockPos getCenter() {
        return originalCenter;
    }

    public CaveInTier getTier() {
        return tier;
    }

    public void tick() {
        if (finished) return;

        if (player.isRemoved() || !player.isAlive()) {
            finished = true;
            return;
        }

        if (scanning) {
            continueScan();
            return;
        }

        if (tickDelay > 0) {
            tickDelay--;
            return;
        }

        if (currentClusterIndex < currentClusters.size()) {
            double uncappedRatePerTick = currentClusters.size() / (double) TARGET_LAYER_TICKS;
            double releaseRatePerTick = Math.max(1.0 / PACE_DIVISOR, uncappedRatePerTick);
            releaseCredit += releaseRatePerTick;
            int clustersThisTick = (int) releaseCredit;
            if (clustersThisTick > 0) {
                releaseCredit -= clustersThisTick;
                int endIndex = Math.min(currentClusterIndex + clustersThisTick, currentClusters.size());
                for (int i = currentClusterIndex; i < endIndex; i++) {
                    for (BlockPos pos : currentClusters.get(i)) {
                        BlockState state = level.getBlockState(pos);
                        if (CaveInBlockRegistry.isAffected(state) && !state.isAir()) {
                            FallingBlockEntity entity = FallingBlockEntity.fall(level, pos, state);
                            entity.setHurtsEntities(FALL_DAMAGE_PER_DISTANCE, FALL_DAMAGE_MAX);
                            entity.addTag("collapsingcaves_cavein");
                            entity.dropItem = false;
                        }
                    }
                }
                currentClusterIndex = endIndex;
            }
        }

        if (currentClusterIndex >= currentClusters.size()) {
            currentLayer++;
            if (currentLayer >= tier.maxLayers) {
                finished = true;
                return;
            }
            currentClusterIndex = 0;
            releaseCredit = 0.0;
            tickDelay = tier.layerDelayTicks;
            beginLayerScan();
            continueScan();
        }
    }

    /**
     * Resets the incremental scan cursor to the start of a new radius^3 cube centered on
     * originalCenter. Does not examine any positions itself - see continueScan().
     */
    private void beginLayerScan() {
        // Anchored on the original trigger point, not the player's current position -
        // a multi-layer cave-in must keep collapsing around where it started even if
        // the player wanders off mid-event (client-side shake/rumble effects are also
        // anchored on originalCenter, so this keeps the two in sync).
        scanRadius = tier.getScaledRadius(sizeMultiplier);
        scanRadiusSq = scanRadius * scanRadius;
        scanX = -scanRadius;
        scanY = -scanRadius;
        scanZ = -scanRadius;
        scanEligibleBlocks = new ArrayList<>();
        scanning = true;
    }

    /**
     * Examines up to SCAN_POSITIONS_PER_TICK positions of the current layer's cube, resuming
     * from wherever the previous call left off. When the whole cube has been examined, clusters
     * the collected eligible blocks into currentClusters (identical result to a single
     * synchronous pass, just spread across more calls) and clears the scanning flag.
     */
    private void continueScan() {
        PlacedBlockTracker tracker = PlacedBlockTracker.get(level);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int remainingBudget = SCAN_POSITIONS_PER_TICK;

        for (; scanX <= scanRadius; scanX++, scanY = -scanRadius) {
            for (; scanY <= scanRadius; scanY++, scanZ = -scanRadius) {
                for (; scanZ <= scanRadius; scanZ++) {
                    if (remainingBudget-- <= 0) {
                        return; // resumes at (scanX, scanY, scanZ) on the next call
                    }
                    examinePosition(mutable, tracker, scanX, scanY, scanZ);
                }
            }
        }

        scanning = false;
        currentClusters = clusterBlocks(scanEligibleBlocks);
        scanEligibleBlocks = null;
        if (currentClusters.isEmpty()) {
            finished = true;
        }
    }

    private void examinePosition(BlockPos.MutableBlockPos mutable, PlacedBlockTracker tracker, int x, int y, int z) {
        if (x * x + y * y + z * z > scanRadiusSq) return;
        mutable.set(originalCenter.getX() + x, originalCenter.getY() + y, originalCenter.getZ() + z);

        if (level.isOutsideBuildHeight(mutable.getY())) return;

        long packed = mutable.asLong();
        if (processedBlocks.contains(packed)) return;

        BlockState state = level.getBlockState(mutable);
        if (state.isAir() || !CaveInBlockRegistry.isAffected(state)) return;
        if (tracker.isPlayerPlaced(mutable)) return;

        // Skip surface blocks: if the block above can see sky, this is a surface block
        if (level.canSeeSky(mutable.above())) return;

        if (isAdjacentToAir(mutable)) {
            scanEligibleBlocks.add(mutable.immutable());
            processedBlocks.add(packed);
        }
    }

    private List<List<BlockPos>> clusterBlocks(List<BlockPos> blocks) {
        Set<BlockPos> remaining = new HashSet<>(blocks);
        List<List<BlockPos>> clusters = new ArrayList<>();

        while (!remaining.isEmpty()) {
            BlockPos seed = remaining.iterator().next();
            remaining.remove(seed);
            List<BlockPos> cluster = new ArrayList<>();
            cluster.add(seed);

            Queue<BlockPos> queue = new LinkedList<>();
            queue.add(seed);

            while (!queue.isEmpty() && cluster.size() < CLUSTER_SIZE) {
                BlockPos current = queue.poll();
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = current.relative(dir);
                    if (remaining.remove(neighbor)) {
                        cluster.add(neighbor);
                        queue.add(neighbor);
                        if (cluster.size() >= CLUSTER_SIZE) break;
                    }
                }
            }

            clusters.add(cluster);
        }

        Collections.shuffle(clusters);
        return clusters;
    }

    private boolean isAdjacentToAir(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(pos.relative(dir)).isAir()) {
                return true;
            }
        }
        return false;
    }

    public String getTierName() {
        return tier.id;
    }
}
