package com.mrlacar.univom.network.wireless;

import com.mrlacar.univom.data.player.WirelessEnergyNetworkSavedData;
import com.mrlacar.univom.utils.WirelessEnergyNetworkUtils;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WirelessEnergyNetwork {

    @Nullable
    public static WirelessEnergyNetwork getOrCreateNetwork(UUID playerUUID) {
        return getOrCreateByNetworkID(WirelessEnergyNetworkUtils.getTeamUUID(playerUUID));
    }

    @Nullable
    public static WirelessEnergyNetwork getOrCreateByNetworkID(@Nullable UUID networkID) {
        if (networkID == null) return null;
        WirelessEnergyNetworkSavedData data = WirelessEnergyNetworkSavedData.INSTANCE;
        if (data == null) return null;
        return data.networkMap.computeIfAbsent(networkID, WirelessEnergyNetwork::new);
    }

    private final UUID networkID;

    private long storageLong;
    @Nullable
    private BigInteger storageBig;

    private long capacityLong;
    @Nullable
    private BigInteger capacityBig;

    private final Map<UUID, BigInteger> substationCapacities = new HashMap<>();

    private long currentAdded;
    private long currentRemoved;
    private long lastSecAdded;
    private long lastSecRemoved;

    public WirelessEnergyNetwork(UUID networkID, BigInteger storage) {
        this.networkID = networkID;
        setStorage(storage);
    }

    private WirelessEnergyNetwork(UUID networkID) {
        this.networkID = networkID;
    }

    public UUID getNetworkID() {
        return networkID;
    }

    public BigInteger getStorage() {
        return storageBig != null ? storageBig : BigInteger.valueOf(storageLong);
    }

    public BigInteger getCapacity() {
        return capacityBig != null ? capacityBig : BigInteger.valueOf(capacityLong);
    }

    public Map<UUID, BigInteger> getSubstationCapacities() {
        return substationCapacities;
    }

    public long getCurrentAdded() {
        return currentAdded;
    }

    public long getCurrentRemoved() {
        return currentRemoved;
    }

    public long getLastSecAdded() {
        return lastSecAdded;
    }

    public long getLastSecRemoved() {
        return lastSecRemoved;
    }

    private void setStorage(BigInteger value) {
        if (value.signum() <= 0) {
            this.storageLong = 0L;
            this.storageBig = null;
        } else if (value.bitLength() <= 63) {
            this.storageLong = value.longValue();
            this.storageBig = null;
        } else {
            this.storageBig = value;
        }
    }

    private void setCapacity(BigInteger value) {
        if (value.signum() <= 0) {
            this.capacityLong = 0L;
            this.capacityBig = null;
        } else if (value.bitLength() <= 63) {
            this.capacityLong = value.longValue();
            this.capacityBig = null;
        } else {
            this.capacityBig = value;
        }
    }

    private static long saturatedAdd(long a, long b) {
        long r = a + b;
        if (((a ^ r) & (b ^ r)) < 0) return Long.MAX_VALUE;
        return r;
    }

    private void markDirty() {
        WirelessEnergyNetworkSavedData data = WirelessEnergyNetworkSavedData.INSTANCE;
        if (data != null) {
            data.setDirty();
        }
    }

    public void updateStats() {
        this.lastSecAdded = this.currentAdded;
        this.lastSecRemoved = this.currentRemoved;
        this.currentAdded = 0L;
        this.currentRemoved = 0L;
    }

    public void setSubstationCapacity(UUID substationID, BigInteger substationCapacity) {
        if (substationCapacity.signum() <= 0) {
            substationCapacities.remove(substationID);
        } else {
            substationCapacities.put(substationID, substationCapacity);
        }
        recalculateCapacity();
        markDirty();
    }

    public void recalculateCapacity() {
        long sum = 0L;
        BigInteger big = null;

        for (BigInteger c : substationCapacities.values()) {
            if (c.signum() <= 0) continue;

            if (big == null && c.bitLength() <= 63) {
                long v = c.longValue();
                if (sum <= Long.MAX_VALUE - v) {
                    sum += v;
                    continue;
                }
            }
            if (big == null) big = BigInteger.valueOf(sum);
            big = big.add(c);
        }

        if (big != null) {
            setCapacity(big);
        } else {
            this.capacityLong = sum;
            this.capacityBig = null;
        }
    }

    public BigInteger addEnergy(BigInteger energy) {
        if (energy.signum() <= 0) return BigInteger.ZERO;

        BigInteger capacity = getCapacity();
        BigInteger storage = getStorage();
        BigInteger actualInsert;

        if (capacity.signum() > 0) {
            BigInteger maxInsert = capacity.subtract(storage);
            if (maxInsert.signum() <= 0) return BigInteger.ZERO;
            actualInsert = energy.min(maxInsert);
        } else {
            actualInsert = energy;
        }

        setStorage(storage.add(actualInsert));
        this.currentAdded = saturatedAdd(this.currentAdded, WirelessEnergyNetworkUtils.getLongValue(actualInsert));
        markDirty();
        return actualInsert;
    }

    public BigInteger removeEnergy(BigInteger maxExtract) {
        if (maxExtract.signum() <= 0) return BigInteger.ZERO;

        BigInteger storage = getStorage();
        BigInteger extracted = storage.min(maxExtract);
        if (extracted.signum() <= 0) return BigInteger.ZERO;

        setStorage(storage.subtract(extracted));
        this.currentRemoved = saturatedAdd(this.currentRemoved, WirelessEnergyNetworkUtils.getLongValue(extracted));
        markDirty();
        return extracted;
    }

    public long addEnergy(long energy) {
        if (energy <= 0) return 0L;

        if (storageBig == null && capacityBig == null) {
            long insert;
            if (capacityLong > 0L) {
                long space = capacityLong - storageLong;
                if (space <= 0L) return 0L;
                insert = Math.min(energy, space);
            } else {
                if (energy > Long.MAX_VALUE - storageLong) {
                    this.storageBig = BigInteger.valueOf(storageLong);
                    return WirelessEnergyNetworkUtils.getLongValue(addEnergy(BigInteger.valueOf(energy)));
                }
                insert = energy;
            }

            this.storageLong += insert;
            this.currentAdded = saturatedAdd(this.currentAdded, insert);
            markDirty();
            return insert;
        }

        return WirelessEnergyNetworkUtils.getLongValue(addEnergy(BigInteger.valueOf(energy)));
    }

    public long removeEnergy(long maxExtract) {
        if (maxExtract <= 0) return 0L;

        if (storageBig == null) {
            if (storageLong <= 0L) return 0L;
            long extracted = Math.min(storageLong, maxExtract);
            this.storageLong -= extracted;
            this.currentRemoved = saturatedAdd(this.currentRemoved, extracted);
            markDirty();
            return extracted;
        }

        return WirelessEnergyNetworkUtils.getLongValue(removeEnergy(BigInteger.valueOf(maxExtract)));
    }
}
