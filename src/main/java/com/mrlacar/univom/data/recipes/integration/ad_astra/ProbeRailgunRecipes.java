package com.mrlacar.univom.data.recipes.integration.ad_astra;

import com.mrlacar.univom.UNIVOM;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.LV;
import static com.gregtechceu.gtceu.api.GTValues.VA;
import static com.mrlacar.univom.api.ModValues.SYSTEM;
import static com.mrlacar.univom.common.data.ModItems.PROBE_CONTAINER;
import static com.mrlacar.univom.common.data.ModRecipeTypes.PROBE_RAILGUN_RECIPES;

public class ProbeRailgunRecipes {
    public static void init(Consumer<FinishedRecipe> provider) {
        //Deep Space Scan//
        PROBE_RAILGUN_RECIPES.recipeBuilder(UNIVOM.id("solar_system"))
                .inputItems(PROBE_CONTAINER)
                .duration(100).EUt(VA[LV])
                .addData(SYSTEM, "proxima_centauri")
                .save(provider);
    }
}
