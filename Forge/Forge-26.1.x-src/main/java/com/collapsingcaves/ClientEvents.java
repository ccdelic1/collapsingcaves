package com.collapsingcaves;

import com.collapsingcaves.client.CaveInRumbleInstance;
import com.collapsingcaves.client.ScreenShakeHandler;
import com.collapsingcaves.network.CaveInNetworking;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CollapsingCaves.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        ScreenShakeHandler.tick();
        CaveInNetworking.activeRumbles.removeIf(CaveInRumbleInstance::isStopped);
    }
}
