package com.collapsingcaves.network;

import com.collapsingcaves.cavein.CaveInTier;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class CaveInNetworking {
    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(CaveInPayload.TYPE, CaveInPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(CaveInStopPayload.TYPE, CaveInStopPayload.STREAM_CODEC);
    }

    public static void sendCaveInToNearbyPlayers(ServerLevel level, BlockPos center, CaveInTier tier) {
        double maxDist = tier.shakeMaxDistance;
        for (ServerPlayer player : level.players()) {
            double dist = player.blockPosition().distSqr(center);
            if (dist <= maxDist * maxDist) {
                ServerPlayNetworking.send(player, new CaveInPayload(center, tier.ordinal()));
            }
        }
    }

    public static void sendCaveInStopToNearbyPlayers(ServerLevel level, BlockPos center, CaveInTier tier) {
        double maxDist = tier.shakeMaxDistance;
        for (ServerPlayer player : level.players()) {
            double dist = player.blockPosition().distSqr(center);
            if (dist <= maxDist * maxDist) {
                ServerPlayNetworking.send(player, new CaveInStopPayload(center));
            }
        }
    }
}
