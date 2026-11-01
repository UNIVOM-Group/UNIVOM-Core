package com.mrlacar.univom.network;

import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.network.packets.SyncUnlockTagPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(UNIVOM.MOD_ID, "main_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        CHANNEL.messageBuilder(SyncUnlockTagPacket.class, id())
                .encoder(SyncUnlockTagPacket::encode)
                .decoder(SyncUnlockTagPacket::new)
                .consumerMainThread(SyncUnlockTagPacket::handle)
                .add();

    }
}
