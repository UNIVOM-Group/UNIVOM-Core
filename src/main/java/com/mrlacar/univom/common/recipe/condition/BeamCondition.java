package com.mrlacar.univom.common.recipe.condition;

import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mrlacar.univom.common.data.ModItems;
import com.mrlacar.univom.common.data.ModRecipeConditions;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
public class BeamCondition extends RecipeCondition<BeamCondition> {
    public static final Codec<BeamCondition> CODEC = RecordCodecBuilder.create(instance -> RecipeCondition.isReverse(instance).and(instance.group(
            Codec.STRING.fieldOf("particle").forGetter(BeamCondition::getParticle),
            Codec.LONG.fieldOf("amount").forGetter(BeamCondition::getAmount)
    )).apply(instance, BeamCondition::new));

    @Getter
    private String particle;
    @Getter
    private long amount;

    public BeamCondition(String particle, long amount) {
        super();
        this.particle = particle;
        this.amount = amount;
    }

    public BeamCondition(boolean isReverse, String particle, long amount) {
        super(isReverse);
        this.particle = particle;
        this.amount = amount;
    }

    @Override
    public RecipeConditionType<BeamCondition> getType() {
        return ModRecipeConditions.BEAM;
    }

    @Override
    public Component getTooltips() {
        Component particleName = Component.translatable("particle.univom." + this.particle);
        return Component.translatable("univom.recipe.beam_condition", particleName, this.amount);
    }

    public SlotWidget setupParticleIcon(int xOffset, int yOffset) {
        ItemStack icon = Items.ENDER_PEARL.getDefaultInstance();
        if (ModItems.PARTICLE_MODELS.containsKey(this.particle)) {
            icon = ModItems.PARTICLE_MODELS.get(this.particle).asStack();
        }

        CustomItemStackHandler handler = new CustomItemStackHandler(1);
        SlotWidget slot = new SlotWidget(handler, 0, xOffset, yOffset, false, false)
                .setIngredientIO(IngredientIO.INPUT);
        handler.setStackInSlot(0, icon);

        return slot;
    }

    @Override
    public boolean testCondition(@NotNull GTRecipe recipe, @NotNull RecipeLogic recipeLogic) {
        return true;
    }

    @Override
    public BeamCondition createTemplate() {
        return new BeamCondition();
    }
}
