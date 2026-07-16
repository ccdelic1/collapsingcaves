package com.collapsingcaves.tracking;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class PlacedBlockTracker extends SavedData {
    private static final String DATA_NAME = "collapsingcaves_placed_blocks";
    private static final String NBT_KEY = "placed_positions";
    private final LongOpenHashSet placedPositions = new LongOpenHashSet();

    public PlacedBlockTracker() {
    }

    public static PlacedBlockTracker load(CompoundTag tag) {
        PlacedBlockTracker tracker = new PlacedBlockTracker();
        for (long pos : tag.getLongArray(NBT_KEY)) {
            tracker.placedPositions.add(pos);
        }
        return tracker;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray(NBT_KEY, placedPositions.toLongArray());
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
