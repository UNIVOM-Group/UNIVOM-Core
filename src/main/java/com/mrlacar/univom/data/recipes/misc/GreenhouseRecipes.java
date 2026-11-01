package com.mrlacar.univom.data.recipes.misc;

import com.mrlacar.univom.common.data.ModBlocks;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.IV;
import static com.gregtechceu.gtceu.api.GTValues.VA;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.mrlacar.univom.common.data.ModRecipeTypes.GREENHOUSE_RECIPES;

public class GreenhouseRecipes {
    public static void init(Consumer<FinishedRecipe> provider) {
        GREENHOUSE_RECIPES.recipeBuilder("infernal_metallophyte_block")
                .inputItems(Items.BLAZE_POWDER, 2)
                .inputItems(Items.CRIMSON_FUNGUS, 2)
                .inputItems(Items.WARPED_FUNGUS, 2)
                .inputItems(Blocks.SOUL_SAND.asItem())
                .inputFluids(Mutagen.getFluid(1000))
                .inputFluids(HydrogenSulfide.getFluid(1000))
                .inputFluids(SterileGrowthMedium.getFluid(1000))
                .outputItems(ModBlocks.INFERNAL_METALLOPHYTE_BLOCK.asStack(2))
                .duration(800).EUt(VA[IV]).blastFurnaceTemp(4500).save(provider);
    }
}
