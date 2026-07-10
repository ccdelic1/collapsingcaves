package com.collapsingcaves.network;

import com.collapsingcaves.cavein.CaveInTier;
import com.collapsingcaves.client.CaveInRumbleInstance;
import com.collapsingcaves.client.ScreenShakeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

import java.util.ArrayList;
import java.util.List;

public class CaveInNetworking {
    public static final List<CaveInRumbleInstance> activeRumbles = new ArrayList<>();

    private static final SimpleChannel CHANNEL = ChannelBuilder
            .named(Identifier.fromNamespaceAndPath("collapsingcaves", "main"))
            .networkProtocolVersion(1)
            .acceptedVersions(Channel.VersionTest.exact(1))
            .simpleChannel();

    public static void registerPayloads() {
        CHANNEL.messageBuilder(CaveInPayload.class, 0)
                .codec(CaveInPayload.STREAM_CODEC)
                .consumerMainThread((payload, context) -> handleCaveInStart(payload))
                .add();

        CHANNEL.messageBuilder(CaveInStopPayload.class, 1)
                .codec(CaveInStopPayload.STREAM_CODEC)
                .consumerMainThread((payload, context) -> handleCaveInStop(payload))
                .add();
    }

    private static void handleCaveInStart(CaveInPayload payload) {
        ScreenShakeHandler.startShake(payload.pos(), payload.tierOrdinal());

        CaveInTier tier = CaveInTier.values()[payload.tierOrdinal()];
        float volume = 1.0f + tier.ordinal() * 0.3f;
        CaveInRumbleInstance rumble = new CaveInRumbleInstance(payload.pos(), volume);
        activeRumbles.add(rumble);
        Minecraft.getInstance().getSoundManager().play(rumble);
    }

    private static void handleCaveInStop(CaveInStopPayload payload) {
        ScreenShakeHandler.stopShake(payload.pos());
        activeRumbles.removeIf(rumble -> {
            if (rumble.getCenter().equals(payload.pos())) {
                rumble.stopRumble();
                return true;
            }
            return false;
        });
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
