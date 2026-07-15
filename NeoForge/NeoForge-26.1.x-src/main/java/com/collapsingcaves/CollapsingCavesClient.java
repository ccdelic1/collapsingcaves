package com.collapsingcaves;

import com.collapsingcaves.client.CaveInRumbleInstance;
import com.collapsingcaves.client.ScreenShakeHandler;
import com.collapsingcaves.network.CaveInNetworking;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
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
            CaveInNetworking.activeRumbles.removeIf(CaveInRumbleInstance::isStopped);
        }
    }
}
