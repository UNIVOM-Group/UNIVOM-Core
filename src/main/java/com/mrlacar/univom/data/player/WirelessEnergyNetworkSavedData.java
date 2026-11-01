package com.mrlacar.univom.data.player;

import com.mrlacar.univom.network.wireless.WirelessEnergyNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WirelessEnergyNetworkSavedData extends SavedData {
    public static WirelessEnergyNetworkSavedData INSTANCE;

    public static WirelessEnergyNetworkSavedData getOrCreate(ServerLevel serverLevel) {
        return serverLevel.getDataStorage().computeIfAbsent(
                WirelessEnergyNetworkSavedData::new,
                WirelessEnergyNetworkSavedData::new,
                "univom_wireless_energy"
        );
    }

    public final Map<UUID, WirelessEnergyNetwork> networkMap = new HashMap<>();

    public WirelessEnergyNetworkSavedData() {}

    public WirelessEnergyNetworkSavedData(CompoundTag tag) {
        ListTag allEnergy = tag.getList("networks", Tag.TAG_COMPOUND);
        for (int i = 0; i < allEnergy.size(); i++) {
            CompoundTag netTag = allEnergy.getCompound(i);
            UUID uuid = netTag.getUUID("uuid");
            String enStr = netTag.getString("energy");
            BigInteger energy = new BigInteger(enStr.isEmpty() ? "0" : enStr);

            WirelessEnergyNetwork network = new WirelessEnergyNetwork(uuid, energy);

            if (netTag.contains("capacities", Tag.TAG_LIST)) {
                ListTag capList = netTag.getList("capacities", Tag.TAG_COMPOUND);
                for (int j = 0; j < capList.size(); j++) {
                    CompoundTag capTag = capList.getCompound(j);
                    network.getSubstationCapacities().put(
                            capTag.getUUID("subID"),
                            new BigInteger(capTag.getString("cap"))
                    );
                }
                network.recalculateCapacity();
            }
            networkMap.put(uuid, network);
        }
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag) {
        ListTag allEnergy = new ListTag();
        for (WirelessEnergyNetwork network : networkMap.values()) {
            if (network.getStorage().equals(BigInteger.ZERO) && network.getCapacity().equals(BigInteger.ZERO)) {
                continue;
            }

            CompoundTag netTag = new CompoundTag();
            netTag.putUUID("uuid", network.getNetworkID());
            netTag.putString("energy", network.getStorage().toString());

            ListTag capList = new ListTag();
            for (Map.Entry<UUID, BigInteger> entry : network.getSubstationCapacities().entrySet()) {
                CompoundTag capTag = new CompoundTag();
                capTag.putUUID("subID", entry.getKey());
                capTag.putString("cap", entry.getValue().toString());
                capList.add(capTag);
            }
            netTag.put("capacities", capList);

            allEnergy.add(netTag);
        }
        compoundTag.put("networks", allEnergy);
        return compoundTag;
    }
}
