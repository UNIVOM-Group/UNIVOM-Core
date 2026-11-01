package com.mrlacar.univom.mixin.gtceu;

import com.mrlacar.univom.integration.emi.multipage.MultiblockInfoEmiCategory;
import com.mrlacar.univom.integration.emi.recipe.Ae2PatternTerminalHandler;

import com.gregtechceu.gtceu.integration.emi.GTEMIPlugin;

import net.minecraft.world.inventory.MenuType;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GTEMIPlugin.class, remap = false)
public class GTEMIPluginMixin {
    @Redirect(
              method = "register",
              at = @At(
                       value = "INVOKE",
                       target = "Ldev/emi/emi/api/EmiRegistry;addCategory(Ldev/emi/emi/api/recipe/EmiRecipeCategory;)V"))
    private void blockAddCategory(EmiRegistry instance, EmiRecipeCategory category) {
        if (category == MultiblockInfoEmiCategory.CATEGORY) {
            return;
        }
        instance.addCategory(category);
    }

    @Redirect(
              method = "register",
              at = @At(
                       value = "INVOKE",
                       target = "Lcom/gregtechceu/gtceu/integration/emi/multipage/MultiblockInfoEmiCategory;registerDisplays(Ldev/emi/emi/api/EmiRegistry;)V"))
    private void blockRegisterDisplays(EmiRegistry registry) {}

    @Redirect(
              method = "register",
              at = @At(
                       value = "INVOKE",
                       target = "Ldev/emi/emi/api/EmiRegistry;addRecipeHandler(Lnet/minecraft/world/inventory/MenuType;Ldev/emi/emi/api/recipe/handler/EmiRecipeHandler;)V"))
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void blockAddRecipeHandler(EmiRegistry instance, MenuType<?> type, EmiRecipeHandler<?> handler) {
        if (handler instanceof Ae2PatternTerminalHandler) return;
        instance.addRecipeHandler((MenuType) type, (EmiRecipeHandler) handler);
    }
}
