package com.mrlacar.univom.common.data;

import com.mrlacar.univom.data.recipes.VanillaShapedCraftingRecipes;
import com.mrlacar.univom.data.recipes.generated.OreProcessorRecipeHandler;
import com.mrlacar.univom.data.recipes.handler.GreenhouseRecipesHandler;
import com.mrlacar.univom.data.recipes.integration.ad_astra.OxygenDistributorRecipes;
import com.mrlacar.univom.data.recipes.integration.ad_astra.ProbeRailgunRecipes;
import com.mrlacar.univom.data.recipes.misc.*;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.function.Consumer;

public class ModRecipes {
    public static final Set<ResourceLocation> RECIPE_FILTERS = new ObjectOpenHashSet<>();

    public static void init(Consumer<FinishedRecipe> originalConsumer) {
        Consumer<FinishedRecipe> consumer = recipe -> {
            if (!RECIPE_FILTERS.contains(recipe.getId())) {
                originalConsumer.accept(recipe);
            }
        };

        BlastRecipes.init(consumer);
        BreweryRecipes.init(consumer);
        CentrifugeRecipes.init(consumer);
        ChemicalBathRecipes.init(consumer);
        ChemistryRecipes.init(consumer);
        CrystallizerRecipes.init(consumer);
        DistillationRecipes.init(consumer);
        ElectrochemicalDialysisEngineRecipes.init(consumer);
        FermenterRecipes.init(consumer);
        GreenhouseRecipes.init(consumer);
        LargeHadronColliderRecipes.init(consumer);
        MaceratorRecipes.init(consumer);
        MixerRecipes.init(consumer);
        PressureChamberRecipes.init(consumer);
        RadioFrequencySourceRecipes.init(consumer);
        SuperconductorAssemblerRecipes.init(consumer);
        ThermalCentrifugeRecipes.init(consumer);
        VacuumFreezerRecipes.init(consumer);

        VanillaShapedCraftingRecipes.init(consumer);

        //Handler//
        GreenhouseRecipesHandler.run(consumer);

        //Generated//
        for (Material material : GTCEuAPI.materialManager.getRegisteredMaterials()) {
            OreProcessorRecipeHandler.run(consumer, material);
        }

        //Integration//
        OxygenDistributorRecipes.init(consumer);
        ProbeRailgunRecipes.init(consumer);
    }
}
