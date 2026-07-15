package com.collapsingcaves.client;

import com.collapsingcaves.cavein.CaveInTier;
import com.collapsingcaves.network.CaveInPayload;
import com.collapsingcaves.network.CaveInStopPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class CaveInClientNetworking {
    public static final List<CaveInRumbleInstance> activeRumbles = new ArrayList<>();

    public static void handleCaveInStart(CaveInPayload payload) {
        Minecraft.getInstance().execute(() -> {
            ScreenShakeHandler.startShake(payload.pos(), payload.tierOrdinal());

            CaveInTier tier = CaveInTier.values()[payload.tierOrdinal()];
            float volume = 1.0f + tier.ordinal() * 0.3f;
            CaveInRumbleInstance rumble = new CaveInRumbleInstance(payload.pos(), volume);
            activeRumbles.add(rumble);
            Minecraft.getInstance().getSoundManager().play(rumble);
        });
    }

    public static void handleCaveInStop(CaveInStopPayload payload) {
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

    public static void clearAll() {
        for (CaveInRumbleInstance rumble : activeRumbles) {
            rumble.stopRumble();
        }
        activeRumbles.clear();
    }
}
