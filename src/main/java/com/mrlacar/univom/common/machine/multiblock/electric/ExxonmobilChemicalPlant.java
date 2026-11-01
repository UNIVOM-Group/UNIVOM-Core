package com.mrlacar.univom.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.client.model.machine.MachineRenderState;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.mrlacar.univom.api.machine.multiblock.PartTierWorkableElectricMultiblockMachine;
import com.mrlacar.univom.api.machine.property.ModMachineModelProperties;
import com.mrlacar.univom.api.machine.tier.PartTierDimension;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ExxonmobilChemicalPlant extends PartTierWorkableElectricMultiblockMachine {

    public static final PartTierDimension CASING = PartTierDimension.of("casing", () -> List.of(
            GTBlocks.CASING_STEEL_SOLID.get(),
            GTBlocks.CASING_ALUMINIUM_FROSTPROOF.get(),
            GTBlocks.CASING_STAINLESS_CLEAN.get(),
            GTBlocks.CASING_TITANIUM_STABLE.get(),
            GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get()));

    public static final PartTierDimension PIPE = PartTierDimension.of("pipe", () -> List.of(
            GTBlocks.CASING_STEEL_PIPE.get(),
            GTBlocks.CASING_TITANIUM_PIPE.get(),
            GTBlocks.CASING_TUNGSTENSTEEL_PIPE.get()));

    public static final PartTierDimension COIL = PartTierDimension.coils("coil");

    public static final PartTierDimension MACHINE_CASING = PartTierDimension.of("machine_casing", () -> List.of(
            GTBlocks.MACHINE_CASING_LV.get(),
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

    public static final PartTierDimension[] DIMENSIONS = { CASING, PIPE, COIL, MACHINE_CASING };

    private static final int PARALLEL_PER_PIPE_TIER = 8;
    private static final double SPEED_PER_COIL_TIER = 0.5;
    private static final String VOLTAGE_FAIL_KEY = "univom.multiblock.exxonmobil_chemical_plant.voltage_capped";

    public ExxonmobilChemicalPlant(IMachineBlockEntity holder, Object... args) {
        super(holder, DIMENSIONS, args);
    }

    @Override
    protected void applyRenderState() {
        int tier = getPartTier(CASING);
        if (tier == PartTierDimension.NO_TIER) tier = 1;
        MachineRenderState state = getRenderState();
        if (state.hasProperty(ModMachineModelProperties.CASING_TIER)) {
            setRenderState(state.setValue(ModMachineModelProperties.CASING_TIER,
                    Math.min(tier, ModMachineModelProperties.MAX_CASING_TIER)));
        }
    }

    @Nullable
    @Override
    public BlockState getAppearanceState() {
        MachineRenderState state = getRenderState();
        return state.hasProperty(ModMachineModelProperties.CASING_TIER) ?
                CASING.blockAt(state.getValue(ModMachineModelProperties.CASING_TIER)).defaultBlockState() : null;
    }

    public static ModifierFunction recipeModifier(MetaMachine machine, GTRecipe recipe) {
        if (!(machine instanceof ExxonmobilChemicalPlant plant)) return RecipeModifier.nullWrongType(ExxonmobilChemicalPlant.class, machine);

        int machineCasingTier = plant.getPartTier(MACHINE_CASING);
        if (machineCasingTier != PartTierDimension.NO_TIER && machineCasingTier <= GTValues.UV &&
                plant.getTier() > machineCasingTier) {
            return ModifierFunction.cancel(Component.translatable(VOLTAGE_FAIL_KEY,
                    GTValues.VNF[Math.min(machineCasingTier, GTValues.VNF.length - 1)]));
        }

        int pipeTier = Math.max(plant.getPartTier(PIPE), 1);
        int coilTier = Math.max(plant.getEffectivePartTier(COIL), 1);
        double speed = 1.0 + SPEED_PER_COIL_TIER * (coilTier - 1);

        return ModifierFunction.builder()
                .parallels(1 + PARALLEL_PER_PIPE_TIER * (pipeTier - 1))
                .durationMultiplier(1.0 / speed)
                .build();
    }
}
