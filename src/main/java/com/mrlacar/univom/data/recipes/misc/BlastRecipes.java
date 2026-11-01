package com.mrlacar.univom.data.recipes.misc;

import com.mrlacar.univom.common.data.ModItems;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.IV;
import static com.gregtechceu.gtceu.api.GTValues.VA;
import static com.gregtechceu.gtceu.common.data.GTMaterials.Helium;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.BLAST_RECIPES;

public class BlastRecipes {
    public static void init(Consumer<FinishedRecipe> provider) {
        BLAST_RECIPES.recipeBuilder("blast_netherite_ingot")
                .circuitMeta(1)
                .inputItems(ModItems.NANOGRID_NETHERITE_PRECURSOR_DUST)
                .outputItems(ModItems.HOT_NETHERITE_INGOT)
                .duration(800).EUt(VA[IV]).blastFurnaceTemp(4500).save(provider);

        BLAST_RECIPES.recipeBuilder("blast_netherite_ingot_gas")
                .circuitMeta(2)
                .inputItems(ModItems.NANOGRID_NETHERITE_PRECURSOR_DUST)
                .inputFluids(Helium.getFluid(100))
                .outputItems(ModItems.HOT_NETHERITE_INGOT)
                .duration(536).EUt(VA[IV]).blastFurnaceTemp(4500).save(provider);
    }
}
