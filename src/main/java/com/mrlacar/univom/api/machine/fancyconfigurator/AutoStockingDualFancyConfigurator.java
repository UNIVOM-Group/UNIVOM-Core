package com.mrlacar.univom.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.mrlacar.univom.integration.ae2.machine.feature.multiblock.IMEStockingDualPart;
import net.minecraft.network.chat.Component;

public class AutoStockingDualFancyConfigurator implements IFancyConfigurator {
    private final IMEStockingDualPart machine;

    public AutoStockingDualFancyConfigurator(IMEStockingDualPart machine) {
        this.machine = machine;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gtceu.gui.adv_stocking_config.title");
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(GTItems.TOOL_DATA_STICK.asStack());
    }

    @Override
    public Widget createConfigurator() {
        var group = new WidgetGroup(0, 0, 90, 104);

        group.addWidget(new LabelWidget(4, 2, "gtceu.gui.title.adv_stocking_config.min_item_count"));
        group.addWidget(new IntInputWidget(4, 12, 81, 14, machine::getMinStackSize,
                machine::setMinStackSize).setMin(1)
                .appendHoverTooltips(Component.translatable("gtceu.gui.adv_stocking_config.min_item_count")));

        group.addWidget(new LabelWidget(4, 36, "gtceu.gui.title.adv_stocking_config.min_fluid_count"));
        group.addWidget(new IntInputWidget(4, 46, 81, 14, machine::getFluidMinStackSize,
                machine::setFluidMinStackSize).setMin(1)
                .appendHoverTooltips(Component.translatable("gtceu.gui.adv_stocking_config.min_fluid_count")));

        group.addWidget(new LabelWidget(4, 70, "gtceu.gui.title.adv_stocking_config.ticks_per_cycle"));
        group.addWidget(new IntInputWidget(4, 80, 81, 14, machine::getTicksPerCycle,
                machine::setTicksPerCycle).setMin(ConfigHolder.INSTANCE.compat.ae2.updateIntervals)
                .setHoverTooltips(Component.translatable("gtceu.gui.adv_stocking_config.ticks_per_cycle")));

        return group;
    }
}
