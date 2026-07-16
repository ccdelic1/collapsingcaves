package com.collapsingcaves.mixin.client;

import com.collapsingcaves.client.ScreenShakeHandler;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(Camera.class)
public abstract class CameraShakeMixin {
    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    private float yRot;

    @Shadow
    private float xRot;

    // Reused instead of allocating a new Random every render frame - reseeded per
    // frame below, which is all that's needed to vary the jitter frame to frame.
    private final Random collapsingcaves$shakeRandom = new Random();

    @Inject(method = "setup", at = @At("RETURN"))
    private void collapsingcaves$applyScreenShake(BlockGetter area, Entity focusedEntity, boolean thirdPerson,
                                                    boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        float intensity = ScreenShakeHandler.getShakeIntensity();
        if (intensity > 0.01f) {
            collapsingcaves$shakeRandom.setSeed(ScreenShakeHandler.getShakeSeed() + (long) (partialTick * 1000));
            float yawOffset = (collapsingcaves$shakeRandom.nextFloat() - 0.5f) * intensity * 2.0f;
            float pitchOffset = (collapsingcaves$shakeRandom.nextFloat() - 0.5f) * intensity * 1.0f;
            setRotation(this.yRot + yawOffset, this.xRot + pitchOffset);
        }
    }
}
