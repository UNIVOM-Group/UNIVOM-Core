package com.mrlacar.univom.api.machine.feature;

import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.widget.EnumSelectorWidget;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.FancySelectorConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.IMachineFeature;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.mrlacar.univom.api.gui.ModGuiTextures;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface IEnergyNetworkDisplay extends IMachineFeature {
    default void setDisplayMode(DisplayMode mode) {}

    default DisplayMode getDisplayMode() {
        return DisplayMode.MIXED;
    }

    static void attachConfigurators(ConfiguratorPanel configuratorPanel, IEnergyNetworkDisplay controller) {
        configuratorPanel.attachConfigurators(
                new FancySelectorConfigurator<>(DisplayMode.VALUES, controller.getDisplayMode(), controller::setDisplayMode)
                        .setTooltip(m -> List.of(
                                Component.translatable("univom.gui.energy_matrix_info.title"),
                                Component.translatable(m.getTooltip())
                        ))
        );
    }

    enum DisplayMode implements StringRepresentable, EnumSelectorWidget.SelectableEnum {
        LOCAL("univom.gui.energy_matrix_info.title.local"),
        NETWORK("univom.gui.energy_matrix_info.title.network"),
        MIXED("univom.gui.energy_matrix_info.title.mixed");

        public static final DisplayMode[] VALUES = values();
        private final String localeName;
        @Getter
        private final IGuiTexture icon;

        DisplayMode(String localeName) {
            this.localeName = localeName;
            this.icon = ModGuiTextures.BUTTON_ENERGY_NETWORK.getSubTexture(0, ordinal() * (1.0 / 3.0), 1, (1.0 / 3.0));
        }

        @NotNull
        @Override
        public String getSerializedName() {
            return localeName;
        }

        @NotNull
        @Override
        public String getTooltip() {
            return localeName;
        }
    }
}
