package com.collapsingcaves.sound;

import com.collapsingcaves.CollapsingCaves;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CaveInSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CollapsingCaves.MOD_ID);

    public static final RegistryObject<SoundEvent> BLOCK_LAND =
            SOUND_EVENTS.register("block_land", () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(CollapsingCaves.MOD_ID, "block_land")));

    public static final RegistryObject<SoundEvent> CAVE_RUMBLE =
            SOUND_EVENTS.register("cave_rumble", () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(CollapsingCaves.MOD_ID, "cave_rumble")));
}
