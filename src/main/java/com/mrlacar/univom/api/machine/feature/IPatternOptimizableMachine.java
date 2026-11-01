package com.mrlacar.univom.api.machine.feature;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton;
import com.gregtechceu.gtceu.api.machine.feature.IMachineFeature;
import com.gregtechceu.gtceu.integration.ae2.machine.feature.IGridConnectedMachine;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;

import com.mrlacar.uae2.helpers.IPatternOptimizable;

import net.minecraft.network.chat.Component;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.core.definitions.AEItems;
import appeng.helpers.patternprovider.PatternContainer;

import java.util.List;

public interface IPatternOptimizableMachine extends IMachineFeature, IPatternOptimizable,
                                            PatternContainer, IGridConnectedMachine {

    boolean isPatternOptimizationAllowed();

    void setPatternOptimizationAllowed(boolean allowed);

    @Override
    default boolean uae2$isPatternOptimizationAllowed() {
        return isPatternOptimizationAllowed();
    }

    @Override
    default void uae2$setPatternOptimizationAllowed(boolean allowed) {
        setPatternOptimizationAllowed(allowed);
    }

    @Override
    default InternalInventory uae2$getOptimizablePatternInv() {
        return getTerminalPatternInventory();
    }

    @Override
    default void uae2$notifyPatternsChanged() {
        ICraftingProvider.requestUpdate(getMainNode());
    }

    static void attachConfigurators(ConfiguratorPanel configuratorPanel, IPatternOptimizableMachine machine) {
        configuratorPanel.attachConfigurators(new IFancyConfiguratorButton.Toggle(
                new GuiTextureGroup(GuiTextures.BUTTON,
                        new ItemStackTexture(AEItems.CERTUS_QUARTZ_WRENCH.stack()).scale(16f / 18),
                        new ColorRectTexture(0x80202020)),
                new GuiTextureGroup(GuiTextures.BUTTON,
                        new ItemStackTexture(AEItems.CERTUS_QUARTZ_WRENCH.stack()).scale(16f / 18)),
                machine::isPatternOptimizationAllowed,
                (clickData, pressed) -> machine.setPatternOptimizationAllowed(pressed))
                .setTooltipsSupplier(pressed -> List.of(
                        Component.translatable("gui.uae2.PatternOptimization"),
                        Component.translatable(pressed ? "gui.uae2.Enabled" : "gui.uae2.Disabled"))));
    }
}
