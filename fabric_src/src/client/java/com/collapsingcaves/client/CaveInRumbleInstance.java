package com.collapsingcaves.client;

import com.collapsingcaves.sound.CaveInSounds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

@Environment(EnvType.CLIENT)
public class CaveInRumbleInstance extends AbstractTickableSoundInstance {
    private final BlockPos center;

    public CaveInRumbleInstance(BlockPos center, float volume) {
        super(CaveInSounds.CAVE_RUMBLE, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.center = center;
        this.x = center.getX() + 0.5;
        this.y = center.getY() + 0.5;
        this.z = center.getZ() + 0.5;
        this.looping = true;
        this.volume = volume;
        this.attenuation = Attenuation.LINEAR;
    }

    public BlockPos getCenter() {
        return center;
    }

    public void stopRumble() {
        this.stop();
    }

    @Override
    public void tick() {
        // Looping is handled by the sound engine; stop() called externally
    }
}
