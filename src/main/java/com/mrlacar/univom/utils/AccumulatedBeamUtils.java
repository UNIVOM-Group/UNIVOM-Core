package com.mrlacar.univom.utils;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;

@Getter
public class AccumulatedBeamUtils {
    private final String particleName;
    private double currentEnergy;
    private long currentFlux;

    public static final long MAX_FLUX = 2000000000L;

    public AccumulatedBeamUtils(String particleName, double currentEnergy, long currentFlux) {
        this.particleName = particleName;
        this.currentEnergy = currentEnergy;
        this.currentFlux = currentFlux;
    }

    public long merge(double inputEnergy, long inputFlux) {
        long availableSpace = MAX_FLUX - this.currentFlux;
        if (availableSpace <= 0) return 0;

        long toAdd = Math.min(availableSpace, inputFlux);

        double totalEnergy = (this.currentEnergy * this.currentFlux) + (inputEnergy * toAdd);
        this.currentFlux += toAdd;
        this.currentEnergy = totalEnergy / this.currentFlux;

        return toAdd;
    }

    public long calculateCoolingDemand(double restMass) {
        if (this.currentEnergy <= restMass) return 0L;
        double coolingFactor = 0.8;
        double deltaE = this.currentEnergy - restMass;
        return (long) ((deltaE * this.currentFlux * (1.0 - coolingFactor)) / 1000.0);
    }

    public void applyCooling(double restMass) {
        if (this.currentEnergy <= restMass) {
            this.currentEnergy = restMass;
            return;
        }

        double deltaE = this.currentEnergy - restMass;
        deltaE *= 0.5;

        if (deltaE < 0.01) {
            this.currentEnergy = restMass;
        } else {
            this.currentEnergy = restMass + deltaE;
        }
    }

    public long extractFlux(long amount) {
        long extracted = Math.min(this.currentFlux, amount);
        this.currentFlux -= extracted;
        if (this.currentFlux <= 0) {
            this.currentEnergy = 0.0;
        }
        return extracted;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Particle", this.particleName);
        tag.putDouble("Energy", this.currentEnergy);
        tag.putLong("Flux", this.currentFlux);
        return tag;
    }

    public static AccumulatedBeamUtils deserializeNBT(CompoundTag tag) {
        return new AccumulatedBeamUtils(
                tag.getString("Particle"),
                tag.getDouble("Energy"),
                tag.getLong("Flux")
        );
    }
}
