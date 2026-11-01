package com.mrlacar.univom.api.capability;

import com.mrlacar.univom.api.data.particle.BeamPacket;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface IBeamProvider {
    @NotNull
    BeamPacket requestBeam(long maxFlux, boolean simulate, @NotNull Collection<IBeamProvider> seen);

    long getMaxFlux(@NotNull Collection<IBeamProvider> seen);

    boolean canBridge(@NotNull Collection<IBeamProvider> seen);
}
