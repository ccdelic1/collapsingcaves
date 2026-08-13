package com.collapsingcaves;

import com.collapsingcaves.client.CaveInClientNetworking;
import com.collapsingcaves.client.ScreenShakeHandler;
import com.collapsingcaves.network.CaveInPayload;
import com.collapsingcaves.network.CaveInStopPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import com.collapsingcaves.client.CaveInGreeting;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;

public class CollapsingCavesClient implements ClientModInitializer {
   public void onInitializeClient() {
      ClientPlayNetworking.registerGlobalReceiver(CaveInPayload.TYPE, (payload, context) -> CaveInClientNetworking.handleCaveInStart(payload));
      ClientPlayNetworking.registerGlobalReceiver(CaveInStopPayload.TYPE, (payload, context) -> CaveInClientNetworking.handleCaveInStop(payload));
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         ScreenShakeHandler.tick();
         CaveInClientNetworking.ACTIVE_RUMBLES.removeIf(AbstractTickableSoundInstance::isStopped);
      });
      ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> CaveInGreeting.showOnce());
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> {
         ScreenShakeHandler.reset();
         CaveInClientNetworking.clearAll();
      });
   }
}
