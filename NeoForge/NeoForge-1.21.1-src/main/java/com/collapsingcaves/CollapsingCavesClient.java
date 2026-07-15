package com.collapsingcaves;

import com.collapsingcaves.client.CaveInClientNetworking;
import com.collapsingcaves.client.CaveInRumbleInstance;
import com.collapsingcaves.client.ScreenShakeHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@Mod(value = CollapsingCaves.MOD_ID, dist = Dist.CLIENT)
public class CollapsingCavesClient {

    public CollapsingCavesClient() {
        // Payload registration is handled by CaveInNetworking.registerPayloads()
        // in the main CollapsingCaves mod constructor to avoid duplicate registration.
    }

    @EventBusSubscriber(modid = CollapsingCaves.MOD_ID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        static void onClientTick(ClientTickEvent.Post event) {
            ScreenShakeHandler.tick();
            CaveInClientNetworking.activeRumbles.removeIf(CaveInRumbleInstance::isStopped);
        }

        @SubscribeEvent
        static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            // A player can walk out of a cave-in's shakeMaxDistance (or disconnect entirely)
            // before the server sends the stop packet, which would otherwise leave the shake
            // source and looping rumble sound tracked forever. Force-clear on every disconnect.
            ScreenShakeHandler.reset();
            CaveInClientNetworking.clearAll();
        }
    }
}
