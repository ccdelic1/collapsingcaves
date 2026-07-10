package com.collapsingcaves.network;

import com.collapsingcaves.cavein.CaveInTier;
import com.collapsingcaves.client.CaveInRumbleInstance;
import com.collapsingcaves.client.ScreenShakeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

public class CaveInNetworking {
    public static final List<CaveInRumbleInstance> activeRumbles = new ArrayList<>();

    public static void registerPayloads(IEventBus modEventBus) {
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            PayloadRegistrar registrar = event.registrar("1");
            registrar.commonToClient(CaveInPayload.TYPE, CaveInPayload.STREAM_CODEC,
                    (payload, context) -> handleCaveInStart(payload));
            registrar.commonToClient(CaveInStopPayload.TYPE, CaveInStopPayload.STREAM_CODEC,
                    (payload, context) -> handleCaveInStop(payload));
        });
    }

    private static void handleCaveInStart(CaveInPayload payload) {
        Minecraft.getInstance().execute(() -> {
            ScreenShakeHandler.startShake(payload.pos(), payload.tierOrdinal());

            CaveInTier tier = CaveInTier.values()[payload.tierOrdinal()];
            float volume = 1.0f + tier.ordinal() * 0.3f;
            CaveInRumbleInstance rumble = new CaveInRumbleInstance(payload.pos(), volume);
            activeRumbles.add(rumble);
            Minecraft.getInstance().getSoundManager().play(rumble);
        });
    }

    private static void handleCaveInStop(CaveInStopPayload payload) {
        Minecraft.getInstance().execute(() -> {
            ScreenShakeHandler.stopShake(payload.pos());
            activeRumbles.removeIf(rumble -> {
                if (rumble.getCenter().equals(payload.pos())) {
                    rumble.stopRumble();
                    return true;
                }
                return false;
            });
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
