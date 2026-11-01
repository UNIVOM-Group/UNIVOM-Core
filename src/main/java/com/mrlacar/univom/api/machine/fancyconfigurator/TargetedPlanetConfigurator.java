package com.mrlacar.univom.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.mrlacar.univom.api.gui.ModGuiTextures;
import com.mrlacar.univom.common.machine.multiblock.electric.ProbeRailgun;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.List;

public class TargetedPlanetConfigurator implements IFancyUIProvider {
    private final ProbeRailgun machine;

    public TargetedPlanetConfigurator(ProbeRailgun machine) {
        this.machine = machine;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("univom.gui.planet_selection.title");
    }

    @Override
    public IGuiTexture getTabIcon() {
        return ModGuiTextures.PLANET_AU_CATEGORY;
    }

    @Override
    public Widget createMainPage(FancyMachineUIWidget widget) {
        var group = new CoordinateInputGroup(0, 0, 140, 50);
        group.setBackground(GuiTextures.BACKGROUND_INVERSE);

        LabelWidget hintLabel = new LabelWidget(5, 5, Component.translatable("univom.gui.planet_selection.input_au"));
        group.addWidget(hintLabel);

        TextFieldWidget auInput = new TextFieldWidget(5, 20, 95, 16, null, null);
        auInput.setCurrentString(machine.getCurrentAUInput());

        auInput.setValidator(text -> text.replaceAll("[^0-9.]", ""));
        auInput.setActive(!machine.isDeepSpaceMode());
        auInput.setMaxStringLength(8);
        group.addWidget(auInput);

        IGuiTexture lockTex = new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("✔"));
        ButtonWidget lockButton = new ButtonWidget(105, 20, 30, 16, lockTex, cd -> {
            if (cd.isRemote) group.sendAUInput(auInput.getCurrentString());
        });
        lockButton.setActive(!machine.isDeepSpaceMode());
        group.addWidget(lockButton);

        return group;
    }

    @Override
    public List<Component> getTabTooltips() {
        return List.of(Component.translatable("univom.gui.planet_selection.tab_tooltip"));
    }

    public class CoordinateInputGroup extends WidgetGroup {
        public CoordinateInputGroup(int x, int y, int width, int height) {
            super(x, y, width, height);
        }

        public void sendAUInput(String auText) {
            this.writeClientAction(101, buf -> buf.writeUtf(auText));
        }

        @Override
        public void handleClientAction(int id, FriendlyByteBuf buffer) {
            if (id == 101) {
                machine.processAUInput(buffer.readUtf());
            }
        }
    }
}
