package com.collapsingcaves.mixin.client;

import com.collapsingcaves.client.ScreenShakeHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
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

    @Inject(method = "update", at = @At("RETURN"))
    private void collapsingcaves$applyScreenShake(DeltaTracker deltaTracker, CallbackInfo ci) {
        float intensity = ScreenShakeHandler.getShakeIntensity();
        if (intensity > 0.01f) {
            float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
            Random random = new Random(ScreenShakeHandler.getShakeSeed() + (long) (partialTick * 1000));
            float yawOffset = (random.nextFloat() - 0.5f) * intensity * 2.0f;
            float pitchOffset = (random.nextFloat() - 0.5f) * intensity * 1.0f;
            setRotation(this.yRot + yawOffset, this.xRot + pitchOffset);
        }
    }
}
