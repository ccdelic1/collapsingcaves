package com.collapsingcaves.tracking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.List;
import java.util.stream.Collectors;

public class PlacedBlockTracker extends SavedData {
    private static final String DATA_NAME = "collapsingcaves_placed_blocks";
    private final LongOpenHashSet placedPositions = new LongOpenHashSet();

    public static final Codec<PlacedBlockTracker> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.LONG.listOf().fieldOf("placed_positions").forGetter(
                            tracker -> tracker.placedPositions.longStream().boxed().collect(Collectors.toList())
                    )
            ).apply(instance, positionsList -> {
                PlacedBlockTracker tracker = new PlacedBlockTracker();
                for (Long pos : positionsList) {
                    tracker.placedPositions.add(pos.longValue());
                }
                return tracker;
            })
    );

    public static final SavedDataType<PlacedBlockTracker> TYPE = new SavedDataType<>(
            DATA_NAME, PlacedBlockTracker::new, CODEC, DataFixTypes.LEVEL
    );

    public PlacedBlockTracker() {
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
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}
