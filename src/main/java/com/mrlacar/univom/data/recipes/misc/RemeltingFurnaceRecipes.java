package com.mrlacar.univom.data.recipes.misc;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.ItemMaterialInfo;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialStack;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.common.data.ModRecipeTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.L;
import static com.gregtechceu.gtceu.api.GTValues.M;

public class RemeltingFurnaceRecipes {
    public static void init(Consumer<FinishedRecipe> provider) {
        for (var entry : ChemicalHelper.getAllItemInfos()) {
            ItemStack itemStack = entry.getFirst();
            ItemMaterialInfo materialInfo = entry.getSecond();
            List<MaterialStack> materialStacks = materialInfo.getMaterials();
            registerRemeltingRecipes(provider, itemStack, materialStacks);
        }
    }

    public static void registerRemeltingRecipes(Consumer<FinishedRecipe> provider, ItemStack input, List<MaterialStack> components) {
        MaterialStack fluidMs = components.stream()
                .filter(ms -> ms.material().hasProperty(PropertyKey.FLUID) && ms.material().getFluid() != null)
                .filter(ms -> ms.material().hasProperty(PropertyKey.BLAST))
                .findFirst().orElse(null);
        if (fluidMs == null) return;

        MaterialStack itemMs = components.stream()
                .filter(ms -> !ms.material().equals(fluidMs.material()))
                .filter(ms -> ms.amount() >= M / 9)
                .findFirst().orElse(null);

        int blastTemp = fluidMs.material().getBlastTemperature();

        int multiplier = (blastTemp < 2000) ? 4 : 16;

        long duration = fluidMs.amount() * fluidMs.material().getMass();
        if (itemMs != null) duration += (itemMs.amount() * itemMs.material().getMass());
        duration = Math.max(1L, duration / M);

        MaterialEntry entry = ChemicalHelper.getMaterialEntry(input.getItem());
        TagKey<Item> inputTag = null;
        if (!entry.isEmpty() && !entry.material().isNull()) inputTag = ChemicalHelper.getTag(entry.tagPrefix(), entry.material());

        ResourceLocation itemPath = BuiltInRegistries.ITEM.getKey(input.getItem());
        GTRecipeBuilder builder = ModRecipeTypes.REMELTING_FURNACE_RECIPES
                .recipeBuilder(UNIVOM.id("remelt_" + itemPath.getPath()))
                .outputFluids(fluidMs.material().getFluid((int) (fluidMs.amount() * L / M)))
                .duration((int) duration)
                .EUt((long) GTValues.VA[GTValues.LV] * multiplier)
                .blastFurnaceTemp(blastTemp);

        if (inputTag == null) {
            builder.inputItems(input.copy());
        } else {
            builder.inputItems(inputTag);
        }
        if (itemMs != null) {
            ItemStack outputStack = ChemicalHelper.getIngotOrDust(itemMs);
            if (!outputStack.isEmpty()) builder.outputItems(outputStack);
        }

        builder.save(provider);
    }
}
