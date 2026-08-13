package com.collapsingcaves.network;

import com.collapsingcaves.CollapsingCaves;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record CaveInStopPayload(BlockPos pos) implements CustomPacketPayload {
   public static final Type<CaveInStopPayload> TYPE = new Type(CollapsingCaves.id("cave_in_stop"));
   public static final StreamCodec<FriendlyByteBuf, CaveInStopPayload> STREAM_CODEC = new StreamCodec<FriendlyByteBuf, CaveInStopPayload>() {
      public CaveInStopPayload decode(FriendlyByteBuf buf) {
         return new CaveInStopPayload(buf.readBlockPos());
      }

      public void encode(FriendlyByteBuf buf, CaveInStopPayload payload) {
         buf.writeBlockPos(payload.pos());
      }
   };


   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

}
