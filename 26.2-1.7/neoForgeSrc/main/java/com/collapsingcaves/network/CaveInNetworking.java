package com.collapsingcaves.network;

import com.collapsingcaves.cavein.CaveInTier;
import com.collapsingcaves.client.CaveInClientNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class CaveInNetworking {
   private CaveInNetworking() {
   }

   public static void registerPayloads(IEventBus modEventBus) {
      modEventBus.addListener(
         RegisterPayloadHandlersEvent.class,
         event -> {
            PayloadRegistrar registrar = event.registrar("1");
            registrar.commonToClient(CaveInPayload.TYPE, CaveInPayload.STREAM_CODEC, (payload, context) -> CaveInClientNetworking.handleCaveInStart(payload));
            registrar.commonToClient(
               CaveInStopPayload.TYPE, CaveInStopPayload.STREAM_CODEC, (payload, context) -> CaveInClientNetworking.handleCaveInStop(payload)
            );
         }
      );
   }

   public static void sendCaveInStart(ServerLevel level, BlockPos center, CaveInTier tier) {
      double maxDistance = tier.effectRange();

      for (ServerPlayer player : level.players()) {
         if (player.blockPosition().distSqr(center) <= maxDistance * maxDistance) {
            PacketDistributor.sendToPlayer(player, new CaveInPayload(center, tier.ordinal()), new CustomPacketPayload[0]);
         }
      }
   }

   public static void sendCaveInStop(ServerLevel level, BlockPos center) {
      for (ServerPlayer player : level.players()) {
         PacketDistributor.sendToPlayer(player, new CaveInStopPayload(center), new CustomPacketPayload[0]);
      }
   }
}
