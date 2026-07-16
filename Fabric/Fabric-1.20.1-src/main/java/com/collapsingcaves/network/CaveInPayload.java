package com.collapsingcaves.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record CaveInPayload(BlockPos pos, int tierOrdinal) {
    public static final ResourceLocation CHANNEL = new ResourceLocation("collapsingcaves", "cave_in");

    public static CaveInPayload decode(FriendlyByteBuf buf) {
        return new CaveInPayload(buf.readBlockPos(), buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(tierOrdinal);
    }
}
