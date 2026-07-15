package com.collapsingcaves.sound;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class CaveInSounds {
    public static final SoundEvent BLOCK_LAND = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("collapsingcaves", "block_land")
    );

    public static final SoundEvent CAVE_RUMBLE = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("collapsingcaves", "cave_rumble")
    );

    public static void register() {
        Registry.register(BuiltInRegistries.SOUND_EVENT,
                ResourceLocation.fromNamespaceAndPath("collapsingcaves", "block_land"), BLOCK_LAND);
        Registry.register(BuiltInRegistries.SOUND_EVENT,
                ResourceLocation.fromNamespaceAndPath("collapsingcaves", "cave_rumble"), CAVE_RUMBLE);
    }
}
