package com.mrlacar.univom.common.data.machines;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.mrlacar.univom.common.machine.multiblock.part.CreativeFluidHatchPartMachine;
import com.mrlacar.univom.common.machine.multiblock.part.CreativeItemBusPartMachine;
import net.minecraft.network.chat.Component;

import static com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties.IS_FORMED;
import static com.gregtechceu.gtceu.common.data.GTMachines.CREATIVE_TOOLTIPS;
import static com.gregtechceu.gtceu.common.data.models.GTMachineModels.OVERLAY_FLUID_HATCH_INPUT;
import static com.gregtechceu.gtceu.common.data.models.GTMachineModels.OVERLAY_ITEM_HATCH_INPUT;
import static com.mrlacar.univom.api.registry.UNIVOMRegistration.REGISTRATE;

public class ModCreativePartMachines {
    public static final MachineDefinition CREATIVE_ITEM_IMPORT_BUS = REGISTRATE
            .machine("creative_input_bus", CreativeItemBusPartMachine::new)
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.IMPORT_ITEMS)
            .modelProperty(IS_FORMED, false)
            .colorOverlayTieredHullModel(OVERLAY_ITEM_HATCH_INPUT, "overlay_pipe", "overlay_creative_pipe_in_emissive")
            .tooltips(Component.translatable("gtceu.machine.item_bus.import.tooltip"))
            .tooltipBuilder(CREATIVE_TOOLTIPS)
            .register();

    public static final MachineDefinition CREATIVE_FLUID_INPUT_HATCH = REGISTRATE
            .machine("creative_input_hatch", CreativeFluidHatchPartMachine::new)
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.IMPORT_FLUIDS, PartAbility.IMPORT_FLUIDS_4X, PartAbility.IMPORT_FLUIDS_9X)
            .modelProperty(IS_FORMED, false)
            .colorOverlayTieredHullModel(OVERLAY_FLUID_HATCH_INPUT, "overlay_pipe_9x", "overlay_creative_pipe_in_emissive")
            .tooltips(Component.translatable("gtceu.machine.fluid_hatch.import.tooltip"))
            .tooltipBuilder(CREATIVE_TOOLTIPS)
            .register();

    public static void init() {}
}
