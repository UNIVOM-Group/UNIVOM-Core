package com.mrlacar.univom.data.recipes.misc;

import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.EV;
import static com.gregtechceu.gtceu.api.GTValues.VA;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.dust;
import static com.gregtechceu.gtceu.common.data.GTMaterials.Gold;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.THERMAL_CENTRIFUGE_RECIPES;
import static com.mrlacar.univom.common.data.ModMaterials.DeslaggedNetherite;
import static com.mrlacar.univom.common.data.ModMaterials.PurifiedNetheriteConcentrate;

public class ThermalCentrifugeRecipes {
    public static void init(Consumer<FinishedRecipe> provider) {
        THERMAL_CENTRIFUGE_RECIPES.recipeBuilder("purified_netherite_concentrate_from_deslagged_netherite")
                .inputItems(dust, DeslaggedNetherite)
                .outputItems(dust, PurifiedNetheriteConcentrate)
                .outputItems(dust, Gold)
                .duration(600).EUt(VA[EV]).save(provider);
    }
}
