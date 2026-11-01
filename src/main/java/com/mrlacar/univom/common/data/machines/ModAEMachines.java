package com.mrlacar.univom.common.data.machines;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.data.tooltips.CommonMachineTooltips;
import com.mrlacar.univom.integration.ae2.machine.*;
import net.minecraft.network.chat.Component;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.common.data.machines.GTMachineUtils.DUAL_INPUT_HATCH_ABILITIES;
import static com.gregtechceu.gtceu.common.data.machines.GTMachineUtils.DUAL_OUTPUT_HATCH_ABILITIES;
import static com.mrlacar.univom.api.registry.UNIVOMRegistration.REGISTRATE;

public class ModAEMachines {
    public final static MachineDefinition WIRELESS_ME_NETWORK_TRANSMITTER = REGISTRATE
            .machine("wireless_me_network_transmitter", WirelessMENetworkTransmitterMachine::new)
            .rotationState(RotationState.ALL)
            .colorOverlayTieredHullModel(UNIVOM.id("block/overlay/appeng/wireless_me_network_transmitter"))
            .tooltips(CommonMachineTooltips.WIRELESS_AE_MACHINE)
            .tier(LuV)
            .register();

    public final static MachineDefinition WIRELESS_ITEM_IMPORT_BUS_ME = REGISTRATE
            .machine("wireless_me_input_bus", WirelessMEInputBusPartMachine::new)
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.IMPORT_ITEMS)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/appeng/me_input_bus"))
            .tooltips(
                    Component.translatable("gtceu.machine.item_bus.import.tooltip"),
                    Component.translatable("gtceu.machine.me.item_import.tooltip"),
                    Component.translatable("gtceu.machine.me.copy_paste.tooltip"),
                    Component.translatable("gtceu.part_sharing.enabled"))
            .tooltips(CommonMachineTooltips.WIRELESS_AE_MACHINE)
            .tier(LuV)
            .register();

    public final static MachineDefinition WIRELESS_STOCKING_IMPORT_BUS_ME = REGISTRATE
            .machine("wireless_me_stocking_input_bus", WirelessMEStockingBusPartMachine::new)
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.IMPORT_ITEMS)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/appeng/me_input_bus"))
            .tooltips(
                    Component.translatable("gtceu.machine.item_bus.import.tooltip"),
                    Component.translatable("gtceu.machine.me.stocking_item.tooltip.0"),
                    Component.translatable("gtceu.machine.me_import_item_hatch.configs.tooltip"),
                    Component.translatable("gtceu.machine.me.copy_paste.tooltip"),
                    Component.translatable("gtceu.machine.me.stocking_item.tooltip.1"),
                    Component.translatable("gtceu.part_sharing.enabled"))
            .tooltips(CommonMachineTooltips.WIRELESS_AE_MACHINE)
            .tier(ZPM)
            .register();

    public final static MachineDefinition WIRELESS_ITEM_EXPORT_BUS_ME = REGISTRATE
            .machine("wireless_me_output_bus", WirelessMEOutputBusPartMachine::new)
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.EXPORT_ITEMS)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/appeng/me_output_bus"))
            .tooltips(
                    Component.translatable("gtceu.machine.item_bus.export.tooltip"),
                    Component.translatable("gtceu.machine.me.item_export.tooltip"),
                    Component.translatable("gtceu.machine.me.export.tooltip"),
                    Component.translatable("gtceu.part_sharing.enabled"))
            .tooltips(CommonMachineTooltips.WIRELESS_AE_MACHINE)
            .tier(LuV)
            .register();

    public final static MachineDefinition WIRELESS_FLUID_IMPORT_HATCH_ME = REGISTRATE
            .machine("wireless_me_input_hatch", WirelessMEInputHatchPartMachine::new)
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.IMPORT_FLUIDS)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/appeng/me_input_hatch"))
            .tooltips(
                    Component.translatable("gtceu.machine.fluid_hatch.import.tooltip"),
                    Component.translatable("gtceu.machine.me.fluid_import.tooltip"),
                    Component.translatable("gtceu.machine.me.copy_paste.tooltip"),
                    Component.translatable("gtceu.part_sharing.enabled"))
            .tooltips(CommonMachineTooltips.WIRELESS_AE_MACHINE)
            .tier(LuV)
            .register();

    public final static MachineDefinition WIRELESS_STOCKING_IMPORT_HATCH_ME = REGISTRATE
            .machine("wireless_me_stocking_input_hatch", WirelessMEStockingHatchPartMachine::new)
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.IMPORT_FLUIDS)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/appeng/me_input_hatch"))
            .tooltips(
                    Component.translatable("gtceu.machine.fluid_hatch.import.tooltip"),
                    Component.translatable("gtceu.machine.me.stocking_fluid.tooltip.0"),
                    Component.translatable("gtceu.machine.me_import_fluid_hatch.configs.tooltip"),
                    Component.translatable("gtceu.machine.me.copy_paste.tooltip"),
                    Component.translatable("gtceu.machine.me.stocking_fluid.tooltip.1"),
                    Component.translatable("gtceu.part_sharing.enabled"))
            .tooltips(CommonMachineTooltips.WIRELESS_AE_MACHINE)
            .tier(ZPM)
            .register();

    public final static MachineDefinition WIRELESS_FLUID_EXPORT_HATCH_ME = REGISTRATE
            .machine("wireless_me_output_hatch", WirelessMEOutputHatchPartMachine::new)
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.EXPORT_FLUIDS)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/appeng/me_output_hatch"))
            .tooltips(
                    Component.translatable("gtceu.machine.fluid_hatch.export.tooltip"),
                    Component.translatable("gtceu.machine.me.fluid_export.tooltip"),
                    Component.translatable("gtceu.machine.me.export.tooltip"),
                    Component.translatable("gtceu.part_sharing.enabled"))
            .tooltips(CommonMachineTooltips.WIRELESS_AE_MACHINE)
            .tier(LuV)
            .register();

    public final static MachineDefinition WIRELESS_DUAL_IMPORT_HATCH_ME = REGISTRATE
            .machine("wireless_me_dual_input_hatch", WirelessMEInputDualHatchPartMachine::new)
            .rotationState(RotationState.ALL)
            .abilities(DUAL_INPUT_HATCH_ABILITIES)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/appeng/me_input_hatch"))
            .tooltips(
                    Component.translatable("gtceu.machine.dual_hatch.import.tooltip"),
                    Component.translatable("univom.machine.me.dual_hatch.import.tooltip"),
                    Component.translatable("gtceu.machine.me.copy_paste.tooltip"),
                    Component.translatable("gtceu.part_sharing.enabled"))
            .tooltips(CommonMachineTooltips.WIRELESS_AE_MACHINE)
            .tier(LuV)
            .register();

    public final static MachineDefinition WIRELESS_STOCKING_DUAL_IMPORT_HATCH_ME = REGISTRATE
            .machine("wireless_me_stocking_dual_input_hatch", WirelessMEStockingInputDualHatchPartMachine::new)
            .rotationState(RotationState.ALL)
            .abilities(DUAL_INPUT_HATCH_ABILITIES)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/appeng/me_input_hatch"))
            .tooltips(
                    Component.translatable("gtceu.machine.dual_hatch.import.tooltip"),
                    Component.translatable("univom.machine.me.stocking_dual.tooltip.0"),
                    Component.translatable("univom.machine.me_dual_import_hatch.configs.tooltip"),
                    Component.translatable("gtceu.machine.me.copy_paste.tooltip"),
                    Component.translatable("univom.machine.me.stocking_dual.tooltip.1"),
                    Component.translatable("gtceu.part_sharing.enabled"))
            .tooltips(CommonMachineTooltips.WIRELESS_AE_MACHINE)
            .tier(ZPM)
            .register();

    public final static MachineDefinition WIRELESS_DUAL_EXPORT_HATCH_ME = REGISTRATE
            .machine("wireless_me_dual_output_hatch", WirelessMEOutputDualHatchPartMachine::new)
            .rotationState(RotationState.ALL)
            .abilities(DUAL_OUTPUT_HATCH_ABILITIES)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/appeng/me_output_hatch"))
            .tooltips(
                    Component.translatable("gtceu.machine.dual_hatch.export.tooltip"),
                    Component.translatable("univom.machine.me.dual_hatch.export.tooltip"),
                    Component.translatable("gtceu.machine.me.export.tooltip"),
                    Component.translatable("gtceu.part_sharing.enabled"))
            .tooltips(CommonMachineTooltips.WIRELESS_AE_MACHINE)
            .tier(LuV)
            .register();

    public static final MachineDefinition ADVANCED_ME_PATTERN_BUFFER = REGISTRATE
            .machine("advanced_me_pattern_buffer", (holder) -> new WirelessMEPatternBufferPartMachine(holder, 54))
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.IMPORT_ITEMS, PartAbility.IMPORT_FLUIDS, PartAbility.EXPORT_FLUIDS,
                    PartAbility.EXPORT_ITEMS)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/appeng/me_buffer_hatch"))
            .tooltips(
                    Component.translatable("block.gtceu.pattern_buffer.desc.0"),
                    Component.translatable("block.gtceu.pattern_buffer.desc.1"),
                    Component.translatable("block.gtceu.pattern_buffer.desc.2"),
                    Component.translatable("gtceu.part_sharing.enabled"))
            .tooltips(CommonMachineTooltips.WIRELESS_ME_PATTERN_BUFFER)
            .tooltips(CommonMachineTooltips.WIRELESS_AE_MACHINE)
            .tier(UV)
            .register();

    public static final MachineDefinition ADVANCED_ME_PATTERN_BUFFER_PROXY = REGISTRATE
            .machine("advanced_me_pattern_buffer_proxy", (holder) -> new WirelessMEPatternBufferProxyPartMachine(holder, 54))
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.IMPORT_ITEMS, PartAbility.IMPORT_FLUIDS, PartAbility.EXPORT_FLUIDS,
                    PartAbility.EXPORT_ITEMS)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/appeng/me_buffer_hatch_proxy"))
            .tooltips(
                    Component.translatable("block.gtceu.pattern_buffer_proxy.desc.0"),
                    Component.translatable("block.gtceu.pattern_buffer_proxy.desc.1"),
                    Component.translatable("block.gtceu.pattern_buffer_proxy.desc.2"),
                    Component.translatable("gtceu.part_sharing.enabled"))
            .tier(UV)
            .register();

    public static final MachineDefinition EXTENDED_ME_PATTERN_BUFFER = REGISTRATE
            .machine("extended_me_pattern_buffer", (holder) -> new WirelessMEPatternBufferPartMachine(holder, 81))
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.IMPORT_ITEMS, PartAbility.IMPORT_FLUIDS, PartAbility.EXPORT_FLUIDS,
                    PartAbility.EXPORT_ITEMS)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/appeng/me_buffer_hatch"))
            .tooltips(
                    Component.translatable("block.gtceu.pattern_buffer.desc.0"),
                    Component.translatable("block.gtceu.pattern_buffer.desc.1"),
                    Component.translatable("block.gtceu.pattern_buffer.desc.2"),
                    Component.translatable("gtceu.part_sharing.enabled"))
            .tooltips(CommonMachineTooltips.WIRELESS_ME_PATTERN_BUFFER)
            .tooltips(CommonMachineTooltips.WIRELESS_AE_MACHINE)
            .tier(UHV)
            .register();

    public static final MachineDefinition EXTENDED_ME_PATTERN_BUFFER_PROXY = REGISTRATE
            .machine("extended_me_pattern_buffer_proxy", (holder) -> new WirelessMEPatternBufferProxyPartMachine(holder, 81))
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.IMPORT_ITEMS, PartAbility.IMPORT_FLUIDS, PartAbility.EXPORT_FLUIDS,
                    PartAbility.EXPORT_ITEMS)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/appeng/me_buffer_hatch_proxy"))
            .tooltips(
                    Component.translatable("block.gtceu.pattern_buffer_proxy.desc.0"),
                    Component.translatable("block.gtceu.pattern_buffer_proxy.desc.1"),
                    Component.translatable("block.gtceu.pattern_buffer_proxy.desc.2"),
                    Component.translatable("gtceu.part_sharing.enabled"))
            .tier(UHV)
            .register();

    public static final MachineDefinition MOLECULAR_CONFORMATION_SYNTHESIZER_ME_PATTERN_BUFFER = REGISTRATE
            .machine("molecular_conformation_synthesizer_me_pattern_buffer", MolecularConformationSynthesizerMEPatternBufferPartMachine::new)
            .tier(IV)
            .rotationState(RotationState.ALL)
            .colorOverlayTieredHullModel(UNIVOM.id("block/overlay/appeng/molecular_conformation_synthesizer_me_buffer_hatch"))
            .register();

    public static void init() {}
}
