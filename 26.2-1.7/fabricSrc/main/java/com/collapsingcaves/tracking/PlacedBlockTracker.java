package com.collapsingcaves.tracking;

import com.collapsingcaves.CollapsingCaves;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class PlacedBlockTracker extends SavedData {
   private static final String DATA_NAME = "collapsingcaves_placed_blocks";
   private final LongOpenHashSet placedPositions = new LongOpenHashSet();
   public static final Codec<PlacedBlockTracker> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(Codec.LONG.listOf().fieldOf("placed_positions").forGetter(PlacedBlockTracker::positionList))
         .apply(instance, PlacedBlockTracker::fromPositionList)
   );
   public static final SavedDataType<PlacedBlockTracker> TYPE = new SavedDataType(
      CollapsingCaves.id(DATA_NAME), PlacedBlockTracker::new, CODEC, DataFixTypes.LEVEL
   );

   private List<Long> positionList() {
      List<Long> positions = new ArrayList<>(this.placedPositions.size());
      LongIterator var2 = this.placedPositions.iterator();

      while (var2.hasNext()) {
         long position = (Long)var2.next();
         positions.add(position);
      }

      return positions;
   }

   private static PlacedBlockTracker fromPositionList(List<Long> positions) {
      PlacedBlockTracker tracker = new PlacedBlockTracker();

      for (Long position : positions) {
         tracker.placedPositions.add(position);
      }

      return tracker;
   }

   public boolean isPlayerPlaced(BlockPos pos) {
      return this.placedPositions.contains(pos.asLong());
   }

   public void markPlaced(BlockPos pos) {
      this.placedPositions.add(pos.asLong());
      this.setDirty();
   }

   public void removePlaced(BlockPos pos) {
      if (this.placedPositions.remove(pos.asLong())) {
         this.setDirty();
      }
   }

   public static PlacedBlockTracker get(ServerLevel level) {
      return (PlacedBlockTracker)level.getDataStorage().computeIfAbsent(TYPE);
   }
}
