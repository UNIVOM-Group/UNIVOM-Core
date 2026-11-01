package com.mrlacar.univom.data.recipes.integration.ad_astra;

import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.common.item.armor.SpaceFederationOxygenTank;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

import static com.mrlacar.univom.common.data.ModRecipeTypes.OXYGEN_DISTRIBUTOR_RECIPES;
import static com.mrlacar.univom.common.machine.electric.OxygenDistributor.BASE_EUT;
import static com.mrlacar.univom.common.machine.electric.OxygenDistributor.BASE_OXYGEN;

public class OxygenDistributorRecipes {
    public static void init(Consumer<FinishedRecipe> provider) {
        OXYGEN_DISTRIBUTOR_RECIPES.recipeBuilder(UNIVOM.id("oxygen_distribution"))
                .perTick(true)
                .inputFluids(FluidIngredient.of(SpaceFederationOxygenTank.OXYGEN, BASE_OXYGEN))
                .perTick(false)
                .duration(20).EUt(BASE_EUT).save(provider);
    }
}
