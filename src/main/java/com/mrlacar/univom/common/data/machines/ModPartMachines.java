package com.mrlacar.univom.common.data.machines;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.api.machine.multiblock.ModPartAbility;
import com.mrlacar.univom.common.machine.multiblock.part.*;
import net.minecraft.network.chat.Component;

import static com.gregtechceu.gtceu.api.GTValues.HV;
import static com.gregtechceu.gtceu.api.GTValues.UV;
import static com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties.IS_FORMED;
import static com.gregtechceu.gtceu.common.data.models.GTMachineModels.OVERLAY_ITEM_HATCH_INPUT;
import static com.gregtechceu.gtceu.common.data.models.GTMachineModels.OVERLAY_ITEM_HATCH_OUTPUT;
import static com.mrlacar.univom.api.registry.UNIVOMRegistration.REGISTRATE;
import static com.mrlacar.univom.utils.MachineUtils.WIRELESS_ENERGY_HATCH_TIERS;
import static com.mrlacar.univom.utils.RegisterUtils.registerWirelessEnergyHatch;

public class ModPartMachines {
    public static final MachineDefinition SLAUGHTERHOUSE_OUTPUT_BUS = REGISTRATE
            .machine("slaughterhouse_output_bus", SlaughterhouseBusPartMachine::new)
            .rotationState(RotationState.ALL)
            .abilities(ModPartAbility.SLAUGHTERHOUSE_EXPORT_ITEMS)
            .modelProperty(IS_FORMED, false)
            .colorOverlayTieredHullModel(OVERLAY_ITEM_HATCH_OUTPUT, "overlay_pipe", "overlay_pipe_out_emissive")
            .allowCoverOnFront(true)
            .register();

    public static final MachineDefinition PROBE_LOAD_BUS = REGISTRATE
            .machine("probe_load_bus", ProbeLoadBusPartMachine::new)
            .rotationState(RotationState.ALL)
            .abilities(ModPartAbility.PROBE_RAILGUN_IMPORT_ITEMS)
            .modelProperty(IS_FORMED, false)
            .colorOverlayTieredHullModel(OVERLAY_ITEM_HATCH_INPUT, "overlay_pipe", "overlay_probe_load_bus_emissive")
            .allowCoverOnFront(true)
            .register();

    public static final MachineDefinition FUEL_ROD_INPUT_BUS = REGISTRATE
            .machine("fuel_rod_input_bus", FuelRodBusPartMachine::new)
            .rotationState(RotationState.ALL)
            .abilities(ModPartAbility.FUEL_ROD_IMPORT)
            .modelProperty(IS_FORMED, false)
            .colorOverlayTieredHullModel(OVERLAY_ITEM_HATCH_INPUT, "overlay_pipe", "overlay_pipe_in_emissive")
            .allowCoverOnFront(true)
            .register();

    public static final MachineDefinition CONTROL_ROD_INSERT_BUS = REGISTRATE
            .machine("control_rod_insert_bus", ControlRodBusPartMachine::new)
            .rotationState(RotationState.ALL)
            .abilities(ModPartAbility.CONTROL_ROD_IMPORT)
            .modelProperty(IS_FORMED, false)
            .colorOverlayTieredHullModel(OVERLAY_ITEM_HATCH_INPUT, "overlay_pipe", "overlay_control_rod_insert_bus_emissive")
            .allowCoverOnFront(true)
            .register();

    public static final MachineDefinition FISSION_REACTOR_SENSOR_HATCH = REGISTRATE
            .machine("fission_reactor_sensor_hatch", FissionReactorSensorPartMachine::new)
            .rotationState(RotationState.ALL)
            .abilities(ModPartAbility.FISSION_REACTOR_SENSOR)
            .modelProperty(IS_FORMED, false)
            .tooltips(Component.translatable("gtceu.part_sharing.disabled"))
            .overlayTieredHullModel(UNIVOM.id("block/machine/part/redstone_signal_hatch"))
            .register();

    public static final MachineDefinition WIRELESS_COMPUTATION_HATCH_TRANSMITTER = REGISTRATE
            .machine("wireless_computation_transmitter_hatch", (holder) -> new WirelessOpticalComputationHatchMachine(holder, true))
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.COMPUTATION_DATA_TRANSMISSION)
            .tooltips(Component.translatable("gtceu.machine.computation_transmitter_hatch.tooltip"),
                    Component.translatable("gtceu.part_sharing.disabled"))
            .overlayTieredHullModel("computation_data_hatch")
            .tier(UV)
            .register();

    public static final MachineDefinition WIRELESS_COMPUTATION_HATCH_RECEIVER = REGISTRATE
            .machine("wireless_computation_receiver_hatch", (holder) -> new WirelessOpticalComputationHatchMachine(holder, false))
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.COMPUTATION_DATA_RECEPTION)
            .tooltips(Component.translatable("gtceu.machine.computation_receiver_hatch.tooltip"),
                    Component.translatable("gtceu.part_sharing.disabled"))
            .overlayTieredHullModel("computation_data_hatch")
            .tier(UV)
            .register();

    public static final MachineDefinition WIRELESS_DATA_HATCH_TRANSMITTER = REGISTRATE
            .machine("wireless_data_transmitter_hatch", (holder) -> new WirelessOpticalDataHatchMachine(holder, true))
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.OPTICAL_DATA_TRANSMISSION)
            .overlayTieredHullModel("optical_data_hatch")
            .tooltips(Component.translatable("gtceu.machine.data_transmitter_hatch.tooltip"),
                    Component.translatable("gtceu.part_sharing.disabled"))
            .tier(UV)
            .register();

    public static final MachineDefinition WIRELESS_DATA_HATCH_RECEIVER = REGISTRATE
            .machine("wireless_data_receiver_hatch", (holder) -> new WirelessOpticalDataHatchMachine(holder, false))
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.OPTICAL_DATA_RECEPTION)
            .overlayTieredHullModel("optical_data_hatch")
            .tooltips(Component.translatable("gtceu.machine.data_receiver_hatch.tooltip"),
                    Component.translatable("gtceu.part_sharing.disabled"))
            .tier(UV)
            .register();

    public static final MachineDefinition BEAM_HATCH_TRANSMITTER = REGISTRATE
            .machine("beam_transmitter_hatch", (holder) -> new BeamHatchPartMachine(holder, true))
            .rotationState(RotationState.ALL)
            .abilities(ModPartAbility.BEAM_TRANSMISSION)
            .overlayTieredHullModel("beam_hatch")
            .tooltips(Component.translatable("gtceu.part_sharing.disabled"))
            .tier(UV)
            .register();

    public static final MachineDefinition BEAM_HATCH_RECEIVER = REGISTRATE
            .machine("beam_receiver_hatch", (holder) -> new BeamHatchPartMachine(holder, false))
            .rotationState(RotationState.ALL)
            .abilities(ModPartAbility.BEAM_RECEPTION)
            .overlayTieredHullModel("beam_hatch")
            .tooltips(Component.translatable("gtceu.part_sharing.disabled"))
            .tier(UV)
            .register();

    public static final MachineDefinition FILTERED_BEAM_HATCH_TRANSMITTER = REGISTRATE
            .machine("filtered_beam_transmitter_hatch", FilteredBeamHatchPartMachine::new)
            .rotationState(RotationState.ALL)
            .abilities(ModPartAbility.FILTERED_BEAM_TRANSMISSION)
            .overlayTieredHullModel("beam_hatch")
            .tooltips(Component.translatable("gtceu.part_sharing.disabled"))
            .tier(UV)
            .register();

    public static final MachineDefinition PRESSURE_PORT = REGISTRATE
            .machine("pressure_port", PressurePortPartMachine::new)
            .rotationState(RotationState.ALL)
            .abilities(ModPartAbility.PRESSURE_PORT)
            .overlayTieredHullModel("pressure_port")
            .tooltips(Component.translatable("univom.machine.pressure_port.tooltip.0"),
                    Component.translatable("univom.machine.pressure_port.tooltip.1"),
                    Component.translatable("gtceu.part_sharing.disabled"))
            .tier(HV)
            .register();

    public static final MachineDefinition[] WIRELESS_ENERGY_INPUT_HATCH = registerWirelessEnergyHatch(IO.IN, 2, PartAbility.INPUT_ENERGY, WIRELESS_ENERGY_HATCH_TIERS);
    public static final MachineDefinition[] WIRELESS_ENERGY_INPUT_HATCH_4A = registerWirelessEnergyHatch(IO.IN, 4, PartAbility.INPUT_ENERGY, WIRELESS_ENERGY_HATCH_TIERS);
    public static final MachineDefinition[] WIRELESS_ENERGY_INPUT_HATCH_16A = registerWirelessEnergyHatch(IO.IN, 16, PartAbility.INPUT_ENERGY, WIRELESS_ENERGY_HATCH_TIERS);
    public static final MachineDefinition[] WIRELESS_ENERGY_INPUT_HATCH_64A = registerWirelessEnergyHatch(IO.IN, 64, PartAbility.INPUT_ENERGY, WIRELESS_ENERGY_HATCH_TIERS);
    public static final MachineDefinition[] WIRELESS_ENERGY_INPUT_HATCH_256A = registerWirelessEnergyHatch(IO.IN, 256, PartAbility.INPUT_ENERGY, WIRELESS_ENERGY_HATCH_TIERS);
    public static final MachineDefinition[] WIRELESS_ENERGY_INPUT_HATCH_1024A = registerWirelessEnergyHatch(IO.IN, 1024, PartAbility.INPUT_ENERGY, WIRELESS_ENERGY_HATCH_TIERS);
    public static final MachineDefinition[] WIRELESS_ENERGY_INPUT_HATCH_4096A = registerWirelessEnergyHatch(IO.IN, 4096, PartAbility.INPUT_ENERGY, WIRELESS_ENERGY_HATCH_TIERS);

    public static final MachineDefinition[] WIRELESS_ENERGY_OUTPUT_HATCH = registerWirelessEnergyHatch(IO.OUT, 2, PartAbility.OUTPUT_ENERGY, WIRELESS_ENERGY_HATCH_TIERS);
    public static final MachineDefinition[] WIRELESS_ENERGY_OUTPUT_HATCH_4A = registerWirelessEnergyHatch(IO.OUT, 4, PartAbility.OUTPUT_ENERGY, WIRELESS_ENERGY_HATCH_TIERS);
    public static final MachineDefinition[] WIRELESS_ENERGY_OUTPUT_HATCH_16A = registerWirelessEnergyHatch(IO.OUT, 16, PartAbility.OUTPUT_ENERGY, WIRELESS_ENERGY_HATCH_TIERS);
    public static final MachineDefinition[] WIRELESS_ENERGY_OUTPUT_HATCH_64A = registerWirelessEnergyHatch(IO.OUT, 64, PartAbility.OUTPUT_ENERGY, WIRELESS_ENERGY_HATCH_TIERS);
    public static final MachineDefinition[] WIRELESS_ENERGY_OUTPUT_HATCH_256A = registerWirelessEnergyHatch(IO.OUT, 256, PartAbility.OUTPUT_ENERGY, WIRELESS_ENERGY_HATCH_TIERS);
    public static final MachineDefinition[] WIRELESS_ENERGY_OUTPUT_HATCH_1024A = registerWirelessEnergyHatch(IO.OUT, 1024, PartAbility.OUTPUT_ENERGY, WIRELESS_ENERGY_HATCH_TIERS);
    public static final MachineDefinition[] WIRELESS_ENERGY_OUTPUT_HATCH_4096A = registerWirelessEnergyHatch(IO.OUT, 4096, PartAbility.OUTPUT_ENERGY, WIRELESS_ENERGY_HATCH_TIERS);

    public static void init() {}
}
