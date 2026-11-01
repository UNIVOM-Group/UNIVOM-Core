package com.mrlacar.univom.common.data;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.mrlacar.univom.common.data.machines.ModAEMachines;
import com.mrlacar.univom.common.data.machines.ModCreativePartMachines;
import com.mrlacar.univom.common.data.machines.ModMultiMachines;
import com.mrlacar.univom.common.data.machines.ModPartMachines;
import com.mrlacar.univom.common.machine.electric.GasCompressor;
import com.mrlacar.univom.common.machine.electric.OxygenDistributor;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.mrlacar.univom.api.registry.UNIVOMRegistration.REGISTRATE;
import static com.mrlacar.univom.utils.RegisterUtils.registerSimpleMachines;
import static com.mrlacar.univom.utils.RegisterUtils.registerTieredMachines;

public class ModMachines {
    static {
        REGISTRATE.creativeModeTab(() -> ModCreativeModeTabs.MACHINE);
    }

    public static final MachineDefinition[] SUPERCONDUCTOR_ASSEMBLER = registerSimpleMachines("superconductor_assembler",
            ModRecipeTypes.SUPERCONDUCTOR_ASSEMBLER_RECIPES);

    public static final MachineDefinition[] OXYGEN_DISTRIBUTOR = registerSimpleMachines(REGISTRATE, "oxygen_distributor",
            ModRecipeTypes.OXYGEN_DISTRIBUTOR_RECIPES, OxygenDistributor.TANK_CAPACITY, false,
            OxygenDistributor::new, OxygenDistributor::recipeModifier,
            OxygenDistributor::tooltips, HV, EV);

    public static final MachineDefinition[] GAS_COMPRESSOR = registerTieredMachines(REGISTRATE, "gas_compressor",
            GasCompressor::new,
            (tier, builder) -> builder
                    .langValue("%s Gas Compressor %s".formatted(VLVH[tier], VLVT[tier]))
                    .rotationState(RotationState.ALL)
                    .modelProperty(GTMachineModelProperties.RECIPE_LOGIC_STATUS, RecipeLogic.Status.IDLE)
                    .tooltips(GasCompressor.tooltips(tier))
                    .register(),
            LV, MV, HV, EV, IV);

    public static void init() {
        ModCreativePartMachines.init();
        ModPartMachines.init();
        ModAEMachines.init();
        ModMultiMachines.init();
    }
}
