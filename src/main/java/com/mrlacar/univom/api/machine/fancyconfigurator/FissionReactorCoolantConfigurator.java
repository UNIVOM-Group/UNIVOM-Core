package com.mrlacar.univom.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.mrlacar.univom.common.machine.multiblock.electric.FissionReactor;
import net.minecraft.network.chat.Component;

public class FissionReactorCoolantConfigurator implements IFancyConfigurator {
    private final FissionReactor machine;

    public FissionReactorCoolantConfigurator(FissionReactor machine) {
        this.machine = machine;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("univom.gui.fission_reactor_coolant.title");
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(GTMaterials.DistilledWater.getBucket());
    }

    @Override
    public Widget createConfigurator() {
        var group = new WidgetGroup(0, 0, 90, 40);
        group.addWidget(new IntInputWidget(4, 12, 81, 14, machine::getMaxCoolantFlowRate, machine::setMaxCoolantFlowRate)
                .setMin(0)
                .setMax(1_000_000));
        return group;
    }
}
