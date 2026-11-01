package com.mrlacar.univom.mixin.gtceu.api.capability.recipe;

import com.mrlacar.univom.api.recipe.lookup.IngredientCompressor;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;

@Mixin(value = ItemRecipeCapability.class, remap = false)
public abstract class ItemRecipeCapabilityMixin {

    @Inject(method = "compressIngredients", at = @At("HEAD"), cancellable = true)
    private void univom$compressIngredients(Collection<Object> ingredients,
                                            CallbackInfoReturnable<List<Object>> cir) {
        cir.setReturnValue(IngredientCompressor.compress(ingredients));
    }
}
