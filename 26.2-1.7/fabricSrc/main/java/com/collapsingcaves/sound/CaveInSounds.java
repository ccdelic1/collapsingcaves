package com.collapsingcaves.sound;

import com.collapsingcaves.CollapsingCaves;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

public final class CaveInSounds {
   private static final SoundEvent BLOCK_LAND = SoundEvent.createVariableRangeEvent(CollapsingCaves.id("block_land"));
   private static final SoundEvent CAVE_RUMBLE = SoundEvent.createVariableRangeEvent(CollapsingCaves.id("cave_rumble"));

   private CaveInSounds() {
   }

   public static void register() {
      Registry.register(BuiltInRegistries.SOUND_EVENT, CollapsingCaves.id("block_land"), BLOCK_LAND);
      Registry.register(BuiltInRegistries.SOUND_EVENT, CollapsingCaves.id("cave_rumble"), CAVE_RUMBLE);
   }

   public static SoundEvent blockLand() {
      return BLOCK_LAND;
   }

   public static SoundEvent caveRumble() {
      return CAVE_RUMBLE;
   }
}
