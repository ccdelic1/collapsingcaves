package com.collapsingcaves.mixin;

import com.collapsingcaves.cavein.CaveInBlockRegistry;
import com.collapsingcaves.tracking.PlacedBlockTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockPlaceMixin {
   // Only blocks a cave-in could actually drop are worth remembering. Recording every
   // placement grew the saved data without bound for no benefit: isPlayerPlaced is only
   // ever consulted for blocks that already passed the affected-block check. The level
   // test stays first so the block registry is never resolved on the client.
   @Inject(method = "placeBlock", at = @At("RETURN"))
   private void collapsingcaves$onPlaceBlock(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
      if ((Boolean)cir.getReturnValue() && context.getLevel() instanceof ServerLevel serverLevel && CaveInBlockRegistry.isAffected(state)) {
         PlacedBlockTracker.get(serverLevel).markPlaced(context.getClickedPos());
      }
   }
}
