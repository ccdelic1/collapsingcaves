package com.collapsingcaves.client;

import com.collapsingcaves.cavein.CaveInTier;
import com.collapsingcaves.network.CaveInPayload;
import com.collapsingcaves.network.CaveInStopPayload;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-only payload handling. Kept out of {@link com.collapsingcaves.network.CaveInNetworking}
 * so that class can be loaded on a dedicated server, where client classes
 * (Minecraft, SoundInstance, ...) are stripped by the dist cleaner.
 */
@OnlyIn(Dist.CLIENT)
public class ClientPayloadHandler {
    public static final List<CaveInRumbleInstance> activeRumbles = new ArrayList<>();

    public static void handleCaveInStart(CaveInPayload payload) {
        ScreenShakeHandler.startShake(payload.pos(), payload.tierOrdinal());

        CaveInTier tier = CaveInTier.values()[payload.tierOrdinal()];
        // Gain is clamped at 1.0 by the sound engine; values above 1 extend the
        // audible range of the variable-range event so distant players hear it too.
        float volume = 2.0f + tier.ordinal() * 0.5f;
        CaveInRumbleInstance rumble = new CaveInRumbleInstance(payload.pos(), volume);
        activeRumbles.add(rumble);
        Minecraft.getInstance().getSoundManager().play(rumble);
    }

    public static void handleCaveInStop(CaveInStopPayload payload) {
        ScreenShakeHandler.stopShake(payload.pos());
        activeRumbles.removeIf(rumble -> {
            if (rumble.getCenter().equals(payload.pos())) {
                rumble.stopRumble();
                return true;
            }
            return false;
        });
    }
}
