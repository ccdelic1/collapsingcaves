package com.collapsingcaves.network;

import com.collapsingcaves.cavein.CaveInTier;
import com.collapsingcaves.client.ClientPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public class CaveInNetworking {
    private static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath("collapsingcaves", "main"))
            .networkProtocolVersion(1)
            .acceptedVersions(Channel.VersionTest.exact(1))
            .simpleChannel();

    public static void registerPayloads() {
        CHANNEL.messageBuilder(CaveInPayload.class, 0)
                .codec(CaveInPayload.STREAM_CODEC)
                .consumerMainThread((payload, context) ->
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleCaveInStart(payload)))
                .add();

        CHANNEL.messageBuilder(CaveInStopPayload.class, 1)
                .codec(CaveInStopPayload.STREAM_CODEC)
                .consumerMainThread((payload, context) ->
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleCaveInStop(payload)))
                .add();
    }

    public static void sendCaveInToNearbyPlayers(ServerLevel level, BlockPos center, CaveInTier tier) {
        double maxDist = tier.shakeMaxDistance;
        for (ServerPlayer player : level.players()) {
            double dist = player.blockPosition().distSqr(center);
            if (dist <= maxDist * maxDist) {
                CHANNEL.send(new CaveInPayload(center, tier.ordinal()), PacketDistributor.PLAYER.with(player));
            }
        }
    }

    public static void sendCaveInStopToNearbyPlayers(ServerLevel level, BlockPos center, CaveInTier tier) {
        double maxDist = tier.shakeMaxDistance;
        for (ServerPlayer player : level.players()) {
            double dist = player.blockPosition().distSqr(center);
            if (dist <= maxDist * maxDist) {
                CHANNEL.send(new CaveInStopPayload(center), PacketDistributor.PLAYER.with(player));
            }
        }
    }
}
