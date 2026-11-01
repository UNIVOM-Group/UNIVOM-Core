package com.mrlacar.univom.mixin.minecraft.world.item.crafting;

import com.mrlacar.univom.api.item.ToolKit;
import com.mrlacar.univom.world.player.ToolKitCrafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Ingredient.class)
public abstract class IngredientMixin {

    @Inject(method = "test(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void univom$acceptToolKit(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack == null || stack.isEmpty() || !ToolKit.isToolKit(stack)) {
            return;
        }
        if (ToolKitCrafting.test((Ingredient) (Object) this, stack)) {
            cir.setReturnValue(true);
        }
    }
}
