package com.mrlacar.univom.data.recipes.handler;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.mrlacar.univom.common.data.ModRecipeTypes;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import vectorwing.farmersdelight.common.registry.ModItems;

import java.util.List;
import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.LV;

public class GreenhouseRecipesHandler {
    private static final List<Item> crop = List.of(
            Items.CARROT,
            Items.POTATO,
            Items.SUGAR_CANE,
            Items.BAMBOO,
            Items.KELP,
            Items.NETHER_WART,
            Items.RED_MUSHROOM,
            Items.BROWN_MUSHROOM,
            ModItems.ONION.get());

    private static final List<Item> crop_seed_in = List.of(
            Items.WHEAT_SEEDS,
            Items.BEETROOT_SEEDS,
            Items.MELON_SEEDS,
            Items.PUMPKIN_SEEDS,
            ModItems.CABBAGE_SEEDS.get(),
            ModItems.TOMATO_SEEDS.get());

    private static final List<Item> crop_seed_out = List.of(
            Items.WHEAT,
            Items.BEETROOT,
            Items.MELON,
            Items.PUMPKIN,
            ModItems.CABBAGE.get(),
            ModItems.TOMATO.get());

    public static void run(Consumer<FinishedRecipe> provider) {
        // Normal
        for (Item item : crop) {
            ModRecipeTypes.GREENHOUSE_RECIPES.recipeBuilder(item.toString())
                    .notConsumable(item)
                    .inputFluids(GTMaterials.Water.getFluid(1000))
                    .outputItems(item, 32)
                    .duration(320)
                    .EUt(GTValues.VA[LV])
                    .circuitMeta(1)
                    .save(provider);
        }

        for (int i = 0; i < crop_seed_in.size(); i++) {
            Item inputSeed = crop_seed_in.get(i);
            Item outputCrop = crop_seed_out.get(i);
            ModRecipeTypes.GREENHOUSE_RECIPES.recipeBuilder(inputSeed.toString() + "_to_" + outputCrop.toString())
                    .notConsumable(inputSeed)
                    .inputFluids(GTMaterials.Water.getFluid(1000))
                    .outputItems(outputCrop, 24)
                    .duration(320)
                    .EUt(GTValues.VA[LV])
                    .circuitMeta(1)
                    .save(provider);
        }

        // Boosted
        for (Item item : crop) {
            ModRecipeTypes.GREENHOUSE_RECIPES.recipeBuilder(item.toString() + "_boosted")
                    .notConsumable(item)
                    .inputItems(GTItems.FERTILIZER.get(), 4)
                    .inputFluids(GTMaterials.Water.getFluid(1000))
                    .outputItems(item, 64)
                    .duration(320)
                    .EUt(GTValues.VA[LV])
                    .circuitMeta(2)
                    .save(provider);
        }

        for (int i = 0; i < crop_seed_in.size(); i++) {
            Item inputSeed = crop_seed_in.get(i);
            Item outputCrop = crop_seed_out.get(i);
            ModRecipeTypes.GREENHOUSE_RECIPES.recipeBuilder(inputSeed.toString() + "_to_" + outputCrop.toString() + "_boosted")
                    .notConsumable(inputSeed)
                    .inputItems(GTItems.FERTILIZER.get(), 4)
                    .inputFluids(GTMaterials.Water.getFluid(1000))
                    .outputItems(outputCrop, 48)
                    .duration(320)
                    .EUt(GTValues.VA[LV])
                    .circuitMeta(2)
                    .save(provider);
        }
    }
}
