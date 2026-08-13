package com.collapsingcaves.network;

import com.collapsingcaves.CollapsingCaves;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record CaveInPayload(BlockPos pos, int tierOrdinal) implements CustomPacketPayload {
   public static final Type<CaveInPayload> TYPE = new Type(CollapsingCaves.id("cave_in"));
   public static final StreamCodec<FriendlyByteBuf, CaveInPayload> STREAM_CODEC = new StreamCodec<FriendlyByteBuf, CaveInPayload>() {
      public CaveInPayload decode(FriendlyByteBuf buf) {
         return new CaveInPayload(buf.readBlockPos(), buf.readVarInt());
      }

      public void encode(FriendlyByteBuf buf, CaveInPayload payload) {
         buf.writeBlockPos(payload.pos());
         buf.writeVarInt(payload.tierOrdinal());
      }
   };

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

}
