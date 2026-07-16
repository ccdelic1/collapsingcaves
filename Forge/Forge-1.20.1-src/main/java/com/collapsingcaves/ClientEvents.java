package com.collapsingcaves;

import com.collapsingcaves.client.CaveInRumbleInstance;
import com.collapsingcaves.client.ClientPacketHandler;
import com.collapsingcaves.client.ScreenShakeHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CollapsingCaves.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ScreenShakeHandler.tick();
        ClientPacketHandler.activeRumbles.removeIf(CaveInRumbleInstance::isStopped);
    }

    @SubscribeEvent
    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        // A player can walk out of a cave-in's shakeMaxDistance (or disconnect entirely)
        // before the server sends the stop packet, which would otherwise leave the shake
        // source and looping rumble sound tracked forever. Force-clear on every disconnect.
        ScreenShakeHandler.reset();
        ClientPacketHandler.activeRumbles.forEach(CaveInRumbleInstance::stopRumble);
        ClientPacketHandler.activeRumbles.clear();
    }
}
