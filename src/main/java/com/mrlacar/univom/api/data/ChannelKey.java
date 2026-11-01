package com.mrlacar.univom.api.data;

import com.mrlacar.univom.utils.WirelessEnergyNetworkUtils;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record ChannelKey(UUID namespace, String name) {

    public static final UUID LEGACY_NAMESPACE = new UUID(0L, 0L);

    public static ChannelKey of(@Nullable UUID ownerUUID, String name) {
        return new ChannelKey(resolveNamespace(ownerUUID), name);
    }

    public static UUID resolveNamespace(@Nullable UUID ownerUUID) {
        if (ownerUUID == null) return LEGACY_NAMESPACE;
        UUID team = WirelessEnergyNetworkUtils.getTeamUUID(ownerUUID);
        return team == null ? LEGACY_NAMESPACE : team;
    }

    public boolean isEmpty() {
        return name.isEmpty();
    }

    public void write(CompoundTag tag) {
        tag.putUUID("Namespace", namespace);
        tag.putString("Name", name);
    }

    public static ChannelKey read(CompoundTag tag) {
        UUID namespace = tag.hasUUID("Namespace") ? tag.getUUID("Namespace") : LEGACY_NAMESPACE;
        return new ChannelKey(namespace, tag.getString("Name"));
    }
}
