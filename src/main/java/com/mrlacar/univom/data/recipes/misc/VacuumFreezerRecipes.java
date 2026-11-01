package com.mrlacar.univom.data.recipes.misc;

import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.mrlacar.univom.common.data.ModItems;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.EV;
import static com.gregtechceu.gtceu.api.GTValues.VA;
import static com.gregtechceu.gtceu.common.data.GTMaterials.Helium;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.VACUUM_RECIPES;

public class VacuumFreezerRecipes {
    public static void init(Consumer<FinishedRecipe> provider) {
        VACUUM_RECIPES.recipeBuilder("cool_hot_netherite_ingot")
                .inputItems(ModItems.HOT_NETHERITE_INGOT)
                .inputFluids(Helium.getFluid(FluidStorageKeys.LIQUID,500))
                .outputItems(Items.NETHERITE_INGOT)
                .outputFluids(Helium.getFluid(250))
                .duration(200).EUt(VA[EV]).save(provider);
    }
}
