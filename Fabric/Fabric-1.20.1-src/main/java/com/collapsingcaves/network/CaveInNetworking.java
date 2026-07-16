package com.collapsingcaves.network;

import com.collapsingcaves.cavein.CaveInTier;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class CaveInNetworking {
    public static void sendCaveInToNearbyPlayers(ServerLevel level, BlockPos center, CaveInTier tier) {
        double maxDist = tier.shakeMaxDistance;
        for (ServerPlayer player : level.players()) {
            double dist = player.blockPosition().distSqr(center);
            if (dist <= maxDist * maxDist) {
                FriendlyByteBuf buf = PacketByteBufs.create();
                new CaveInPayload(center, tier.ordinal()).encode(buf);
                ServerPlayNetworking.send(player, CaveInPayload.CHANNEL, buf);
            }
        }
    }

    public static void sendCaveInStopToAllPlayers(ServerLevel level, BlockPos center) {
        // Unlike the start payload, the stop payload is broadcast to every player in the
        // level regardless of current distance from center. A player who was in range
        // when the cave-in started (and so has an active shake source / looping rumble
        // queued client-side) may have walked out of shakeMaxDistance by the time it
        // ends; filtering by distance here would leave their client-side state stuck
        // active forever. Sending to everyone is cheap (a single BlockPos) and a no-op
        // on clients that were never actually shaking/rumbling for this center.
        for (ServerPlayer player : level.players()) {
            FriendlyByteBuf buf = PacketByteBufs.create();
            new CaveInStopPayload(center).encode(buf);
            ServerPlayNetworking.send(player, CaveInStopPayload.CHANNEL, buf);
        }
    }
}
