package com.mrlacar.univom.common.recipe.condition;

import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mrlacar.univom.common.data.ModRecipeConditions;
import com.mrlacar.univom.common.machine.multiblock.unique.PressureChamber.PressureChamber;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
public class PressureCondition extends RecipeCondition<PressureCondition> {

    public static final Codec<PressureCondition> CODEC = RecordCodecBuilder
            .create(instance -> RecipeCondition.isReverse(instance).and(
                    Codec.INT.fieldOf("pressure").forGetter(PressureCondition::getPressure))
                    .apply(instance, PressureCondition::new));

    @Getter
    private int pressure;

    public PressureCondition(int pressure) {
        super();
        this.pressure = pressure;
    }

    public PressureCondition(boolean isReverse, int pressure) {
        super(isReverse);
        this.pressure = pressure;
    }

    @Override
    public RecipeConditionType<PressureCondition> getType() {
        return ModRecipeConditions.PRESSURE;
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("univom.recipe.pressure_condition", this.pressure);
    }

    @Override
    public boolean testCondition(@NotNull GTRecipe recipe, @NotNull RecipeLogic recipeLogic) {
        return recipeLogic.getMachine() instanceof PressureChamber chamber && chamber.getPressure() >= this.pressure;
    }

    @Override
    public PressureCondition createTemplate() {
        return new PressureCondition();
    }
}
