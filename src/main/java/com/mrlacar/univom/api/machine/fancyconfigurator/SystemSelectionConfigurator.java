package com.mrlacar.univom.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.mrlacar.univom.api.gui.ModGuiTextures;
import com.mrlacar.univom.common.machine.multiblock.electric.ProbeRailgun;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.List;

public class SystemSelectionConfigurator implements IFancyUIProvider {
    private final ProbeRailgun machine;
    public static final String[] SYSTEMS = {"solar_system", "proxima_centauri", "ex_68_system"};

    public SystemSelectionConfigurator(ProbeRailgun machine) {
        this.machine = machine;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("univom.gui.system_selection.title");
    }

    @Override
    public IGuiTexture getTabIcon() {
        return ModGuiTextures.SYSTEM_CATEGORY;
    }

    @Override
    public Widget createMainPage(FancyMachineUIWidget widget) {
        var group = new SystemSelectionGroup(0, 0, 140, 20 * SYSTEMS.length + 4);
        group.setBackground(GuiTextures.BACKGROUND_INVERSE);

        for (int i = 0; i < SYSTEMS.length; i++) {
            String sysId = SYSTEMS[i];
            int yOffset = 2 + i * 20;

            ButtonWidget button = new ButtonWidget(2, yOffset, 136, 20, IGuiTexture.EMPTY, cd -> {
                if (cd.isRemote) group.sendSystemSelectAction(sysId);
            });

            IGuiTexture normalTex = new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON,
                    new TextTexture("univom.gui.system_selection." + sysId).setWidth(136).setType(TextTexture.TextType.ROLL));
            IGuiTexture selectedTex = new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON.copy().setColor(ColorPattern.CYAN.color),
                    new TextTexture("univom.gui.system_selection." + sysId).setWidth(136).setType(TextTexture.TextType.ROLL));

            ImageWidget background = new ImageWidget(2, yOffset, 136, 20, () -> sysId.equals(machine.getCurrentSystemSetting()) ? selectedTex : normalTex);

            group.addWidget(button);
            group.addWidget(background);
        }
        return group;
    }

    @Override
    public List<Component> getTabTooltips() {
        return List.of(Component.translatable("univom.gui.system_selection.tab_tooltip"));
    }

    public class SystemSelectionGroup extends WidgetGroup {
        public SystemSelectionGroup(int x, int y, int width, int height) {
            super(x, y, width, height);
        }

        public void sendSystemSelectAction(String system) {
            this.writeClientAction(1, buf -> buf.writeUtf(system));
        }

        @Override
        public void handleClientAction(int id, FriendlyByteBuf buffer) {
            if (id == 1) {
                String sys = buffer.readUtf();
                for (String validSys : SYSTEMS) {
                    if (validSys.equals(sys)) {
                        machine.setCurrentSystemSetting(sys);
                        break;
                    }
                }
            }
        }
    }
}
