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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
public class PressureGasCondition extends RecipeCondition<PressureGasCondition> {

    public static final Codec<PressureGasCondition> CODEC = RecordCodecBuilder
            .create(instance -> RecipeCondition.isReverse(instance).and(instance.group(
                    ResourceLocation.CODEC.fieldOf("gas").forGetter(PressureGasCondition::getGas),
                    Codec.LONG.optionalFieldOf("consumption", 0L).forGetter(PressureGasCondition::getConsumption)))
                    .apply(instance, PressureGasCondition::new));

    @Getter
    private ResourceLocation gas = new ResourceLocation("minecraft", "empty");
    @Getter
    private long consumption;

    public PressureGasCondition(ResourceLocation gas, long consumption) {
        super();
        this.gas = gas;
        this.consumption = Math.max(0, consumption);
    }

    public PressureGasCondition(boolean isReverse, ResourceLocation gas, long consumption) {
        super(isReverse);
        this.gas = gas;
        this.consumption = Math.max(0, consumption);
    }

    public PressureGasCondition(Fluid gas, long consumption) {
        this(ForgeRegistries.FLUIDS.getKey(gas), consumption);
    }

    @Override
    public RecipeConditionType<PressureGasCondition> getType() {
        return ModRecipeConditions.PRESSURE_GAS;
    }

    @Override
    public Component getTooltips() {
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(this.gas);
        Component name = fluid == null || fluid == Fluids.EMPTY ? Component.literal(this.gas.toString()) :
                fluid.getFluidType().getDescription();
        return Component.translatable("univom.recipe.pressure_gas_condition", name);
    }

    @Override
    public boolean testCondition(@NotNull GTRecipe recipe, @NotNull RecipeLogic recipeLogic) {
        return recipeLogic.getMachine() instanceof PressureChamber chamber && this.gas.equals(chamber.getGas());
    }

    @Override
    public PressureGasCondition createTemplate() {
        return new PressureGasCondition();
    }
}
