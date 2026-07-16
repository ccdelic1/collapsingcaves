package com.collapsingcaves.tracking;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Tracks block positions placed via {@code BlockItem#placeBlock} (see BlockPlaceMixin) so
 * they're excluded from cave-ins. Entries are only removed when the position is later broken
 * through the mod's own break handling (CollapsingCaves#onInitialize); a position modified by
 * some other path - explosions, other mods/commands writing directly via Level#setBlock, etc.
 * - leaves a stale entry that permanently exempts that position from future cave-ins even after
 * the original placed block is long gone. This is an accepted trade-off of tracking placement
 * by position rather than by block identity/state: reconciling against every possible external
 * mutation source isn't practical, and a stale exemption is a minor false negative (a spot that
 * won't collapse) rather than an incorrect collapse, so it doesn't need active cleanup.
 */
public class PlacedBlockTracker extends SavedData {
    private static final String DATA_NAME = "collapsingcaves_placed_blocks";
    private static final String TAG_PLACED_POSITIONS = "placed_positions";
    private final LongOpenHashSet placedPositions;

    public PlacedBlockTracker() {
        this.placedPositions = new LongOpenHashSet();
    }

    private PlacedBlockTracker(LongOpenHashSet placedPositions) {
        this.placedPositions = placedPositions;
    }

    public static PlacedBlockTracker load(CompoundTag tag) {
        return new PlacedBlockTracker(new LongOpenHashSet(tag.getLongArray(TAG_PLACED_POSITIONS)));
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray(TAG_PLACED_POSITIONS, placedPositions.toLongArray());
        return tag;
    }

    public boolean isPlayerPlaced(BlockPos pos) {
        return placedPositions.contains(pos.asLong());
    }

    public void markPlaced(BlockPos pos) {
        placedPositions.add(pos.asLong());
        setDirty();
    }

    public void removePlaced(BlockPos pos) {
        if (placedPositions.remove(pos.asLong())) {
            setDirty();
        }
    }

    public static PlacedBlockTracker get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PlacedBlockTracker::load, PlacedBlockTracker::new, DATA_NAME);
    }
}
