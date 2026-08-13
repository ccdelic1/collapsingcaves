package com.collapsingcaves;

import com.collapsingcaves.client.CaveInClientNetworking;
import com.collapsingcaves.client.CaveInGreeting;
import com.collapsingcaves.client.ScreenShakeHandler;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingIn;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

@Mod(value = "collapsingcaves", dist = Dist.CLIENT)
public class CollapsingCavesClient {
   @EventBusSubscriber(modid = "collapsingcaves", value = Dist.CLIENT)
   public static class ClientEvents {
      @SubscribeEvent
      static void onClientTick(Post event) {
         ScreenShakeHandler.tick();
         CaveInClientNetworking.ACTIVE_RUMBLES.removeIf(AbstractTickableSoundInstance::isStopped);
      }

      @SubscribeEvent
      static void onLoggingIn(LoggingIn event) {
         CaveInGreeting.showOnce();
      }

      @SubscribeEvent
      static void onLoggingOut(LoggingOut event) {
         ScreenShakeHandler.reset();
         CaveInClientNetworking.clearAll();
      }
   }
}
