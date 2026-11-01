package com.mrlacar.univom.data.recipes.misc;

import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.common.recipe.condition.PressureCondition;
import com.mrlacar.univom.common.recipe.condition.PressureGasCondition;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.block;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.dust;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.gem;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.ingot;
import static com.gregtechceu.gtceu.common.data.GTMaterials.Coal;
import static com.gregtechceu.gtceu.common.data.GTMaterials.Diamond;
import static com.gregtechceu.gtceu.common.data.GTMaterials.RawRubber;
import static com.gregtechceu.gtceu.common.data.GTMaterials.Rubber;
import static com.gregtechceu.gtceu.common.data.GTMaterials.Steam;
import static com.gregtechceu.gtceu.common.data.GTMaterials.Sulfur;
import static com.mrlacar.univom.common.data.ModRecipeTypes.PRESSURE_CHAMBER_RECIPES;

public class PressureChamberRecipes {
    public static void init(Consumer<FinishedRecipe> provider) {
        PRESSURE_CHAMBER_RECIPES.recipeBuilder(UNIVOM.id("diamond_from_coal_block"))
                .inputItems(block, Coal, 8)
                .outputItems(gem, Diamond)
                .addCondition(new PressureCondition(40))
                .duration(400).save(provider);

        PRESSURE_CHAMBER_RECIPES.recipeBuilder(UNIVOM.id("rubber_steam_vulcanization"))
                .inputItems(dust, RawRubber, 3)
                .inputItems(dust, Sulfur)
                .outputItems(ingot, Rubber)
                .addCondition(new PressureCondition(5))
                .addCondition(new PressureGasCondition(Steam.getFluid(), 100))
                .duration(100).save(provider);
    }
}
