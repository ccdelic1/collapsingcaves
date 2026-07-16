package com.collapsingcaves.mixin;

import com.collapsingcaves.cavein.CaveInManager;
import com.collapsingcaves.tracking.PlacedBlockTracker;
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

    // destroyBlock can bail out early (cancelled break event, GameMasterBlock restriction,
    // blockActionRestricted) without actually removing the block, so the pre-break state is
    // captured here but only acted on in the RETURN injection once we know the break succeeded.
    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void collapsingcaves$captureState(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ServerLevel level = (ServerLevel) player.level();
        collapsingcaves$pendingPos = pos;
        collapsingcaves$pendingState = level.getBlockState(pos);
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void collapsingcaves$onBlockBroken(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        if (!pos.equals(collapsingcaves$pendingPos)) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        BlockState brokenState = collapsingcaves$pendingState;

        PlacedBlockTracker tracker = PlacedBlockTracker.get(level);
        if (tracker.isPlayerPlaced(pos)) {
            tracker.removePlaced(pos);
            return;
        }

        CaveInManager.get(level).onBlockBroken(player, pos, brokenState);
    }
}
