package com.mrlacar.univom.api.machine.feature;

import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton;
import com.mrlacar.univom.api.gui.ModGuiTextures;

public interface IInvertibleRedstoneMachine {
    boolean isInverted();

    void setInverted(boolean inverted);

    default void attachInvertedConfigurator(ConfiguratorPanel configuratorPanel) {
        configuratorPanel.attachConfigurators(new IFancyConfiguratorButton.Toggle(
                ModGuiTextures.BUTTON_INVERTIBLE_REDSTONE_MODE.getSubTexture(0, 0, 1, 0.5),
                ModGuiTextures.BUTTON_INVERTIBLE_REDSTONE_MODE.getSubTexture(0, 0.5, 1, 0.5),
                this::isInverted, (clickData, pressed) -> setInverted(pressed)));
    }
}
