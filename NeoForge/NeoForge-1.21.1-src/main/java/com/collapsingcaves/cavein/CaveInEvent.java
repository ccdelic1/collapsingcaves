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

    // Never release faster than one cluster every PACE_DIVISOR ticks, even for a small/sparse layer.
    private static final int PACE_DIVISOR = 6;
    // Shared cap on how long a single layer may take to release, derived by solving
    // tier.maxLayers * (tier.layerDelayTicks + TARGET_LAYER_TICKS) ~= 3000 ticks (150s) for GARGANTUAN
    // (maxLayers=5, layerDelayTicks=12): 5 * (12 + T) = 3000 -> T = 588.
    private static final double TARGET_LAYER_TICKS = 588.0;

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
    private boolean finished = false;
    private double releaseRatePerTick;
    private double releaseCredit = 0.0;

    public CaveInEvent(ServerLevel level, ServerPlayer player, CaveInTier tier, double sizeMultiplier) {
        this.level = level;
        this.player = player;
        this.originalCenter = player.blockPosition();
        this.tier = tier;
        this.sizeMultiplier = sizeMultiplier;
        this.currentClusters = computeSurfaceLayer();
        if (this.currentClusters.isEmpty()) {
            this.finished = true;
        } else {
            startLayerPacing();
        }
    }

    private void startLayerPacing() {
        int layerClusterCount = currentClusters.size();
        releaseRatePerTick = Math.max(1.0 / PACE_DIVISOR, layerClusterCount / TARGET_LAYER_TICKS);
        releaseCredit = 0.0;
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

        if (tickDelay > 0) {
            tickDelay--;
            return;
        }

        if (currentClusterIndex < currentClusters.size()) {
            releaseCredit += releaseRatePerTick;
            int releaseCount = (int) Math.floor(releaseCredit);
            releaseCredit -= releaseCount;

            for (int i = 0; i < releaseCount && currentClusterIndex < currentClusters.size(); i++) {
                List<BlockPos> cluster = currentClusters.get(currentClusterIndex);
                for (BlockPos pos : cluster) {
                    BlockState state = level.getBlockState(pos);
                    if (CaveInBlockRegistry.isAffected(state) && !state.isAir()) {
                        FallingBlockEntity entity = FallingBlockEntity.fall(level, pos, state);
                        entity.setHurtsEntities(FALL_DAMAGE_PER_DISTANCE, FALL_DAMAGE_MAX);
                        entity.addTag("collapsingcaves_cavein");
                        entity.dropItem = false;
                    }
                }
                currentClusterIndex++;
            }
        }

        if (currentClusterIndex >= currentClusters.size()) {
            currentLayer++;
            if (currentLayer >= tier.maxLayers) {
                finished = true;
                return;
            }
            currentClusterIndex = 0;
            tickDelay = tier.layerDelayTicks;
            currentClusters = computeSurfaceLayer();
            if (currentClusters.isEmpty()) {
                finished = true;
            } else {
                startLayerPacing();
            }
        }
    }

    private List<List<BlockPos>> computeSurfaceLayer() {
        BlockPos center = player.blockPosition();
        int radius = tier.getScaledRadius(sizeMultiplier);
        int radiusSq = radius * radius;
        PlacedBlockTracker tracker = PlacedBlockTracker.get(level);
        List<BlockPos> eligibleBlocks = new ArrayList<>();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radiusSq) continue;
                    mutable.set(center.getX() + x, center.getY() + y, center.getZ() + z);

                    if (level.isOutsideBuildHeight(mutable.getY())) continue;

                    long packed = mutable.asLong();
                    if (processedBlocks.contains(packed)) continue;

                    BlockState state = level.getBlockState(mutable);
                    if (state.isAir() || !CaveInBlockRegistry.isAffected(state)) continue;
                    if (tracker.isPlayerPlaced(mutable)) continue;

                    // Skip surface blocks: if the block above can see sky, this is a surface block
                    if (level.canSeeSky(mutable.above())) continue;

                    if (isAdjacentToAir(mutable)) {
                        eligibleBlocks.add(mutable.immutable());
                        processedBlocks.add(packed);
                    }
                }
            }
        }

        return clusterBlocks(eligibleBlocks);
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
