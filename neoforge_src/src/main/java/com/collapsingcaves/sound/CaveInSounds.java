package com.collapsingcaves.sound;

import com.collapsingcaves.CollapsingCaves;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CaveInSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, CollapsingCaves.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_LAND =
            SOUND_EVENTS.register("block_land", () -> SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath(CollapsingCaves.MOD_ID, "block_land")));

    public static final DeferredHolder<SoundEvent, SoundEvent> CAVE_RUMBLE =
            SOUND_EVENTS.register("cave_rumble", () -> SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath(CollapsingCaves.MOD_ID, "cave_rumble")));
}
