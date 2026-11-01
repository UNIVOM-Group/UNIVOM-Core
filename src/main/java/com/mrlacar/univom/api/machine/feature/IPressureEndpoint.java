package com.mrlacar.univom.api.machine.feature;

import net.minecraft.core.Direction;

public interface IPressureEndpoint {
    boolean canConnectPressure(Direction side);
}
