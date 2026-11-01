package com.mrlacar.univom.common.data;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.mrlacar.univom.common.machine.multiblock.electric.Greenhouse;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import static com.gregtechceu.gtceu.common.data.GTRecipeModifiers.ELECTRIC_OVERCLOCK;

public class ModRecipeModifiers {
    private static final OverclockingLogic.OCResult NONE_OC_FACTOR = new OverclockingLogic.OCResult(1.0, 1.0, 0, 1);

    public static final RecipeModifier OC_NONE = ELECTRIC_OVERCLOCK.apply((params, maxV) -> NONE_OC_FACTOR);


    public static @NotNull ModifierFunction greenhouseOverclock(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof Greenhouse greenhouse)) {
            return RecipeModifier.nullWrongType(Greenhouse.class, machine);
        }

        if (RecipeHelper.getRecipeEUtTier(recipe) > greenhouse.getTier()) {
            return ModifierFunction.cancel(Component.translatable("gtceu.recipe_modifier.insufficient_voltage"));
        }

        Level level = greenhouse.getLevel();

        if (level != null) {
            int yOffset = 0;
            int minLightLevel = 9;

            BlockPos controllerPos = greenhouse.getRecipeLogic().getMachine().getHolder().getCurrentPos();
            Direction facing = greenhouse.getFrontFacing();
            Direction backwardFacing = facing.getOpposite();
            Direction right = facing.getClockWise();

            for (int depth = 0; depth <= 6; depth++) {
                for (int width = -2; width <= 2; width++) {
                    BlockPos targetPos = controllerPos
                            .relative(backwardFacing, depth)
                            .relative(right, width)
                            .above(yOffset);

                    int lightLevel = level.getMaxLocalRawBrightness(targetPos.above());

                    if (lightLevel < minLightLevel) {
                        return ModifierFunction.cancel(Component.translatable("univom.recipe_modifier.insufficient_light_level"));
                    }
                }
            }
        }

        return OverclockingLogic.NON_PERFECT_OVERCLOCK_SUBTICK.getModifier(machine, recipe, greenhouse.getOverclockVoltage());
    }
}
