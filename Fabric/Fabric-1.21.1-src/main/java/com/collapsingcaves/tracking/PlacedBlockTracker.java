package com.collapsingcaves.tracking;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public class PlacedBlockTracker extends SavedData {
    private static final String DATA_NAME = "collapsingcaves_placed_blocks";
    private static final String TAG_PLACED_POSITIONS = "placed_positions";
    private final LongOpenHashSet placedPositions;

    public static final SavedData.Factory<PlacedBlockTracker> FACTORY = new SavedData.Factory<>(
            PlacedBlockTracker::new, PlacedBlockTracker::load, DataFixTypes.LEVEL
    );

    public PlacedBlockTracker() {
        this.placedPositions = new LongOpenHashSet();
    }

    private PlacedBlockTracker(LongOpenHashSet placedPositions) {
        this.placedPositions = placedPositions;
    }

    public static PlacedBlockTracker load(CompoundTag tag, HolderLookup.Provider registries) {
        return new PlacedBlockTracker(new LongOpenHashSet(tag.getLongArray(TAG_PLACED_POSITIONS)));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
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
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }
}
