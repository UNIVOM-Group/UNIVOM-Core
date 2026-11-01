package com.mrlacar.univom.data.recipes.misc;

import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.dust;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.DISTILLATION_RECIPES;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.DISTILLERY_RECIPES;
import static com.mrlacar.univom.common.data.ModMaterials.*;

public class DistillationRecipes {
    public static void init(Consumer<FinishedRecipe> provider) {
        //Distillation//
        DISTILLATION_RECIPES.recipeBuilder("distill_sodium_acetate_solution")
                .inputFluids(SodiumAcetateSolution.getFluid(1000))
                .outputItems(dust, SodiumAcetate)
                .outputFluids(Water.getFluid(1000))
                .duration(200).EUt(24).save(provider);

        DISTILLATION_RECIPES.recipeBuilder("distill_ammonium_acetate_solution")
                .inputFluids(AmmoniumAcetateSolution.getFluid(1000))
                .outputFluids(AceticAcid.getFluid(1000))
                .outputFluids(Ammonia.getFluid(1000))
                .outputFluids(Water.getFluid(1000))
                .duration(400).EUt(120).save(provider);
        //Distillery//
        DISTILLERY_RECIPES.recipeBuilder("distill_ammonium_phosphate_solution")
                .inputFluids(AmmoniumPhosphateSolution.getFluid(1000))
                .outputItems(dust, AmmoniumPhosphate)
                .outputFluids(Water.getFluid(1000))
                .duration(240).EUt(24).save(provider);

        DISTILLERY_RECIPES.recipeBuilder("distill_trisodium_phosphate_solution")
                .inputFluids(TrisodiumPhosphateSolution.getFluid(1000))
                .outputItems(dust, TrisodiumPhosphate)
                .outputFluids(Water.getFluid(1000))
                .duration(200).EUt(24).save(provider);

        DISTILLERY_RECIPES.recipeBuilder("distill_potassium_sulfate_solution")
                .inputFluids(PotassiumSulfateSolution.getFluid(1000))
                .outputItems(dust, PotassiumSulfate)
                .outputFluids(Water.getFluid(1000))
                .duration(200).EUt(24).save(provider);

        DISTILLERY_RECIPES.recipeBuilder("distill_ammonium_sulfate_solution")
                .inputFluids(AmmoniumSulfateSolution.getFluid(1000))
                .outputItems(dust, AmmoniumSulfate)
                .outputFluids(Water.getFluid(1000))
                .duration(200).EUt(24).save(provider);

        DISTILLERY_RECIPES.recipeBuilder("distill_sodium_sulfate_solution")
                .inputFluids(SodiumSulfateSolution.getFluid(1000))
                .outputItems(dust, SodiumSulfate)
                .outputFluids(Water.getFluid(1000))
                .duration(200).EUt(24).save(provider);

        DISTILLERY_RECIPES.recipeBuilder("distill_crude_diluted_acetic_acid")
                .inputFluids(CrudeDilutedAceticAcid.getFluid(2000))
                .outputFluids(AceticAcid.getFluid(1000))
                .outputFluids(DistilledWater.getFluid(1000))
                .duration(300).EUt(120).save(provider);
    }
}
