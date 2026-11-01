package com.mrlacar.univom.mixin.gtceu.data.recipe.misc;

import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialStack;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.data.recipe.misc.RecyclingRecipes;
import com.mrlacar.univom.data.recipes.misc.RemeltingFurnaceRecipes;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

@Mixin(value = RecyclingRecipes.class, remap = false)
public class RecyclingRecipesMixin {
    @Inject(method = "registerExtractorRecycling", at = @At("HEAD"), cancellable = true)
    private static void univom$replaceExtractorWithRemelting(Consumer<FinishedRecipe> provider, ItemStack input, List<MaterialStack> materials, int multiplier, TagPrefix prefix, CallbackInfo ci) {
        MaterialStack fluidMs = materials.stream()
                .filter(ms -> ms.material().hasProperty(PropertyKey.FLUID) && ms.material().getFluid() != null)
                .filter(ms -> ms.material().hasProperty(PropertyKey.BLAST))
                .findFirst().orElse(null);
        if (fluidMs != null) {
            RemeltingFurnaceRecipes.registerRemeltingRecipes(provider, input, materials);
            ci.cancel();
        }
    }
}
