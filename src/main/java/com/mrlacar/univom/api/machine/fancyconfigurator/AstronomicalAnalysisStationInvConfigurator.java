package com.mrlacar.univom.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator;
import com.gregtechceu.gtceu.api.gui.widget.BlockableSlotWidget;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.mrlacar.univom.common.data.ModItems;
import com.mrlacar.univom.common.machine.multiblock.electric.AstronomicalAnalysisStation;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;

public class AstronomicalAnalysisStationInvConfigurator implements IFancyConfigurator {
    private final CustomItemStackHandler inventory;
    private final AstronomicalAnalysisStation station;

    @Getter
    private final Component title;

    @Getter
    @Setter
    private List<Component> tooltips = Collections.emptyList();

    public AstronomicalAnalysisStationInvConfigurator(CustomItemStackHandler inventory, Component title, AstronomicalAnalysisStation station) {
        this.inventory = inventory;
        this.title = title;
        this.station = station;
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(ModItems.PLANET_INFO_CARD.get());
    }

    @Override
    public Widget createConfigurator() {
        WidgetGroup group = new WidgetGroup(0, 0, 18 + 16, 18 + 16);
        WidgetGroup container = new WidgetGroup(4, 4, 18 + 8, 18 + 8);

        container.addWidget(new BlockableSlotWidget(this.inventory, 0, 4, 4, true, true)
                .setIsBlocked(station::isLocked)
                .setBackgroundTexture(GuiTextures.SLOT)
                .setIngredientIO(IngredientIO.INPUT));

        container.setBackground(GuiTextures.BACKGROUND_INVERSE);
        group.addWidget(container);
        return group;
    }
}
