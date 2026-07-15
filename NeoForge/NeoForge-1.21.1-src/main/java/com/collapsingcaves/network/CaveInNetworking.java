package com.collapsingcaves.network;

import com.collapsingcaves.cavein.CaveInTier;
import com.collapsingcaves.client.CaveInClientNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class CaveInNetworking {
    public static void registerPayloads(IEventBus modEventBus) {
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            PayloadRegistrar registrar = event.registrar("1");
            registrar.commonToClient(CaveInPayload.TYPE, CaveInPayload.STREAM_CODEC,
                    (payload, context) -> CaveInClientNetworking.handleCaveInStart(payload));
            registrar.commonToClient(CaveInStopPayload.TYPE, CaveInStopPayload.STREAM_CODEC,
                    (payload, context) -> CaveInClientNetworking.handleCaveInStop(payload));
        });
    }

    public static void sendCaveInToNearbyPlayers(ServerLevel level, BlockPos center, CaveInTier tier) {
        double maxDist = tier.shakeMaxDistance;
        for (ServerPlayer player : level.players()) {
            double dist = player.blockPosition().distSqr(center);
            if (dist <= maxDist * maxDist) {
                PacketDistributor.sendToPlayer(player, new CaveInPayload(center, tier.ordinal()));
            }
        }
    }

    public static void sendCaveInStopToNearbyPlayers(ServerLevel level, BlockPos center, CaveInTier tier) {
        double maxDist = tier.shakeMaxDistance;
        for (ServerPlayer player : level.players()) {
            double dist = player.blockPosition().distSqr(center);
            if (dist <= maxDist * maxDist) {
                PacketDistributor.sendToPlayer(player, new CaveInStopPayload(center));
            }
        }
    }
}
