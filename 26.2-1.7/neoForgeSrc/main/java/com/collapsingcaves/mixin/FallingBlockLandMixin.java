package com.collapsingcaves.mixin;

import com.collapsingcaves.cavein.CaveInFallingBlock;
import com.collapsingcaves.config.CollapsingCavesConfig;
import com.collapsingcaves.sound.CaveInSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockLandMixin extends Entity implements CaveInFallingBlock {
   @Unique
   private static final EntityDataAccessor<Float> COLLAPSINGCAVES$GRAVITY = SynchedEntityData.defineId(FallingBlockEntity.class, EntityDataSerializers.FLOAT);

   protected FallingBlockLandMixin() {
      super(null, null);
   }

   @Inject(method = "defineSynchedData", at = @At("RETURN"))
   private void collapsingcaves$defineGravity(Builder builder, CallbackInfo ci) {
      builder.define(COLLAPSINGCAVES$GRAVITY, 0.0F);
   }

   @Override
   public void setCaveInGravity(float gravity) {
      this.getEntityData().set(COLLAPSINGCAVES$GRAVITY, gravity);
   }

   @Unique
   private float collapsingcaves$caveInGravity() {
      return (Float)this.getEntityData().get(COLLAPSINGCAVES$GRAVITY);
   }

   @Unique
   private boolean collapsingcaves$isCaveInBlock() {
      return this.collapsingcaves$caveInGravity() > 0.0F;
   }

   @Inject(method = "tick", at = @At("HEAD"))
   private void collapsingcaves$applySlowGravity(CallbackInfo ci) {
      float gravity = this.collapsingcaves$caveInGravity();
      if (!(gravity <= 0.0F)) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, -gravity, 0.0));
         this.needsSync = true;
      }
   }

   @Inject(method = "causeFallDamage", at = @At("HEAD"))
   private void collapsingcaves$onLand(double fallDistance, float multiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
      if (this.collapsingcaves$isCaveInBlock() && CollapsingCavesConfig.get().soundsEnabled) {
         this.level()
            .playSound(
               null,
               this.getX(),
               this.getY(),
               this.getZ(),
               CaveInSounds.blockLand(),
               SoundSource.BLOCKS,
               0.8F,
               0.8F + this.level().getRandom().nextFloat() * 0.4F
            );
      }
   }

   @Inject(method = "tick", at = @At("TAIL"))
   private void collapsingcaves$onLandParticles(CallbackInfo ci) {
      if (this.onGround() && this.collapsingcaves$isCaveInBlock()) {
         if (CollapsingCavesConfig.get().dustParticlesEnabled) {
            if (this.level() instanceof ServerLevel serverLevel && this.random.nextInt(4) == 0) {
               BlockState state = ((FallingBlockEntity)(Object)this).getBlockState();
               serverLevel.sendParticles(
                  new BlockParticleOption(ParticleTypes.BLOCK, state), true, true, this.getX(), this.getY() + 0.2, this.getZ(), 8, 0.3, 0.1, 0.3, 0.05
               );
            }
         }
      }
   }

   public boolean canBeCollidedWith(@Nullable Entity entity) {
      return this.collapsingcaves$isCaveInBlock();
   }
}
