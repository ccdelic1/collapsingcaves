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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
    @Shadow
    protected ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void collapsingcaves$onBlockBroken(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ServerLevel level = (ServerLevel) player.level();
        BlockState brokenState = level.getBlockState(pos);

        PlacedBlockTracker tracker = PlacedBlockTracker.get(level);
        if (tracker.isPlayerPlaced(pos)) {
            tracker.removePlaced(pos);
            return;
        }

        CaveInManager.get(level).onBlockBroken(player, pos, brokenState);
    }
}
