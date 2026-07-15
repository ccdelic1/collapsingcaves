package com.collapsingcaves.mixin;

import com.collapsingcaves.sound.CaveInSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockLandMixin extends Entity {

    protected FallingBlockLandMixin() {
        super(null, null);
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"))
    private void collapsingcaves$onLand(double fallDistance, float multiplier, DamageSource damageSource,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (this.getTags().contains("collapsingcaves_cavein") && this.random.nextBoolean()) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    CaveInSounds.BLOCK_LAND.get(), SoundSource.BLOCKS, 0.8f,
                    0.8f + this.level().getRandom().nextFloat() * 0.4f);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void collapsingcaves$onLandParticles(CallbackInfo ci) {
        if (this.onGround() && this.getTags().contains("collapsingcaves_cavein")) {
            if (this.level() instanceof ServerLevel serverLevel && this.level().getRandom().nextInt(9) == 0) {
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, true,
                        this.getX(), this.getY() + 2.0, this.getZ(),
                        2, 0.15, 0.05, 0.15, 0.02);
            }
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean collapsingcaves$transformBlock(Level level, BlockPos pos, BlockState state, int flags) {
        if (this.getTags().contains("collapsingcaves_cavein")) {
            int roll = this.random.nextInt(6);
            if (roll == 0) {
                boolean deepslate = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().contains("deepslate");
                state = deepslate ? Blocks.COBBLED_DEEPSLATE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
            } else if (roll == 1) {
                boolean deepslate = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().contains("deepslate");
                state = deepslate ? Blocks.TUFF.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
            }
        }
        return level.setBlock(pos, state, flags);
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity entity) {
        if (this.getTags().contains("collapsingcaves_cavein")) {
            return true;
        }
        return false;
    }
}
