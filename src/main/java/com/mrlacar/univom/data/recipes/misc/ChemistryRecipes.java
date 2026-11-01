package com.mrlacar.univom.data.recipes.misc;

import com.gregtechceu.gtceu.common.data.GTItems;
import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.common.data.ModBlocks;
import com.mrlacar.univom.common.data.ModItems;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.level.block.Blocks;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.CHEMICAL_RECIPES;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.LARGE_CHEMICAL_RECIPES;
import static com.mrlacar.univom.common.data.ModMaterials.*;

public class ChemistryRecipes {
    public static void init(Consumer<FinishedRecipe> provider) {
        //Silicon Solar Grade Process//
        CHEMICAL_RECIPES.recipeBuilder(UNIVOM.id("silicon_tetrachloride_from_silicon"))
                .circuitMeta(2)
                .inputItems(dust, Silicon)
                .inputFluids(Chlorine.getFluid(4000))
                .outputFluids(SiliconTetrachloride.getFluid(1000))
                .duration(400).EUt(VA[LV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder(UNIVOM.id("silicon_solar_grade_from_sodium"))
                .circuitMeta(1)
                .inputItems(dust, Sodium, 4)
                .inputFluids(SiliconTetrachloride.getFluid(1000))
                .outputItems(dust, SiliconSolarGrade)
                .outputItems(dust, Salt, 8)
                .duration(100).EUt(VA[LV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder(UNIVOM.id("silicon_solar_grade_from_zinc"))
                .circuitMeta(1)
                .inputItems(dust, Zinc, 2)
                .inputFluids(SiliconTetrachloride.getFluid(1000))
                .outputItems(dust, SiliconSolarGrade)
                .outputItems(dust, ZincChloride, 6)
                .duration(400).EUt(VA[LV]).save(provider);
        //Other Chemicals//
        CHEMICAL_RECIPES.recipeBuilder(UNIVOM.id("ammonia_solution_from_ammonia"))
                .inputFluids(Ammonia.getFluid(1000))
                .inputFluids(Water.getFluid(1000))
                .outputFluids(AmmoniaSolution.getFluid(1000))
                .duration(100).EUt(VA[LV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder(UNIVOM.id("ammonium_sulfate_from_ammonium_phosphate"))
                .inputItems(dust, AmmoniumPhosphate, 2)
                .inputFluids(SulfuricAcid.getFluid(3000))
                .outputItems(dust, AmmoniumSulfate, 3)
                .outputFluids(PhosphoricAcid.getFluid(2000))
                .duration(400).EUt(VA[MV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder("sodium_sulfate_from_trisodium_phosphate")
                .inputItems(dust, TrisodiumPhosphate, 2)
                .inputFluids(SulfuricAcid.getFluid(3000))
                .inputFluids(Water.getFluid(3000))
                .outputFluids(SodiumSulfateSolution.getFluid(3000))
                .outputFluids(PhosphoricAcid.getFluid(2000))
                .duration(400).EUt(VA[MV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder("sodium_sulfate_from_ammonium_sulfate")
                .inputItems(dust, AmmoniumSulfate)
                .inputItems(dust, SodiumHydroxide, 2)
                .outputItems(dust, SodiumSulfate)
                .outputFluids(Ammonia.getFluid(2000))
                .outputFluids(Water.getFluid(2000))
                .duration(300).EUt(VA[LV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder("potassium_sulfate_from_ammonium_sulfate")
                .inputItems(dust, AmmoniumSulfate)
                .inputItems(dust, PotassiumHydroxide, 2)
                .outputItems(dust, PotassiumSulfate)
                .outputFluids(Ammonia.getFluid(2000))
                .outputFluids(Water.getFluid(2000))
                .duration(300).EUt(VA[LV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder("trifluoroacetic_acid_from_acetic_acid")
                .circuitMeta(1)
                .notConsumable(plate, Nickel)
                .inputFluids(AceticAcid.getFluid(1000))
                .inputFluids(HydrofluoricAcid.getFluid(3000))
                .outputFluids(TrifluoroaceticAcid.getFluid(1000))
                .outputFluids(Hydrogen.getFluid(3000))
                .duration(200).EUt(VA[HV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder("sodium_amide_from_sodium")
                .notConsumable(dust, Iron)
                .inputItems(dust, Sodium, 2)
                .inputFluids(Ammonia.getFluid(2000))
                .outputItems(dust, SodiumAmide, 2)
                .outputFluids(Hydrogen.getFluid(1000))
                .duration(600).EUt(VA[LV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder("trifluoroacetic_acid_from_sodium_trifluoroacetate_solution")
                .inputFluids(SodiumTrifluoroacetateSolution.getFluid(4000))
                .inputFluids(SulfuricAcid.getFluid(1000))
                .outputItems(dust, SodiumSulfate, 1)
                .outputFluids(TrifluoroaceticAcid.getFluid(2000))
                .duration(240).EUt(VA[HV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder("tetrafluoroethylene_from_sodium_trifluoroacetate_solution")
                .circuitMeta(2)
                .inputFluids(SodiumTrifluoroacetateSolution.getFluid(4000))
                .outputFluids(Tetrafluoroethylene.getFluid(1000))
                .outputFluids(CarbonDioxide.getFluid(2000))
                .outputItems(dust, SodiumFluoride, 2)
                .duration(400).EUt(VA[EV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder("sodium_sulfate_from_sodium_fluoride")
                .inputItems(dust, SodiumFluoride, 2)
                .inputFluids(SulfuricAcid.getFluid(1000))
                .outputItems(dust, SodiumSulfate)
                .outputFluids(HydrofluoricAcid.getFluid(2000))
                .duration(300).EUt(VA[MV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder("oleum_from_sulfuric_acid")
                .inputFluids(SulfuricAcid.getFluid(1000))
                .inputFluids(SulfurTrioxide.getFluid(1000))
                .outputFluids(Oleum.getFluid(1000))
                .duration(200).EUt(VA[MV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder("hydrogen_chloride_from_hydrochloric_acid")
                .inputFluids(HydrochloricAcid.getFluid(1000))
                .inputFluids(SulfuricAcid.getFluid(1000))
                .outputFluids(HydrogenChloride.getFluid(1000))
                .outputFluids(DilutedSulfuricAcid.getFluid(1000))
                .duration(100).EUt(VA[LV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder("hydrochloric_acid_from_hydrogen_chloride")
                .inputFluids(HydrogenChloride.getFluid(1000))
                .inputFluids(Water.getFluid(1000))
                .outputFluids(HydrochloricAcid.getFluid(1000))
                .duration(100).EUt(VA[LV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder("chlorosulfuric_acid_from_oleum")
                .circuitMeta(1)
                .inputFluids(Oleum.getFluid(1000))
                .inputFluids(HydrogenChloride.getFluid(1000))
                .outputFluids(ChlorosulfuricAcid.getFluid(1000))
                .outputFluids(SulfuricAcid.getFluid(1000))
                .duration(300).EUt(VA[EV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder("crimson_saccharified_liquid_from_crimson_stem")
                .inputItems(Blocks.CRIMSON_STEM.asItem())
                .inputFluids(DilutedSulfuricAcid.getFluid(1000))
                .inputFluids(DistilledWater.getFluid(1000))
                .outputFluids(CrimsonSaccharifiedLiquid.getFluid(2000))
                .duration(400).EUt(VA[MV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder("concentrated_crimson_slurry_from_crimson_saccharified_liquid")
                .inputFluids(CrimsonSaccharifiedLiquid.getFluid(2000))
                .inputFluids(Bacteria.getFluid(2000))
                .outputFluids(ConcentratedCrimsonSlurry.getFluid(3000))
                .outputFluids(BacterialMetabolicWastewater.getFluid(1000))
                .duration(600).EUt(VA[MV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder("concentrated_crimson_lysate_from_concentrated_crimson_slurry")
                .inputFluids(ConcentratedCrimsonSlurry.getFluid(3000))
                .inputFluids(AceticAcid.getFluid(3000))
                .outputFluids(ConcentratedCrimsonLysate.getFluid(3000))
                .outputFluids(OrganicImpurityWasteAcid.getFluid(3000))
                .duration(300).EUt(VA[HV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder("active_cytochrome_carrier_from_concentrated_crimson_lysate")
                .inputItems(dust, IronIISulfate)
                .inputFluids(ConcentratedCrimsonLysate.getFluid(3000))
                .outputFluids(ActiveCytochromeCarrier.getFluid(2000))
                .outputFluids(DenaturedProteinWastewater.getFluid(1000))
                .duration(600).EUt(VA[EV]).save(provider);

        LARGE_CHEMICAL_RECIPES.recipeBuilder("warped_eps_fluid_from_warped_stem")
                .inputItems(Blocks.WARPED_STEM.asItem(), 4)
                .inputItems(dust, Netherrack, 4)
                .inputFluids(AceticAcid.getFluid(2000))
                .inputFluids(SterileGrowthMedium.getFluid(2000))
                .outputFluids(WarpedEPSFluid.getFluid(3000))
                .outputFluids(ChitinousWasteAcid.getFluid(1000))
                .duration(900).EUt(VA[EV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder("infernal_chelating_sap_from_infernal_metallophyte_block")
                .circuitMeta(1)
                .inputItems(ModBlocks.INFERNAL_METALLOPHYTE_BLOCK.asStack())
                .inputFluids(LiquidNetherAir.getFluid(1000))
                .inputFluids(AceticAcid.getFluid(2000))
                .outputFluids(InfernalChelatingSap.getFluid(1000))
                .outputFluids(NetherAir.getFluid(1000))
                .outputFluids(CellularWasteSlurry.getFluid(1000))
                .duration(400).EUt(VA[IV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder("extremophile_chemolithotrophic_sludge_from_bacterial_sludge")
                .inputItems(dust, Netherrack, 2)
                .inputFluids(BacterialSludge.getFluid(3000))
                .inputFluids(HydrogenSulfide.getFluid(2000))
                .inputFluids(Mutagen.getFluid(1000))
                .outputFluids(ExtremophileChemolithotrophicSludge.getFluid(4000))
                .outputFluids(SulfurRichOrganicWastewater.getFluid(2000))
                .duration(1200).EUt(VA[EV]).save(provider);

        LARGE_CHEMICAL_RECIPES.recipeBuilder("bio_chelated_netherite_complex_from_purified_netherite_concentrate")
                .circuitMeta(4)
                .inputItems(dust, PurifiedNetheriteConcentrate)
                .inputFluids(InfernalChelatingSap.getFluid(1000))
                .inputFluids(ExtremophileChemolithotrophicSludge.getFluid(3000))
                .inputFluids(ActiveCytochromeCarrier.getFluid(1000))
                .inputFluids(CarbonMonoxide.getFluid(4000))
                .inputFluids(SulfurDioxide.getFluid(2000))
                .outputFluids(BioChelatedNetheriteComplex.getFluid(4000))
                .outputFluids(BioleachingToxicWastewater.getFluid(1000))
                .duration(1200).EUt(VA[IV]).save(provider);

        LARGE_CHEMICAL_RECIPES.recipeBuilder("nanogrid_netherite_precursor_dust_from_gold")
                .inputItems(dust, Gold, 4)
                .inputFluids(BioChelatedNetheriteComplex.getFluid(2000))
                .inputFluids(WarpedEPSFluid.getFluid(1000))
                .outputItems(ModItems.NANOGRID_NETHERITE_PRECURSOR_DUST)
                .outputFluids(BiomineralizedOrganicWastewater.getFluid(3000))
                .duration(800).EUt(VA[IV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder("fertilizer_from_denatured_protein_wastewater")
                .inputItems(dust, Apatite)
                .inputFluids(DenaturedProteinWastewater.getFluid(1000))
                .outputItems(GTItems.FERTILIZER, 2)
                .outputFluids(Water.getFluid(1000))
                .duration(200).EUt(VA[MV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder("diluted_sulfuric_acid_from_bioleaching_toxic_wastewater")
                .inputFluids(BioleachingToxicWastewater.getFluid(1000))
                .inputFluids(Oxygen.getFluid(500))
                .outputItems(dust, Stone)
                .outputFluids(DilutedSulfuricAcid.getFluid(1000))
                .duration(600).EUt(VA[HV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder("diluted_sulfuric_acid_from_bioleaching_toxic_wastewater")
                .inputFluids(DenaturedAqueousWaste.getFluid(4000))
                .inputFluids(BacterialSludge.getFluid(1000))
                .outputFluids(Methane.getFluid(2000))
                .outputFluids(IndustrialWastewater.getFluid(3000))
                .duration(600).EUt(VA[HV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder("hydrofluoric_acid_from_fluorosilicate_wastewater")
                .inputFluids(FluorosilicateWastewater.getFluid(1000))
                .inputFluids(DistilledWater.getFluid(1000))
                .outputFluids(HydrofluoricAcid.getFluid(1000))
                .outputFluids(IndustrialWastewater.getFluid(1000))
                .duration(400).EUt(VA[HV]).save(provider);

        CHEMICAL_RECIPES.recipeBuilder("water_from_industrial_wastewater")
                .inputItems(dust, Calcite)
                .inputFluids(IndustrialWastewater.getFluid(1000))
                .outputItems(dust, Stone)
                .outputFluids(Water.getFluid(1000))
                .outputFluids(CarbonDioxide.getFluid(1000))
                .duration(200).EUt(VA[MV]).save(provider);
    }
}
