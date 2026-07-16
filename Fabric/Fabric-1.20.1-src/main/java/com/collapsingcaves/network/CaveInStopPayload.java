package com.collapsingcaves.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record CaveInStopPayload(BlockPos pos) {
    public static final ResourceLocation CHANNEL = new ResourceLocation("collapsingcaves", "cave_in_stop");

    public static CaveInStopPayload decode(FriendlyByteBuf buf) {
        return new CaveInStopPayload(buf.readBlockPos());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }
}
