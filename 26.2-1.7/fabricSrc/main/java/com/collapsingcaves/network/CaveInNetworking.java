package com.collapsingcaves.network;

import com.collapsingcaves.cavein.CaveInTier;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class CaveInNetworking {
   private CaveInNetworking() {
   }

   public static void registerPayloads() {
      PayloadTypeRegistry.clientboundPlay().register(CaveInPayload.TYPE, CaveInPayload.STREAM_CODEC);
      PayloadTypeRegistry.clientboundPlay().register(CaveInStopPayload.TYPE, CaveInStopPayload.STREAM_CODEC);
   }

   public static void sendCaveInStart(ServerLevel level, BlockPos center, CaveInTier tier) {
      double maxDistance = tier.effectRange();

      for (ServerPlayer player : level.players()) {
         if (player.blockPosition().distSqr(center) <= maxDistance * maxDistance) {
            ServerPlayNetworking.send(player, new CaveInPayload(center, tier.ordinal()));
         }
      }
   }

   public static void sendCaveInStop(ServerLevel level, BlockPos center) {
      for (ServerPlayer player : level.players()) {
         ServerPlayNetworking.send(player, new CaveInStopPayload(center));
      }
   }
}
