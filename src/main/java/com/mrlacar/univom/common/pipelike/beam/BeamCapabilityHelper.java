package com.mrlacar.univom.common.pipelike.beam;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.mrlacar.univom.api.capability.IBeamProvider;
import com.mrlacar.univom.api.capability.forge.ModCapability;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class BeamCapabilityHelper {
    @Nullable
    public static IBeamProvider getBeamProvider(@Nullable BlockEntity be, @Nullable Direction side) {
        if (be == null) return null;

        IBeamProvider provider = be.getCapability(ModCapability.BEAM_PROVIDER, side).orElse(null);
        if (provider != null) return provider;

        if (be instanceof IMachineBlockEntity machineBE) {
            MetaMachine machine = machineBE.getMetaMachine();

            if (side != null && machine.getFrontFacing() != side) {
                return null;
            }

            if (machine instanceof IBeamProvider p) return p;

            for (var trait : machine.getTraits()) {
                if (trait instanceof IBeamProvider p) {
                    return p;
                }
            }
        }

        return null;
    }
}
