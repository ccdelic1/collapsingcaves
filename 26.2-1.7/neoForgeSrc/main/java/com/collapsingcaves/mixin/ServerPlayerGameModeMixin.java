package com.collapsingcaves.mixin;

import com.collapsingcaves.cavein.CaveInManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
   @Shadow
   protected ServerPlayer player;
   @Unique
   private BlockPos collapsingcaves$pendingPos;
   @Unique
   private BlockState collapsingcaves$pendingState;

   @Inject(method = "destroyBlock", at = @At("HEAD"))
   private void collapsingcaves$captureState(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
      this.collapsingcaves$pendingPos = pos;
      this.collapsingcaves$pendingState = this.player.level().getBlockState(pos);
   }

   @Inject(method = "destroyBlock", at = @At("RETURN"))
   private void collapsingcaves$onBlockBroken(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
      if (Boolean.TRUE.equals(cir.getReturnValue()) && pos.equals(this.collapsingcaves$pendingPos)) {
         if (this.player.level() instanceof ServerLevel serverLevel) {
            CaveInManager.get(serverLevel).onBlockBroken(this.player, pos, this.collapsingcaves$pendingState);
         }
      }
   }
}
