package com.collapsingcaves.network;

import com.collapsingcaves.CollapsingCaves;
import com.collapsingcaves.cavein.CaveInTier;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class CaveInNetworking {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CollapsingCaves.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int nextPacketId = 0;

    public static void registerPayloads() {
        CHANNEL.registerMessage(nextPacketId++, CaveInPayload.class,
                CaveInPayload::encode, CaveInPayload::decode, CaveInPayload::handle);
        CHANNEL.registerMessage(nextPacketId++, CaveInStopPayload.class,
                CaveInStopPayload::encode, CaveInStopPayload::decode, CaveInStopPayload::handle);
    }

    public static void sendCaveInToNearbyPlayers(ServerLevel level, BlockPos center, CaveInTier tier) {
        double maxDist = tier.shakeMaxDistance;
        for (ServerPlayer player : level.players()) {
            double dist = player.blockPosition().distSqr(center);
            if (dist <= maxDist * maxDist) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CaveInPayload(center, tier.ordinal()));
            }
        }
    }

    public static void sendCaveInStopToNearbyPlayers(ServerLevel level, BlockPos center, CaveInTier tier) {
        double maxDist = tier.shakeMaxDistance;
        for (ServerPlayer player : level.players()) {
            double dist = player.blockPosition().distSqr(center);
            if (dist <= maxDist * maxDist) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CaveInStopPayload(center));
            }
        }
    }
}
