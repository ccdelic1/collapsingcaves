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
    private void collapsingcaves$onLand(float fallDistance, float multiplier, DamageSource damageSource,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (this.getTags().contains("collapsingcaves_cavein") && this.random.nextBoolean()) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    CaveInSounds.BLOCK_LAND, SoundSource.BLOCKS, 0.8f,
                    0.8f + this.level().getRandom().nextFloat() * 0.4f);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void collapsingcaves$onLandParticles(CallbackInfo ci) {
        if (this.onGround() && this.getTags().contains("collapsingcaves_cavein")) {
            if (this.level() instanceof ServerLevel serverLevel && this.level().getRandom().nextInt(9) == 0) {
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
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
                state = collapsingcaves$rubbleVariant(state, true);
            } else if (roll == 1) {
                state = collapsingcaves$rubbleVariant(state, false);
            }
        }
        return level.setBlock(pos, state, flags);
    }

    /**
     * Picks a rubble block appropriate to the falling block's material family instead
     * of always degrading to Overworld cobblestone/gravel - e.g. Nether stone lands as
     * blackstone/netherrack rubble, and sandstone as sand, rather than out-of-place
     * cobblestone.
     */
    private static BlockState collapsingcaves$rubbleVariant(BlockState original, boolean coarse) {
        String path = BuiltInRegistries.BLOCK.getKey(original.getBlock()).getPath();
        if (path.contains("deepslate") || path.contains("tuff") || path.contains("calcite") || path.contains("dripstone")) {
            return coarse ? Blocks.COBBLED_DEEPSLATE.defaultBlockState() : Blocks.TUFF.defaultBlockState();
        }
        if (path.contains("blackstone") || path.contains("basalt") || path.equals("netherrack")) {
            return coarse ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState();
        }
        if (path.contains("sandstone")) {
            return path.contains("red") ? Blocks.RED_SAND.defaultBlockState() : Blocks.SAND.defaultBlockState();
        }
        return coarse ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
    }

    @Override
    public boolean canBeCollidedWith() {
        if (this.getTags().contains("collapsingcaves_cavein")) {
            return true;
        }
        return false;
    }
}
