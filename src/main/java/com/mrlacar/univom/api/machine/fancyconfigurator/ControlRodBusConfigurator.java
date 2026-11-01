package com.mrlacar.univom.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.mrlacar.univom.common.data.ModItems;
import com.mrlacar.univom.common.machine.multiblock.part.ControlRodBusPartMachine;
import net.minecraft.network.chat.Component;

public class ControlRodBusConfigurator implements IFancyConfigurator {
    private final ControlRodBusPartMachine machine;

    public ControlRodBusConfigurator(ControlRodBusPartMachine machine) {
        this.machine = machine;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("univom.gui.control_rod_configurator.title");
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(ModItems.CONTROL_ROD_GRAPHITE.asStack());
    }

    @Override
    public Widget createConfigurator() {
        var group = new WidgetGroup(0, 0, 90, 40);
        group.addWidget(new IntInputWidget(4, 12, 81, 14, machine::getControlRodThresholdValue, machine::setControlRodThresholdValue)
                .setMin(0)
                .setMax(100));
        return group;
    }
}
