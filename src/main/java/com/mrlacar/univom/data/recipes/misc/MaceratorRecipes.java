package com.mrlacar.univom.data.recipes.misc;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.HV;
import static com.gregtechceu.gtceu.api.GTValues.VA;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.dust;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.MACERATOR_RECIPES;
import static com.mrlacar.univom.common.data.ModMaterials.ImpureNetheriteScrap;

public class MaceratorRecipes {
    public static void init(Consumer<FinishedRecipe> provider) {
        MACERATOR_RECIPES.recipeBuilder("impure_netherite_scrap_from_netherite_scrap")
                .inputItems(Items.NETHERITE_SCRAP)
                .outputItems(dust, ImpureNetheriteScrap)
                .duration(200).EUt(VA[HV]).save(provider);
    }
}
