package com.collapsingcaves;

import com.collapsingcaves.cavein.CaveInTier;
import com.collapsingcaves.client.CaveInRumbleInstance;
import com.collapsingcaves.client.ScreenShakeHandler;
import com.collapsingcaves.network.CaveInPayload;
import com.collapsingcaves.network.CaveInStopPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class CollapsingCavesClient implements ClientModInitializer {
    private static final List<CaveInRumbleInstance> activeRumbles = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        // Register network receiver for cave-in start
        ClientPlayNetworking.registerGlobalReceiver(CaveInPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                ScreenShakeHandler.startShake(payload.pos(), payload.tierOrdinal());

                // Start looping rumble sound
                CaveInTier tier = CaveInTier.values()[payload.tierOrdinal()];
                float volume = 1.0f + tier.ordinal() * 0.3f;
                CaveInRumbleInstance rumble = new CaveInRumbleInstance(payload.pos(), volume);
                activeRumbles.add(rumble);
                Minecraft.getInstance().getSoundManager().play(rumble);
            });
        });

        // Register network receiver for cave-in stop
        ClientPlayNetworking.registerGlobalReceiver(CaveInStopPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                ScreenShakeHandler.stopShake(payload.pos());
                activeRumbles.removeIf(rumble -> {
                    if (rumble.getCenter().equals(payload.pos())) {
                        rumble.stopRumble();
                        return true;
                    }
                    return false;
                });
            });
        });

        // Tick the screen shake handler and clean up stopped rumbles
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ScreenShakeHandler.tick();
            activeRumbles.removeIf(CaveInRumbleInstance::isStopped);
        });
    }
}
