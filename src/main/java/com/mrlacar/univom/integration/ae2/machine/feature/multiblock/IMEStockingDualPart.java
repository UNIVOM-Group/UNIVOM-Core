package com.mrlacar.univom.integration.ae2.machine.feature.multiblock;

import com.gregtechceu.gtceu.integration.ae2.machine.feature.multiblock.IMEStockingPart;

public interface IMEStockingDualPart extends IMEStockingPart {
    int getFluidMinStackSize();

    void setFluidMinStackSize(int newSize);
}
