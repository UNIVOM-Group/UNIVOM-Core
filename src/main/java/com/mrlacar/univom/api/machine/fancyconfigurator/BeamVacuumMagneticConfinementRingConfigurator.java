package com.mrlacar.univom.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.mrlacar.univom.api.data.particle.ParticleDefinition;
import com.mrlacar.univom.api.gui.ModGuiTextures;
import com.mrlacar.univom.common.data.ModItems;
import com.mrlacar.univom.common.data.ModParticles;
import com.mrlacar.univom.common.machine.multiblock.electric.BeamVacuumMagneticConfinementRing;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BeamVacuumMagneticConfinementRingConfigurator implements IFancyConfigurator {
    private final BeamVacuumMagneticConfinementRing machine;

    public BeamVacuumMagneticConfinementRingConfigurator(BeamVacuumMagneticConfinementRing machine) {
        this.machine = machine;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("univom.gui.beam_vacuum_configurator.title");
    }

    @Override
    public IGuiTexture getIcon() {
        return ModGuiTextures.BUTTON_COLLIDER_PREDICTION;
    }

    private ItemStack getParticleItem(ParticleDefinition p) {
        if (ModItems.PARTICLE_MODELS.containsKey(p.name)) {
            return ModItems.PARTICLE_MODELS.get(p.name).asStack();
        }
        return Items.ENDER_PEARL.getDefaultInstance();
    }

    @Override
    public Widget createConfigurator() {
        WidgetGroup group = new WidgetGroup(0, 0, 220, 160);

        group.addWidget(new LabelWidget(4, 4, Component.translatable("univom.gui.beam_vacuum_magnetic_confinement_ring.void_threshold")));
        group.addWidget(new LabelWidget(4, 16, Component.translatable("univom.gui.beam_vacuum_magnetic_confinement_ring.void_setting")));

        DraggableScrollableWidgetGroup scroll = new DraggableScrollableWidgetGroup(2, 30, 216, 126);
        scroll.setYScrollBarWidth(4).setYBarStyle(GuiTextures.BACKGROUND_INVERSE, GuiTextures.BUTTON);

        int yOffset = 0;
        for (ParticleDefinition p : ModParticles.getAllParticles()) {
            ButtonWidget btn = new ButtonWidget(4, yOffset, 18, 18,
                    new GuiTextureGroup(GuiTextures.BUTTON, new ItemStackTexture(getParticleItem(p))),
                    cd -> {}
            );
            btn.setHoverTooltips(Component.translatable("particle.univom." + p.name).withStyle(ChatFormatting.YELLOW));
            scroll.addWidget(btn);

            scroll.addWidget(new LabelWidget(26, yOffset + 5, Component.translatable("particle.univom." + p.name)));

            scroll.addWidget(new TextFieldWidget(140, yOffset + 2, 60, 14,
                    () -> String.valueOf(machine.getParticleThreshold(p.name)),
                    val -> {
                        String text = val.trim();
                        if (text.isEmpty()) {
                            machine.setParticleThreshold(p.name, 0);
                            return;
                        }
                        try {
                            int parsed = Integer.parseInt(text);
                            machine.setParticleThreshold(p.name, parsed);
                        } catch (NumberFormatException ignored) {}
                    }));

            yOffset += 20;
        }

        group.addWidget(scroll);
        return group;
    }
}
