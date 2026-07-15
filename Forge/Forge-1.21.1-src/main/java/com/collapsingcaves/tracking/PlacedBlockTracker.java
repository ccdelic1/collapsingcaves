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
    private final LongOpenHashSet placedPositions;

    private PlacedBlockTracker(LongOpenHashSet placedPositions) {
        this.placedPositions = placedPositions;
    }

    public PlacedBlockTracker() {
        this(new LongOpenHashSet());
    }

    public static PlacedBlockTracker load(CompoundTag tag, HolderLookup.Provider provider) {
        return new PlacedBlockTracker(new LongOpenHashSet(tag.getLongArray("placed_positions")));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putLongArray("placed_positions", placedPositions.toLongArray());
        return tag;
    }

    private static SavedData.Factory<PlacedBlockTracker> factory() {
        return new SavedData.Factory<>(PlacedBlockTracker::new, PlacedBlockTracker::load, DataFixTypes.LEVEL);
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
        return level.getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }
}
