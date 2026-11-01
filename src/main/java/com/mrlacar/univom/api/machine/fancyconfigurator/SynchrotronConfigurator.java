package com.mrlacar.univom.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.mrlacar.univom.common.data.machines.ModMultiMachines;
import com.mrlacar.univom.common.machine.multiblock.electric.Synchrotron;
import net.minecraft.network.chat.Component;

public class SynchrotronConfigurator implements IFancyConfigurator {
    private final Synchrotron machine;

    public SynchrotronConfigurator(Synchrotron machine) {
        this.machine = machine;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("univom.gui.synchrotron_configurator.title");
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(ModMultiMachines.SYNCHROTRON.asStack());
    }

    @Override
    public Widget createConfigurator() {
        var group = new WidgetGroup(0, 0, 120, 108);

        group.addWidget(new LabelWidget(4, 4, Component.translatable("univom.gui.synchrotron_configurator.eut")));
        group.addWidget(new IntInputWidget(4, 16, 112, 14,
                () -> (int) machine.getTargetEUt(),
                machine::setTargetEUt)
                .setMin(122880)
                .setMax(2000000000));

        group.addWidget(new LabelWidget(4, 38, Component.translatable("univom.gui.synchrotron_configurator.duration")));
        group.addWidget(new IntInputWidget(4, 50, 112, 14,
                machine::getTargetDuration,
                machine::setTargetDuration)
                .setMin(20)
                .setMax(72000));

        group.addWidget(new LabelWidget(4, 72, Component.translatable("univom.gui.synchrotron_configurator.flux")));
        group.addWidget(new IntInputWidget(4, 84, 112, 14,
                () -> (int) machine.getTargetFlux(),
                machine::setTargetFlux)
                .setMin(1)
                .setMax(1000000));

        return group;
    }
}
