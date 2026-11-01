package com.mrlacar.univom.data.recipes.misc;

import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.LV;
import static com.gregtechceu.gtceu.api.GTValues.VA;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.dust;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.MIXER_RECIPES;
import static com.mrlacar.univom.common.data.ModMaterials.*;

public class MixerRecipes {
    public static void init(Consumer<FinishedRecipe> provider) {
        MIXER_RECIPES.recipeBuilder("sodium_bicarbonate_solution_from_sodium_bicarbonate")
                .inputItems(dust, SodiumBicarbonate)
                .inputFluids(Water.getFluid(1000))
                .outputFluids(SodiumBicarbonateSolution.getFluid(1000))
                .duration(100).EUt(16).save(provider);

        MIXER_RECIPES.recipeBuilder("potassium_hydroxide_solution_from_potassium_hydroxide")
                .inputItems(dust, PotassiumHydroxide)
                .inputFluids(Water.getFluid(1000))
                .outputFluids(PotassiumHydroxideSolution.getFluid(1000))
                .duration(100).EUt(16).save(provider);

        MIXER_RECIPES.recipeBuilder("sodium_hydroxide_solution_from_sodium_hydroxide")
                .inputItems(dust, SodiumHydroxide)
                .inputFluids(Water.getFluid(1000))
                .outputFluids(SodiumHydroxideSolution.getFluid(1000))
                .duration(100).EUt(16).save(provider);

        MIXER_RECIPES.recipeBuilder("sodium_amide_ammonia_solution_from_sodium_amide")
                .inputItems(dust, SodiumAmide)
                .inputFluids(Ammonia.getFluid(1000))
                .outputFluids(SodiumAmideAmmoniaSolution.getFluid(1000))
                .duration(100).EUt(VA[LV]).save(provider);
    }
}
