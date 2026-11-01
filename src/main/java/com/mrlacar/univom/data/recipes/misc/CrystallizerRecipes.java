package com.mrlacar.univom.data.recipes.misc;

import com.mrlacar.univom.UNIVOM;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.dust;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.dustSmall;
import static com.gregtechceu.gtceu.common.data.GTItems.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.BLAST_RECIPES;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.CUTTER_RECIPES;
import static com.mrlacar.univom.common.data.ModItems.SILICON_SOLAR_GRADE_SEED_CRYSTAL;
import static com.mrlacar.univom.common.data.ModMaterials.*;
import static com.mrlacar.univom.common.data.ModRecipeTypes.CRYSTALLIZER_RECIPES;

public class CrystallizerRecipes {
    public static void init(Consumer<FinishedRecipe> provider) {
        //Monocrystalline Silicon Boule//
        BLAST_RECIPES.recipeBuilder(UNIVOM.id("silicon_solar_grade_seed_crystal_from_silicon_solar_grade"))
                .inputItems(dust, SiliconSolarGrade, 8)
                .inputFluids(Nitrogen.getFluid(1000))
                .notConsumable(SHAPE_MOLD_BALL)
                .circuitMeta(2)
                .outputItems(SILICON_SOLAR_GRADE_SEED_CRYSTAL)
                .duration(2250).EUt(VA[MV]).blastFurnaceTemp(1784).save(provider);

        CRYSTALLIZER_RECIPES.recipeBuilder(UNIVOM.id("silicon_boule_from_silicon_solar_grade_seed_crystal"))
                .inputItems(SILICON_SOLAR_GRADE_SEED_CRYSTAL)
                .inputItems(dustSmall, GalliumArsenide)
                .inputFluids(SiliconSolarGrade.getFluid(4608))
                .outputItems(SILICON_BOULE)
                .duration(2250).EUt(VA[HV]).blastFurnaceTemp(1784).save(provider);

        CUTTER_RECIPES.recipeBuilder("silicon_solar_grade_seed_crystal_from_silicon_boule")
                .inputItems(SILICON_BOULE)
                .inputFluids(Lubricant.getFluid(20))
                .outputItems(SILICON_WAFER, 16)
                .outputItems(SILICON_SOLAR_GRADE_SEED_CRYSTAL)
                .duration(400).EUt(64).save(provider);

        //Other Chemicals//
        CRYSTALLIZER_RECIPES.recipeBuilder(UNIVOM.id("ammonium_acetate_from_ammonium_acetate_solution"))
                .inputFluids(AmmoniumAcetateSolution.getFluid(1000))
                .outputItems(dust, AmmoniumAcetate)
                .outputFluids(Water.getFluid(1000))
                .duration(120).EUt(VA[LV]).blastFurnaceTemp(273).save(provider);
    }
}
