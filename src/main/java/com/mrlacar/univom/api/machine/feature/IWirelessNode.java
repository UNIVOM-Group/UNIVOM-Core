package com.mrlacar.univom.api.machine.feature;

import com.mrlacar.univom.data.player.WirelessEnergyNetworkSavedData;
import com.mrlacar.univom.network.wireless.WirelessEnergyNetwork;
import com.mrlacar.univom.utils.WirelessEnergyNetworkUtils;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface IWirelessNode {
    @Nullable
    UUID getBoundPlayerUUID();

    void setWirelessNetworkCache(WirelessEnergyNetwork network);

    WirelessEnergyNetwork getWirelessNetworkCache();

    @Nullable
    default WirelessEnergyNetwork getWirelessNetwork() {
        if (WirelessEnergyNetworkSavedData.INSTANCE == null) return null;

        UUID boundUUID = getBoundPlayerUUID();
        if (boundUUID == null) return getWirelessNetworkCache();

        UUID currentTeamID = WirelessEnergyNetworkUtils.getTeamUUID(boundUUID);
        if (currentTeamID == null) return getWirelessNetworkCache();

        WirelessEnergyNetwork cache = getWirelessNetworkCache();
        if (cache != null && cache.getNetworkID().equals(currentTeamID)) {
            return cache;
        }

        WirelessEnergyNetwork network = WirelessEnergyNetwork.getOrCreateByNetworkID(currentTeamID);
        setWirelessNetworkCache(network);
        return network;
    }
}
