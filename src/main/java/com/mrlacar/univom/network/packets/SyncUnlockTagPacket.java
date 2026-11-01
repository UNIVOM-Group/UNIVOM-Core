package com.mrlacar.univom.network.packets;

import com.mrlacar.univom.client.forge.ClientTagCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncUnlockTagPacket {
    private final String tag;

    public SyncUnlockTagPacket(String tag) {
        this.tag = tag;
    }

    public SyncUnlockTagPacket(FriendlyByteBuf buf) {
        this.tag = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.tag);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientTagCache.addTag(this.tag));
        ctx.get().setPacketHandled(true);
    }
}
