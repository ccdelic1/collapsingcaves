package com.collapsingcaves.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CaveInPayload(BlockPos pos, int tierOrdinal) implements CustomPacketPayload {
    public static final Type<CaveInPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("collapsingcaves", "cave_in")
    );

    public static final StreamCodec<FriendlyByteBuf, CaveInPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CaveInPayload decode(FriendlyByteBuf buf) {
            return new CaveInPayload(buf.readBlockPos(), buf.readVarInt());
        }

        @Override
        public void encode(FriendlyByteBuf buf, CaveInPayload payload) {
            buf.writeBlockPos(payload.pos());
            buf.writeVarInt(payload.tierOrdinal());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
