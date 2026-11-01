package com.mrlacar.univom.api.recipe.lookup;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.AbstractMapIngredient;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.MapIngredientTypeManager;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class SearchIngredientCache {

    public static final List<List<AbstractMapIngredient>> NOT_CACHEABLE =
            Collections.unmodifiableList(new ObjectArrayList<>(0));

    private static final Map<IRecipeCapabilityHolder, Entry> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private SearchIngredientCache() {}

    public static void invalidate() {
        CACHE.clear();
    }

    @Nullable
    public static List<List<AbstractMapIngredient>> resolve(@NotNull IRecipeCapabilityHolder holder) {
        Map<RecipeCapability<?>, List<IRecipeHandler<?>>> handlerMap = holder.getCapabilitiesFlat()
                .getOrDefault(IO.IN, Collections.emptyMap());
        if (handlerMap.isEmpty()) {
            return null;
        }

        List<RecipeCapability<?>> caps = new ObjectArrayList<>();
        List<List<Object>> batches = new ObjectArrayList<>();
        List<Object> flat = new ObjectArrayList<>();

        for (var entry : handlerMap.entrySet()) {
            RecipeCapability<?> cap = entry.getKey();
            if (!cap.isRecipeSearchFilter()) {
                continue;
            }
            for (IRecipeHandler<?> handler : entry.getValue()) {
                List<Object> contents = handler.getContents();
                for (int i = 0; i < contents.size(); i++) {
                    Object content = contents.get(i);
                    if (!(content instanceof ItemStack) && !(content instanceof FluidStack)) {
                        return NOT_CACHEABLE;
                    }
                    flat.add(content);
                }
                caps.add(cap);
                batches.add(contents);
            }
        }

        Entry cached = CACHE.get(holder);
        if (cached != null && cached.matches(flat)) {
            return cached.ingredients;
        }

        List<List<AbstractMapIngredient>> ingredients = build(caps, batches, handlerMap.size());
        CACHE.put(holder, new Entry(snapshot(flat), ingredients));
        return ingredients;
    }

    @Nullable
    private static List<List<AbstractMapIngredient>> build(@NotNull List<RecipeCapability<?>> caps,
                                                           @NotNull List<List<Object>> batches, int handlerCount) {
        List<List<AbstractMapIngredient>> list = new ObjectArrayList<>(handlerCount * 8);
        for (int i = 0; i < caps.size(); i++) {
            RecipeCapability<?> cap = caps.get(i);
            for (Object ingredient : cap.compressIngredients(batches.get(i))) {
                list.add(MapIngredientTypeManager.getFrom(ingredient, cap));
            }
        }
        return list.isEmpty() ? null : list;
    }

    private static Object[] snapshot(@NotNull List<Object> contents) {
        Object[] copy = new Object[contents.size()];
        for (int i = 0; i < copy.length; i++) {
            Object content = contents.get(i);
            copy[i] = content instanceof ItemStack stack ? stack.copy() : ((FluidStack) content).copy();
        }
        return copy;
    }

    private record Entry(Object[] snapshot, @Nullable List<List<AbstractMapIngredient>> ingredients) {

        boolean matches(@NotNull List<Object> contents) {
            if (contents.size() != snapshot.length) {
                return false;
            }
            for (int i = 0; i < snapshot.length; i++) {
                Object current = contents.get(i);
                Object previous = snapshot[i];
                if (current instanceof ItemStack stack) {
                    if (!(previous instanceof ItemStack other) || stack.getCount() != other.getCount() ||
                            !ItemStack.isSameItemSameTags(stack, other)) {
                        return false;
                    }
                } else {
                    FluidStack stack = (FluidStack) current;
                    if (!(previous instanceof FluidStack other) || stack.getAmount() != other.getAmount() ||
                            !stack.isFluidEqual(other)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
