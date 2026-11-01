package com.mrlacar.univom.api.machine.fancyconfigurator;

import appeng.core.definitions.AEItems;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.mrlacar.univom.integration.ae2.machine.WirelessMEPatternBufferPartMachine;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class PatternModifierConfigurator implements IFancyConfigurator {
    private final WirelessMEPatternBufferPartMachine machine;

    public PatternModifierConfigurator(WirelessMEPatternBufferPartMachine machine) {
        this.machine = machine;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("univom.gui.pattern_modifier.title");
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(AEItems.BLANK_PATTERN.stack());
    }

    @Override
    public Widget createConfigurator() {
        PatternModifierGroup group = new PatternModifierGroup(0, 0, 104, 42);

        int[] factors = { 2, 4, 8, 16 };
        for (int i = 0; i < 4; i++) {
            int factor = factors[i];

            ButtonWidget mulBtn = new ButtonWidget(2 + i * 20, 2, 18, 18,
                    new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("x" + factor)), clickData -> {
                if (group.getGui() != null && group.getGui().holder.isRemote()) {
                    group.sendClientAction(777, buf -> {
                        buf.writeInt(factor);
                        buf.writeBoolean(false);
                    });
                }
            });
            mulBtn.setHoverTooltips(Component.translatable("univom.gui.pattern_modifier.multiply", factor));
            group.addWidget(mulBtn);

            ButtonWidget divBtn = new ButtonWidget(2 + i * 20, 22, 18, 18,
                    new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("÷" + factor)), clickData -> {
                if (group.getGui() != null && group.getGui().holder.isRemote()) {
                    group.sendClientAction(777, buf -> {
                        buf.writeInt(factor);
                        buf.writeBoolean(true);
                    });
                }
            });
            divBtn.setHoverTooltips(Component.translatable("univom.gui.pattern_modifier.divide", factor));
            group.addWidget(divBtn);
        }

        ButtonWidget uploadBtn = new ButtonWidget(82, 2, 18, 18,
                new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("⬆")), clickData -> {
            if (group.getGui() != null && group.getGui().holder.isRemote()) {
                group.sendClientAction(778, buf -> {});
            }
        });
        uploadBtn.setHoverTooltips(Component.translatable("univom.gui.pattern_modifier.upload"));
        group.addWidget(uploadBtn);

        ButtonWidget downloadBtn = new ButtonWidget(82, 22, 18, 18,
                new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("⬇")), clickData -> {
            if (group.getGui() != null && group.getGui().holder.isRemote()) {
                group.sendClientAction(779, buf -> {});
            }
        });
        downloadBtn.setHoverTooltips(Component.translatable("univom.gui.pattern_modifier.download"));
        group.addWidget(downloadBtn);

        return group;
    }

    private class PatternModifierGroup extends WidgetGroup {

        public PatternModifierGroup(int x, int y, int width, int height) {
            super(x, y, width, height);
        }

        public void sendClientAction(int id, Consumer<FriendlyByteBuf> dataWriter) {
            this.writeClientAction(id, dataWriter);
        }

        @Override
        public void handleClientAction(int id, FriendlyByteBuf buffer) {
            if (id == 777) {
                int factor = buffer.readInt();
                boolean divide = buffer.readBoolean();
                machine.modifyPatterns(factor, divide);
            } else if (id == 778) {
                if (getGui() != null && getGui().entityPlayer != null) {
                    machine.uploadPatterns(getGui().entityPlayer);
                }
            } else if (id == 779) {
                if (getGui() != null && getGui().entityPlayer != null) {
                    machine.downloadPatterns(getGui().entityPlayer);
                }
            } else {
                super.handleClientAction(id, buffer);
            }
        }
    }
}
