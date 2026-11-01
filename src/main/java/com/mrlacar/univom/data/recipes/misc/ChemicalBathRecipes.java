package com.mrlacar.univom.data.recipes.misc;

import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.HV;
import static com.gregtechceu.gtceu.api.GTValues.VA;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.dust;
import static com.gregtechceu.gtceu.common.data.GTMaterials.HydrofluoricAcid;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.CHEMICAL_BATH_RECIPES;
import static com.mrlacar.univom.common.data.ModMaterials.*;

public class ChemicalBathRecipes {
    public static void init(Consumer<FinishedRecipe> provider) {
        CHEMICAL_BATH_RECIPES.recipeBuilder("deslagged_netherite_from_impure_netherite_scrap")
                .inputItems(dust, ImpureNetheriteScrap)
                .inputFluids(HydrofluoricAcid.getFluid(1000))
                .outputItems(dust, DeslaggedNetherite)
                .outputFluids(FluorosilicateWastewater.getFluid(1000))
                .duration(300).EUt(VA[HV]).save(provider);
    }
}
