package com.collapsingcaves.mixin;

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
    @Inject(method = "placeBlock", at = @At("RETURN"))
    private void collapsingcaves$onPlaceBlock(BlockPlaceContext context, BlockState state,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && context.getLevel() instanceof ServerLevel serverLevel) {
            PlacedBlockTracker.get(serverLevel).markPlaced(context.getClickedPos());
        }
    }
}
