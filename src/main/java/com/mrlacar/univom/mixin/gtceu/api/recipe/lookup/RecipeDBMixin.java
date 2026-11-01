package com.mrlacar.univom.mixin.gtceu.api.recipe.lookup;

import com.mrlacar.univom.api.recipe.lookup.SearchIngredientCache;

import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.lookup.RecipeDB;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.AbstractMapIngredient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

@Mixin(value = RecipeDB.class, remap = false)
public abstract class RecipeDBMixin {

    @Inject(method = "iterator", at = @At("HEAD"), cancellable = true)
    private void univom$cachedIterator(IRecipeCapabilityHolder holder, Predicate<GTRecipe> predicate,
                                       CallbackInfoReturnable<RecipeDB.RecipeIterator> cir) {
        List<List<AbstractMapIngredient>> ingredients = SearchIngredientCache.resolve(holder);
        if (ingredients == SearchIngredientCache.NOT_CACHEABLE) {
            return;
        }
        if (ingredients == null) {
            cir.setReturnValue(null);
            return;
        }
        cir.setReturnValue(new RecipeDB.RecipeIterator((RecipeDB) (Object) this, ingredients, predicate));
    }
}
