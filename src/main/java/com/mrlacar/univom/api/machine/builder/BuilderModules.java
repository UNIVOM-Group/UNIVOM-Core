package com.mrlacar.univom.api.machine.builder;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.common.data.GTBlocks;

import net.minecraft.world.level.block.Block;

import java.util.Comparator;
import java.util.List;

public final class BuilderModules {

    public static final BuilderModuleCategory COIL = BuilderModuleCategory.of("coil",
            () -> GTCEuAPI.HEATING_COILS.entrySet().stream()
                    .sorted(Comparator.comparingInt(entry -> entry.getKey().getCoilTemperature()))
                    .<Block>map(entry -> entry.getValue().get())
                    .toList());

    public static final BuilderModuleCategory SOLID_CASING = BuilderModuleCategory.of("solid_casing", () -> List.of(
            GTBlocks.CASING_STEEL_SOLID.get(),
            GTBlocks.CASING_BRONZE_BRICKS.get(),
            GTBlocks.CASING_INVAR_HEATPROOF.get(),
            GTBlocks.CASING_ALUMINIUM_FROSTPROOF.get(),
            GTBlocks.CASING_STAINLESS_CLEAN.get(),
            GTBlocks.CASING_TITANIUM_STABLE.get(),
            GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get(),
            GTBlocks.CASING_PTFE_INERT.get(),
            GTBlocks.CASING_HSSE_STURDY.get(),
            GTBlocks.CASING_PALLADIUM_SUBSTATION.get()));

    public static final BuilderModuleCategory PIPE_CASING = BuilderModuleCategory.of("pipe_casing", () -> List.of(
            GTBlocks.CASING_STEEL_PIPE.get(),
            GTBlocks.CASING_BRONZE_PIPE.get(),
            GTBlocks.CASING_TITANIUM_PIPE.get(),
            GTBlocks.CASING_TUNGSTENSTEEL_PIPE.get(),
            GTBlocks.CASING_POLYTETRAFLUOROETHYLENE_PIPE.get()));

    public static final BuilderModuleCategory MACHINE_CASING = BuilderModuleCategory.of("machine_casing", () -> List.of(
            GTBlocks.MACHINE_CASING_LV.get(),
            GTBlocks.MACHINE_CASING_ULV.get(),
            GTBlocks.MACHINE_CASING_MV.get(),
            GTBlocks.MACHINE_CASING_HV.get(),
            GTBlocks.MACHINE_CASING_EV.get(),
            GTBlocks.MACHINE_CASING_IV.get(),
            GTBlocks.MACHINE_CASING_LuV.get(),
            GTBlocks.MACHINE_CASING_ZPM.get(),
            GTBlocks.MACHINE_CASING_UV.get(),
            GTBlocks.MACHINE_CASING_UHV.get(),
            GTBlocks.MACHINE_CASING_UEV.get(),
            GTBlocks.MACHINE_CASING_UIV.get(),
            GTBlocks.MACHINE_CASING_UXV.get(),
            GTBlocks.MACHINE_CASING_OpV.get(),
            GTBlocks.MACHINE_CASING_MAX.get()));

    public static final BuilderModuleCategory GEARBOX = BuilderModuleCategory.of("gearbox", () -> List.of(
            GTBlocks.CASING_STEEL_GEARBOX.get(),
            GTBlocks.CASING_BRONZE_GEARBOX.get(),
            GTBlocks.CASING_STAINLESS_STEEL_GEARBOX.get(),
            GTBlocks.CASING_TITANIUM_GEARBOX.get(),
            GTBlocks.CASING_TUNGSTENSTEEL_GEARBOX.get()));

    public static final BuilderModuleCategory TURBINE_CASING = BuilderModuleCategory.of("turbine_casing", () -> List.of(
            GTBlocks.CASING_STEEL_TURBINE.get(),
            GTBlocks.CASING_STAINLESS_TURBINE.get(),
            GTBlocks.CASING_TITANIUM_TURBINE.get(),
            GTBlocks.CASING_TUNGSTENSTEEL_TURBINE.get()));

    public static final List<BuilderModuleCategory> ALL = List.of(
            COIL, SOLID_CASING, PIPE_CASING, MACHINE_CASING, GEARBOX, TURBINE_CASING);

    private BuilderModules() {}
}
