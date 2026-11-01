package com.mrlacar.univom.api.recipe.content;

import com.gregtechceu.gtceu.api.recipe.content.IContentSerializer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mrlacar.univom.api.data.particle.BeamPacket;
import net.minecraft.network.FriendlyByteBuf;

public class SerializerBeamPacket implements IContentSerializer<BeamPacket> {
    public static final SerializerBeamPacket INSTANCE = new SerializerBeamPacket();

    public static final Codec<BeamPacket> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("particleName").forGetter(BeamPacket::particle),
            Codec.DOUBLE.fieldOf("energy").forGetter(BeamPacket::energy),
            Codec.LONG.fieldOf("flux").forGetter(BeamPacket::flux)
    ).apply(instance, BeamPacket::new));

    private SerializerBeamPacket() {}

    @Override
    public BeamPacket of(Object o) {
        if (o instanceof BeamPacket packet) return packet;
        return defaultValue();
    }

    @Override
    public BeamPacket defaultValue() {
        return BeamPacket.EMPTY;
    }

    @Override
    public Class<BeamPacket> contentClass() {
        return BeamPacket.class;
    }

    @Override
    public Codec<BeamPacket> codec() {
        return CODEC;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, BeamPacket content) {
        buf.writeUtf(content.particle());
        buf.writeDouble(content.energy());
        buf.writeLong(content.flux());
    }

    @Override
    public BeamPacket fromNetwork(FriendlyByteBuf buf) {
        return new BeamPacket(buf.readUtf(), buf.readDouble(), buf.readLong());
    }
}
