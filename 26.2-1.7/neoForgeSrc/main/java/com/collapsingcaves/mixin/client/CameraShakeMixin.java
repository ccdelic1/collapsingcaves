package com.collapsingcaves.mixin.client;

import com.collapsingcaves.client.ScreenShakeHandler;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraShakeMixin {
   @Shadow
   private float yRot;
   @Shadow
   private float xRot;
   // setRotation works in degrees. The configured strength is a fraction from 0 to 1, so
   // it needs a span to be a fraction of: a strength of 1.0 swings the view this far to
   // each side. Pitch takes half of it, because vertical movement is harder to read.
   @Unique
   private static final float COLLAPSINGCAVES$MAX_SHAKE_DEGREES = 2.0F;
   @Unique
   private final Random collapsingcaves$shakeRandom = new Random();

   @Shadow
   protected abstract void setRotation(float var1, float var2);

   @Inject(method = "update", at = @At("RETURN"))
   private void collapsingcaves$applyScreenShake(DeltaTracker deltaTracker, CallbackInfo ci) {
      float intensity = ScreenShakeHandler.getShakeIntensity();
      if (intensity > 0.01F) {
         float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
         this.collapsingcaves$shakeRandom.setSeed(ScreenShakeHandler.getShakeSeed() + (long)(partialTick * 1000.0F));
         float swing = intensity * COLLAPSINGCAVES$MAX_SHAKE_DEGREES;
         float yawOffset = (this.collapsingcaves$shakeRandom.nextFloat() - 0.5F) * swing * 2.0F;
         float pitchOffset = (this.collapsingcaves$shakeRandom.nextFloat() - 0.5F) * swing;
         this.setRotation(this.yRot + yawOffset, this.xRot + pitchOffset);
      }
   }
}
