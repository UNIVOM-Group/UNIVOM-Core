package com.mrlacar.univom.mixin.gtceu.integration.xei.widgets;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.integration.xei.widgets.GTRecipeWidget;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.mrlacar.univom.common.recipe.condition.BeamCondition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GTRecipeWidget.class, remap = false)
public abstract class GTRecipeWidgetMixin extends WidgetGroup {
    public GTRecipeWidgetMixin(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Shadow(remap = false)
    @Final
    private GTRecipe recipe;

    @Shadow(remap = false)
    @Final
    private int xOffset;

    @Inject(method = "setRecipeWidget", at = @At("TAIL"), remap = false)
    private void injectBeamConditionIcon(CallbackInfo ci) {
        for (RecipeCondition<?> condition : this.recipe.conditions) {
            if (condition instanceof BeamCondition beamCondition) {
                int width = this.recipe.recipeType.getRecipeUI().getJEISize().width;
                int height = this.recipe.recipeType.getRecipeUI().getJEISize().height;

                int iconX = width - this.xOffset - 66;
                int iconY = height - 32;

                this.addWidget(beamCondition.setupParticleIcon(iconX, iconY).setBackgroundTexture(IGuiTexture.EMPTY));
            }
        }
    }
}
