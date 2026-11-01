package com.mrlacar.univom.data.recipes.misc;

import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.dust;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.CENTRIFUGE_RECIPES;
import static com.mrlacar.univom.common.data.ModMaterials.*;

public class CentrifugeRecipes {
    public static void init(Consumer<FinishedRecipe> provider) {
        CENTRIFUGE_RECIPES.recipeBuilder("bacteria_from_bacterial_metabolic_wastewater")
                .inputFluids(BacterialMetabolicWastewater.getFluid(1000))
                .outputFluids(Bacteria.getFluid(500))
                .outputFluids(DistilledWater.getFluid(500))
                .duration(200).EUt(VA[HV]).save(provider);

        CENTRIFUGE_RECIPES.recipeBuilder("sulfur_from_sulfur_rich_organic_wastewater")
                .inputFluids(SulfurRichOrganicWastewater.getFluid(2000))
                .outputItems(dust, Sulfur)
                .outputFluids(DistilledWater.getFluid(2000))
                .duration(300).EUt(VA[MV]).save(provider);

        CENTRIFUGE_RECIPES.recipeBuilder("stone_from_organic_impurity_waste_acid")
                .inputFluids(OrganicImpurityWasteAcid.getFluid(3000))
                .outputItems(dust, Stone)
                .outputFluids(CrudeDilutedAceticAcid.getFluid(2000))
                .outputFluids(Biomass.getFluid(1000))
                .duration(300).EUt(VA[HV]).save(provider);

        CENTRIFUGE_RECIPES.recipeBuilder("acetic_acid_from_chitinous_waste_acid")
                .inputFluids(ChitinousWasteAcid.getFluid(1000))
                .outputFluids(AceticAcid.getFluid(500))
                .outputFluids(BacterialSludge.getFluid(500))
                .duration(300).EUt(VA[HV]).save(provider);

        CENTRIFUGE_RECIPES.recipeBuilder("biomass_from_cellular_waste_slurry")
                .inputFluids(CellularWasteSlurry.getFluid(1000))
                .outputFluids(Biomass.getFluid(500))
                .outputFluids(AceticAcid.getFluid(500))
                .duration(400).EUt(VA[HV]).save(provider);

        CENTRIFUGE_RECIPES.recipeBuilder("warped_eps_fluid_from_biomineralized_organic_wastewater")
                .inputFluids(BiomineralizedOrganicWastewater.getFluid(6000))
                .outputFluids(WarpedEPSFluid.getFluid(2000))
                .outputFluids(DenaturedAqueousWaste.getFluid(4000))
                .duration(900).EUt(VA[EV]).save(provider);
    }
}
