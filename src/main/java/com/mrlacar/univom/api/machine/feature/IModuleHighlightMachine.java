package com.mrlacar.univom.api.machine.feature;

import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.ButtonConfigurator;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import net.minecraft.network.chat.Component;

import java.util.List;

public interface IModuleHighlightMachine {
    void triggerHighlight(ClickData clickData);

    default void attachHighlightConfigurator(ConfiguratorPanel configuratorPanel) {
        configuratorPanel.attachConfigurators(new ButtonConfigurator(
                new GuiTextureGroup(new ItemStackTexture(GTMultiMachines.ASSEMBLY_LINE.asStack())),
                this::triggerHighlight)
                .setTooltips(List.of(Component.translatable("behaviour.machine.highlight_modules"))));
    }
}
