package com.collapsingcaves.network;

import com.collapsingcaves.client.CaveInClientNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CaveInStopPayload(BlockPos pos) {

    public static void encode(CaveInStopPayload payload, FriendlyByteBuf buf) {
        buf.writeBlockPos(payload.pos());
    }

    public static CaveInStopPayload decode(FriendlyByteBuf buf) {
        return new CaveInStopPayload(buf.readBlockPos());
    }

    public static void handle(CaveInStopPayload payload, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> CaveInClientNetworking.handleCaveInStop(payload));
        ctx.setPacketHandled(true);
    }
}
