package com.collapsingcaves.client;

import com.collapsingcaves.sound.CaveInSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

public class CaveInRumbleInstance extends AbstractTickableSoundInstance {
   private final BlockPos center;

   public CaveInRumbleInstance(BlockPos center, float volume) {
      super(CaveInSounds.caveRumble(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
      this.center = center;
      this.x = center.getX() + 0.5;
      this.y = center.getY() + 0.5;
      this.z = center.getZ() + 0.5;
      this.looping = true;
      this.volume = volume;
      this.attenuation = Attenuation.LINEAR;
   }

   public BlockPos getCenter() {
      return this.center;
   }

   public void stopRumble() {
      this.stop();
   }

   public void tick() {
   }
}
