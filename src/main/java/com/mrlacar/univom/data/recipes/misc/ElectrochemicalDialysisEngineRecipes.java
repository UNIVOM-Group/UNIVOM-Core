package com.mrlacar.univom.data.recipes.misc;

import com.mrlacar.univom.UNIVOM;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.mrlacar.univom.common.data.ModMaterials.*;
import static com.mrlacar.univom.common.data.ModRecipeTypes.ELECTROCHEMICAL_DIALYSIS_ENGINE_RECIPES;

public class ElectrochemicalDialysisEngineRecipes {
    public static void init(Consumer<FinishedRecipe> provider) {
        ELECTROCHEMICAL_DIALYSIS_ENGINE_RECIPES.recipeBuilder(UNIVOM.id("acetic_acid_and_sodium_bicarbonate_solution"))
                .inputFluids(AceticAcid.getFluid(1000))
                .inputFluids(SodiumBicarbonateSolution.getFluid(1000))
                .outputFluids(SodiumAcetateSolution.getFluid(2000))
                .outputFluids(CarbonDioxide.getFluid(1000))
                .duration(2900).EUt(-32).save(provider);

        ELECTROCHEMICAL_DIALYSIS_ENGINE_RECIPES.recipeBuilder(UNIVOM.id("acetic_acid_and_ammonia_solution"))
                .inputFluids(AceticAcid.getFluid(1000))
                .inputFluids(AmmoniaSolution.getFluid(1000))
                .outputFluids(AmmoniumAcetateSolution.getFluid(2000))
                .duration(2800).EUt(-64).save(provider);

        ELECTROCHEMICAL_DIALYSIS_ENGINE_RECIPES.recipeBuilder(UNIVOM.id("phosphoric_acid_and_ammonia_solution"))
                .inputFluids(PhosphoricAcid.getFluid(1000))
                .inputFluids(AmmoniaSolution.getFluid(1000))
                .outputFluids(AmmoniumPhosphateSolution.getFluid(2000))
                .duration(2320).EUt(-96).save(provider);

        ELECTROCHEMICAL_DIALYSIS_ENGINE_RECIPES.recipeBuilder(UNIVOM.id("phosphoric_acid_and_sodium_hydroxide"))
                .inputFluids(PhosphoricAcid.getFluid(1000))
                .inputFluids(SodiumHydroxideSolution.getFluid(1000))
                .outputFluids(TrisodiumPhosphateSolution.getFluid(2000))
                .duration(2400).EUt(-128).save(provider);

        ELECTROCHEMICAL_DIALYSIS_ENGINE_RECIPES.recipeBuilder(UNIVOM.id("diluted_sulfuric_acid_and_potassium_hydroxide_solution"))
                .inputFluids(DilutedSulfuricAcid.getFluid(1000))
                .inputFluids(PotassiumHydroxideSolution.getFluid(1000))
                .outputFluids(PotassiumSulfateSolution.getFluid(2000))
                .duration(1280).EUt(-256).save(provider);

        ELECTROCHEMICAL_DIALYSIS_ENGINE_RECIPES.recipeBuilder(UNIVOM.id("sulfuric_acid_and_ammonia_solution"))
                .inputFluids(SulfuricAcid.getFluid(1000))
                .inputFluids(AmmoniaSolution.getFluid(1000))
                .outputFluids(AmmoniumSulfateSolution.getFluid(2000))
                .duration(1920).EUt(-384).save(provider);

        ELECTROCHEMICAL_DIALYSIS_ENGINE_RECIPES.recipeBuilder(UNIVOM.id("sulfuric_acid_and_sodium_hydroxide_solution"))
                .inputFluids(SulfuricAcid.getFluid(1000))
                .inputFluids(SodiumHydroxideSolution.getFluid(1000))
                .outputFluids(SodiumSulfateSolution.getFluid(2000))
                .duration(1680).EUt(-512).save(provider);

        ELECTROCHEMICAL_DIALYSIS_ENGINE_RECIPES.recipeBuilder(UNIVOM.id("trifluoroacetic_acid_and_sodium_amide_ammonia_solution"))
                .inputFluids(TrifluoroaceticAcid.getFluid(1000))
                .inputFluids(SodiumAmideAmmoniaSolution.getFluid(1000))
                .outputFluids(SodiumTrifluoroacetateSolution.getFluid(2000))
                .duration(1480).EUt(-1024).save(provider);
    }
}