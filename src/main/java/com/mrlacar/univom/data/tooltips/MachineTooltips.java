package com.mrlacar.univom.data.tooltips;

import com.mrlacar.univom.common.machine.multiblock.unique.PressureChamber.PressureChamber;
import com.mrlacar.univom.common.machine.multiblock.unique.PressureChamber.PressureChamberLayout;
import com.mrlacar.univom.common.pipelike.pressure.PressureProcessing;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class MachineTooltips {
    //Steam//
    public static final Component[] STEAM_FORGE_HAMMER = {
            Component.translatable("univom.machine.steam_forge_hammer.tooltip.0")
    };

    public static final Component[] STEAM_EXTRACTION_MACHINE = {
            Component.translatable("univom.machine.steam_extraction_machine.tooltip.0")
    };

    public static final Component[] STEAM_CENTRIFUGAL_UNIT = {
            Component.translatable("univom.machine.steam_centrifugal_unit.tooltip.0")
    };

    public static final Component[] STEAM_ORE_WASHER = {
            Component.translatable("univom.machine.steam_ore_washer.tooltip.0")
    };

    public static final Component[] STEAM_COMPRESSOR = {
            Component.translatable("univom.machine.steam_compressor.tooltip.0")
    };

    public static final Component[] STEAM_MIXING_VESSEL = {
            Component.translatable("univom.machine.steam_mixing_vessel.tooltip.0")
    };

    public static final Component[] STEAM_ALLOY_SMELTER = {
            Component.translatable("univom.machine.steam_alloy_smelter.tooltip.0")
    };

    //Electric//
    public static final Component[] GREENHOUSE = {
            Component.translatable("univom.machine.greenhouse.tooltip.0"),
            Component.translatable("univom.machine.greenhouse.tooltip.1"),
            Component.translatable("univom.machine.greenhouse.tooltip.2")
    };

    public static final Component[] SIMPLE_SLAUGHTERHOUSE = {
            Component.translatable("univom.machine.simple_slaughterhouse.tooltip.0"),
            Component.translatable("univom.machine.simple_slaughterhouse.tooltip.1"),
            Component.translatable("univom.machine.simple_slaughterhouse.tooltip.2"),
            Component.translatable("univom.machine.simple_slaughterhouse.tooltip.3")
    };

    public static final Component[] REMELTING_FURNACE = {
            Component.translatable("univom.machine.remelting_furnace.tooltip.0"),
            Component.translatable("gtceu.machine.electric_blast_furnace.tooltip.0"),
            Component.translatable("gtceu.machine.electric_blast_furnace.tooltip.1"),
            Component.translatable("gtceu.machine.electric_blast_furnace.tooltip.2")
    };

    public static final Component[] CRYSTALLIZER = {
            Component.translatable("univom.machine.crystallizer.tooltip.0")
    };

    public static final Component[] FISHING_PORT = {
            Component.translatable("univom.machine.fishing_port.tooltip.0"),
            Component.translatable("univom.machine.fishing_port.tooltip.1"),
            Component.translatable("univom.machine.fishing_port.tooltip.2"),
            Component.translatable("univom.machine.fishing_port.tooltip.3"),
            Component.translatable("univom.machine.fishing_port.tooltip.4"),
            Component.translatable("univom.machine.fishing_port.tooltip.5"),
            Component.translatable("univom.machine.common_tooltip.output_amount").append("Voltage Tier ^ 2").withStyle(ChatFormatting.LIGHT_PURPLE)
    };

    public static final Component[] ASTRONOMICAL_ANALYSIS_STATION = {
            Component.translatable("univom.machine.astronomical_analysis_station.tooltip.0"),
            Component.translatable("univom.machine.astronomical_analysis_station.tooltip.1"),
            Component.translatable("univom.machine.astronomical_analysis_station.tooltip.2"),
            Component.translatable("univom.machine.astronomical_analysis_station.tooltip.3")
    };

    public static final Component[] PROBE_RAILGUN = {
            Component.translatable("univom.machine.probe_railgun.tooltip.0"),
            Component.translatable("univom.machine.probe_railgun.tooltip.1"),
            Component.translatable("univom.machine.probe_railgun.tooltip.2"),
            Component.translatable("univom.machine.probe_railgun.tooltip.3"),
            Component.translatable("univom.machine.probe_railgun.tooltip.4"),
            Component.translatable("univom.machine.probe_railgun.tooltip.5"),
            Component.translatable("univom.machine.probe_railgun.tooltip.6"),
            Component.translatable("univom.machine.probe_railgun.tooltip.7"),
            Component.translatable("univom.machine.probe_railgun.tooltip.8"),
            Component.translatable("univom.machine.probe_railgun.tooltip.9"),
            Component.translatable("univom.machine.probe_railgun.tooltip.10"),
            Component.translatable("univom.machine.probe_railgun.tooltip.11"),
            Component.translatable("univom.machine.probe_railgun.tooltip.12"),
            Component.translatable("univom.machine.probe_railgun.tooltip.13"),
    };

    public static final Component[] FISSION_REACTOR = {
            Component.translatable("univom.machine.fission_reactor.tooltip.0"),
            Component.translatable("univom.machine.fission_reactor.tooltip.1"),
            Component.translatable("univom.machine.fission_reactor.tooltip.2"),
            Component.translatable("univom.machine.fission_reactor.tooltip.3"),
            Component.translatable("univom.machine.fission_reactor.tooltip.4"),
            Component.translatable("univom.machine.fission_reactor.tooltip.5"),
            Component.translatable("univom.machine.fission_reactor.tooltip.6"),
            Component.translatable("univom.machine.fission_reactor.tooltip.7"),
            Component.translatable("univom.machine.fission_reactor.tooltip.8"),
            Component.translatable("univom.machine.fission_reactor.tooltip.9"),
            Component.translatable("univom.machine.fission_reactor.tooltip.10"),
            Component.translatable("univom.machine.fission_reactor.tooltip.11"),
            Component.translatable("univom.machine.fission_reactor.tooltip.12"),
            Component.translatable("univom.machine.fission_reactor.tooltip.13"),
            Component.translatable("univom.machine.fission_reactor.tooltip.14"),
            Component.translatable("univom.machine.fission_reactor.tooltip.15"),
            Component.translatable("univom.machine.fission_reactor.tooltip.16"),
            Component.translatable("univom.machine.fission_reactor.tooltip.17"),
            Component.translatable("univom.machine.fission_reactor.tooltip.18"),
            Component.translatable("univom.machine.fission_reactor.tooltip.19"),
            Component.translatable("univom.machine.fission_reactor.tooltip.20"),
            Component.translatable("univom.machine.fission_reactor.tooltip.21"),
            Component.translatable("univom.machine.fission_reactor.tooltip.22"),
            Component.translatable("univom.machine.fission_reactor.tooltip.23"),
            Component.translatable("univom.machine.fission_reactor.tooltip.24"),
            Component.translatable("univom.machine.fission_reactor.tooltip.25")
    };

    public static final Component[] EXXONMOBIL_CHEMICAL_PLANT = {
            Component.translatable("univom.machine.exxonmobil_chemical_plant.tooltip.0"),
            Component.translatable("univom.machine.exxonmobil_chemical_plant.tooltip.1"),
            Component.translatable("univom.machine.exxonmobil_chemical_plant.tooltip.2"),
            Component.translatable("univom.machine.exxonmobil_chemical_plant.tooltip.3")
    };

    public static final Component[] PRESSURE_CHAMBER = {
            Component.translatable("univom.machine.pressure_chamber.tooltip.0"),
            Component.translatable("univom.machine.pressure_chamber.tooltip.1", 3, PressureChamberLayout.MAX_INTERIOR + 2),
            Component.translatable("univom.machine.pressure_chamber.tooltip.2", PressureChamber.VOLUME_PER_BLOCK),
            Component.translatable("univom.machine.pressure_chamber.tooltip.3", PressureChamberLayout.rating(1),
                    PressureChamberLayout.rating(2), PressureChamberLayout.rating(3), PressureChamberLayout.rating(4),
                    PressureChamberLayout.rating(5)),
            Component.translatable("univom.machine.pressure_chamber.tooltip.4", PressureProcessing.AIRLOCK_VOLUME),
            Component.translatable("univom.machine.pressure_chamber.tooltip.5"),
            Component.translatable("univom.machine.pressure_chamber.tooltip.6")
    };
}
