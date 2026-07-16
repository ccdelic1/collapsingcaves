package com.collapsingcaves.tracking;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class PlacedBlockTracker extends SavedData {
    private static final String DATA_NAME = "collapsingcaves_placed_blocks";
    private final LongOpenHashSet placedPositions;

    private PlacedBlockTracker(LongOpenHashSet placedPositions) {
        this.placedPositions = placedPositions;
    }

    public PlacedBlockTracker() {
        this(new LongOpenHashSet());
    }

    public static PlacedBlockTracker load(CompoundTag tag) {
        return new PlacedBlockTracker(new LongOpenHashSet(tag.getLongArray("placed_positions")));
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("placed_positions", placedPositions.toLongArray());
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
