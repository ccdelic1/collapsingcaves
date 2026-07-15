package com.collapsingcaves.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CaveInStopPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<CaveInStopPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("collapsingcaves", "cave_in_stop")
    );

    public static final StreamCodec<FriendlyByteBuf, CaveInStopPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CaveInStopPayload decode(FriendlyByteBuf buf) {
            return new CaveInStopPayload(buf.readBlockPos());
        }

        @Override
        public void encode(FriendlyByteBuf buf, CaveInStopPayload payload) {
            buf.writeBlockPos(payload.pos());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
