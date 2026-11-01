package com.mrlacar.univom.api.recipe.lookup;

import com.gregtechceu.gtceu.api.recipe.ingredient.IntCircuitIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.gregtechceu.gtceu.utils.IngredientEquality;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class IngredientCompressor {

    private IngredientCompressor() {}

    public static List<Object> compress(@Unmodifiable @NotNull Collection<Object> ingredients) {
        List<Object> list = new ObjectArrayList<>(ingredients.size());
        List<Ingredient> keptIngredients = new ArrayList<>();
        Reference2ObjectMap<Item, List<ItemStack>> keptStacks = new Reference2ObjectOpenHashMap<>();

        for (Object item : ingredients) {
            if (item instanceof Ingredient ingredient) {
                if (matchesKept(ingredient, keptIngredients, keptStacks)) {
                    continue;
                }
                if (isCircuit(ingredient)) {
                    list.add(0, ingredient);
                } else {
                    list.add(ingredient);
                }
                keptIngredients.add(ingredient);
            } else if (item instanceof ItemStack stack) {
                if (matchesKept(stack, keptIngredients, keptStacks)) {
                    continue;
                }
                list.add(stack);
                keptStacks.computeIfAbsent(stack.getItem(), key -> new ArrayList<>(1)).add(stack);
            }
        }
        return list;
    }

    private static boolean isCircuit(@NotNull Ingredient ingredient) {
        if (ingredient instanceof IntCircuitIngredient) {
            return true;
        }
        if (ingredient instanceof SizedIngredient sized && sized.getInner() instanceof IntCircuitIngredient) {
            return true;
        }
        return ingredient instanceof IntProviderIngredient provider &&
                provider.getInner() instanceof IntCircuitIngredient;
    }

    private static boolean matchesKept(@NotNull Ingredient ingredient, @NotNull List<Ingredient> keptIngredients,
                                       @NotNull Reference2ObjectMap<Item, List<ItemStack>> keptStacks) {
        for (Ingredient kept : keptIngredients) {
            if (IngredientEquality.ingredientEquals(ingredient, kept)) {
                return true;
            }
        }
        for (List<ItemStack> bucket : keptStacks.values()) {
            for (int i = 0; i < bucket.size(); i++) {
                if (ingredient.test(bucket.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesKept(@NotNull ItemStack stack, @NotNull List<Ingredient> keptIngredients,
                                       @NotNull Reference2ObjectMap<Item, List<ItemStack>> keptStacks) {
        for (Ingredient kept : keptIngredients) {
            if (kept.test(stack)) {
                return true;
            }
        }
        List<ItemStack> bucket = keptStacks.get(stack.getItem());
        if (bucket == null) {
            return false;
        }
        for (int i = 0; i < bucket.size(); i++) {
            if (GTUtil.isSameItemSameTags(stack, bucket.get(i))) {
                return true;
            }
        }
        return false;
    }
}
