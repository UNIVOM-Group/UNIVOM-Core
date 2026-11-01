package com.mrlacar.univom.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.mrlacar.univom.common.data.machines.ModMultiMachines;
import com.mrlacar.univom.common.machine.multiblock.electric.LargeHadronCollider;
import net.minecraft.network.chat.Component;

public class LargeHadronColliderEnergyConfigurator implements IFancyConfigurator {
    private final LargeHadronCollider machine;

    public LargeHadronColliderEnergyConfigurator(LargeHadronCollider machine) {
        this.machine = machine;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("univom.gui.large_hadron_collider_energy.title");
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(ModMultiMachines.LARGE_HADRON_COLLIDER.asStack());
    }

    @Override
    public Widget createConfigurator() {
        var group = new WidgetGroup(0, 0, 120, 74);

        group.addWidget(new LabelWidget(4, 4, Component.translatable("univom.gui.large_hadron_collider_energy.target.1")));
        group.addWidget(new IntInputWidget(4, 16, 112, 14,
                () -> (int) machine.getTargetEnergy1(),
                machine::setTargetEnergy1)
                .setMin(0)
                .setMax(Integer.MAX_VALUE));

        group.addWidget(new LabelWidget(4, 38, Component.translatable("univom.gui.large_hadron_collider_energy.target.2")));
        group.addWidget(new IntInputWidget(4, 50, 112, 14,
                () -> (int) machine.getTargetEnergy2(),
                machine::setTargetEnergy2)
                .setMin(0)
                .setMax(Integer.MAX_VALUE));

        return group;
    }
}
