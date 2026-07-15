package com.collapsingcaves;

import com.collapsingcaves.client.CaveInRumbleInstance;
import com.collapsingcaves.client.ClientPacketHandler;
import com.collapsingcaves.client.ScreenShakeHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CollapsingCaves.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        ScreenShakeHandler.tick();
        ClientPacketHandler.activeRumbles.removeIf(CaveInRumbleInstance::isStopped);
    }
}
