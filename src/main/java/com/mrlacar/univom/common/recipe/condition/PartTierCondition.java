package com.mrlacar.univom.common.recipe.condition;

import com.mrlacar.univom.api.machine.multiblock.PartTierWorkableElectricMultiblockMachine;
import com.mrlacar.univom.api.machine.tier.PartTierDimension;
import com.mrlacar.univom.common.data.ModRecipeConditions;

import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;

import net.minecraft.network.chat.Component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
public class PartTierCondition extends RecipeCondition<PartTierCondition> {

    public static final Codec<PartTierCondition> CODEC = RecordCodecBuilder
            .create(instance -> RecipeCondition.isReverse(instance).and(instance.group(
                    Codec.STRING.fieldOf("dimension").forGetter(PartTierCondition::getDimension),
                    Codec.INT.fieldOf("min_tier").forGetter(PartTierCondition::getMinTier)))
                    .apply(instance, PartTierCondition::new));

    @Getter
    private String dimension;
    @Getter
    private int minTier;

    public PartTierCondition(String dimension, int minTier) {
        super();
        this.dimension = dimension;
        this.minTier = minTier;
    }

    public PartTierCondition(boolean isReverse, String dimension, int minTier) {
        super(isReverse);
        this.dimension = dimension;
        this.minTier = minTier;
    }

    public PartTierCondition(PartTierDimension dimension, int minTier) {
        this(dimension.name(), minTier);
    }

    @Override
    public RecipeConditionType<PartTierCondition> getType() {
        return ModRecipeConditions.PART_TIER;
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("univom.recipe.part_tier_condition",
                Component.translatable("univom.part_tier." + this.dimension), this.minTier);
    }

    @Override
    public boolean testCondition(@NotNull GTRecipe recipe, @NotNull RecipeLogic recipeLogic) {
        return recipeLogic.getMachine() instanceof PartTierWorkableElectricMultiblockMachine machine &&
                machine.getPartTier(this.dimension) >= this.minTier;
    }

    @Override
    public PartTierCondition createTemplate() {
        return new PartTierCondition();
    }
}
