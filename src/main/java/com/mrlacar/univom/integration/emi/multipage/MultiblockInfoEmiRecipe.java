package com.mrlacar.univom.integration.emi.multipage;

import com.mrlacar.univom.api.gui.widget.PatternPreviewWidget;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

import com.lowdragmc.lowdraglib.emi.ModularEmiRecipe;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.List;

import javax.annotation.Nullable;

public class MultiblockInfoEmiRecipe extends ModularEmiRecipe<WidgetGroup> {
    private final MultiblockMachineDefinition definition;

    public MultiblockInfoEmiRecipe(MultiblockMachineDefinition definition) {
        super(() -> PatternPreviewWidget.getPatternWidget(definition));
        this.definition = definition;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        super.addWidgets(widgets);
        SlotWidget slotWidget = new SlotWidget(EmiStack.of(definition.getItem().asItem()), 138, 12)
                .recipeContext(this)
                .drawBack(false);

        widgets.add(slotWidget);
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return MultiblockInfoEmiCategory.CATEGORY;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return definition.getId();
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of(EmiStack.of(definition.getItem()));
    }
}
