package com.mrlacar.univom.common.machine.multiblock.generator;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.EnergyStack;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LargeElectrochemicalDialysisEngine extends WorkableElectricMultiblockMachine {
    public LargeElectrochemicalDialysisEngine(IMachineBlockEntity holder) {
        super(holder);
    }

    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof WorkableElectricMultiblockMachine multiblockMachine)) {
            return RecipeModifier.nullWrongType(WorkableElectricMultiblockMachine.class, machine);
        }
        EnergyStack EUt = recipe.getOutputEUt();
        if (!EUt.isEmpty()) {
            int maxParallel = 4;
            int actualParallel = ParallelLogic.getParallelAmount(multiblockMachine, recipe, maxParallel);

            return ModifierFunction.builder()
                    .inputModifier(ContentModifier.multiplier(actualParallel))
                    .outputModifier(ContentModifier.multiplier(actualParallel))
                    .eutMultiplier(actualParallel)
                    .parallels(actualParallel)
                    .build();
        }
        return ModifierFunction.NULL;
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        MultiblockDisplayText.Builder builder = MultiblockDisplayText.builder(textList, isFormed())
                .setWorkingStatus(recipeLogic.isWorkingEnabled(), recipeLogic.isActive());

        long lastEUt = recipeLogic.getLastRecipe() != null ?
                recipeLogic.getLastRecipe().getOutputEUt().getTotalEU() : 0;

        if (isActive() && isWorkingEnabled()) {
            builder.addCurrentEnergyProductionLine(lastEUt);
        }

        builder.addWorkingStatusLine();
    }
}
