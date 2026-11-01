package com.mrlacar.univom.common.machine.multiblock.electric.LHCModule;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.mrlacar.univom.api.machine.multiblock.LargeHadronColliderModuleType;
import com.mrlacar.univom.common.machine.multiblock.electric.LargeHadronColliderModule;
import org.jetbrains.annotations.NotNull;

public class LHCBModule extends LargeHadronColliderModule {
    public LHCBModule(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public @NotNull LargeHadronColliderModuleType getModuleType() {
        return LargeHadronColliderModuleType.LHCB;
    }
}
