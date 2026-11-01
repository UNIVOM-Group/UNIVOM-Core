package com.mrlacar.univom.api.machine.fancyconfigurator;

import appeng.core.definitions.AEBlocks;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.mrlacar.univom.api.machine.feature.IAEWirelessMachine;
import net.minecraft.network.chat.Component;

import java.util.List;

public class WirelessNetworkSelectionConfigurator implements IFancyUIProvider {
    private final IAEWirelessMachine machine;

    public WirelessNetworkSelectionConfigurator(IAEWirelessMachine machine) {
        this.machine = machine;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("univom.gui.wireless_setting.title");
    }

    @Override
    public IGuiTexture getTabIcon() {
        return new ItemStackTexture(AEBlocks.CONTROLLER.stack());
    }

    @Override
    public Widget createMainPage(FancyMachineUIWidget widget) {
        WidgetGroup group = new WidgetGroup(0, 0, 140, 60);
        group.setBackground(GuiTextures.BACKGROUND_INVERSE);

        group.addWidget(new LabelWidget(9, 8, Component.translatable("univom.gui.target_channel")));

        group.addWidget(new TextFieldWidget(9, 22, 122, 16,
                machine::getTargetChannel,
                machine::setTargetChannel));

        return group;
    }

    @Override
    public List<Component> getTabTooltips() {
        return List.of(Component.translatable("univom.gui.wireless_setting.tab_tooltip"));
    }
}
