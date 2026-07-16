package com.collapsingcaves.network;

import com.collapsingcaves.client.ClientPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CaveInPayload(BlockPos pos, int tierOrdinal) {

    public static void encode(CaveInPayload payload, FriendlyByteBuf buf) {
        buf.writeBlockPos(payload.pos());
        buf.writeVarInt(payload.tierOrdinal());
    }

    public static CaveInPayload decode(FriendlyByteBuf buf) {
        return new CaveInPayload(buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(CaveInPayload payload, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> ClientPacketHandler.handleCaveInStart(payload));
        ctx.setPacketHandled(true);
    }
}
