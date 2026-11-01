package com.mrlacar.univom.common.data;

import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.mrlacar.univom.common.recipe.condition.BeamCondition;
import com.mrlacar.univom.common.recipe.condition.PartTierCondition;
import com.mrlacar.univom.common.recipe.condition.PressureCondition;
import com.mrlacar.univom.common.recipe.condition.PressureGasCondition;

public class ModRecipeConditions {
    public static final RecipeConditionType<BeamCondition> BEAM =
            GTRegistries.RECIPE_CONDITIONS.register("beam_condition", new RecipeConditionType<>(BeamCondition::new, BeamCondition.CODEC));

    public static final RecipeConditionType<PartTierCondition> PART_TIER =
            GTRegistries.RECIPE_CONDITIONS.register("part_tier_condition", new RecipeConditionType<>(PartTierCondition::new, PartTierCondition.CODEC));

    public static final RecipeConditionType<PressureCondition> PRESSURE =
            GTRegistries.RECIPE_CONDITIONS.register("pressure_chamber_condition", new RecipeConditionType<>(PressureCondition::new, PressureCondition.CODEC));

    public static final RecipeConditionType<PressureGasCondition> PRESSURE_GAS =
            GTRegistries.RECIPE_CONDITIONS.register("pressure_chamber_gas_condition", new RecipeConditionType<>(PressureGasCondition::new, PressureGasCondition.CODEC));

    public static void init() {}
}
