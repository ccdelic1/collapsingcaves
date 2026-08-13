package com.collapsingcaves.sound;

import com.collapsingcaves.CollapsingCaves;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CaveInSounds {
   public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, CollapsingCaves.MOD_ID);
   private static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_LAND = SOUND_EVENTS.register(
      "block_land", () -> SoundEvent.createVariableRangeEvent(CollapsingCaves.id("block_land"))
   );
   private static final DeferredHolder<SoundEvent, SoundEvent> CAVE_RUMBLE = SOUND_EVENTS.register(
      "cave_rumble", () -> SoundEvent.createVariableRangeEvent(CollapsingCaves.id("cave_rumble"))
   );

   private CaveInSounds() {
   }

   public static SoundEvent blockLand() {
      return (SoundEvent)BLOCK_LAND.get();
   }

   public static SoundEvent caveRumble() {
      return (SoundEvent)CAVE_RUMBLE.get();
   }
}
