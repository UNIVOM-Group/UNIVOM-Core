package com.mrlacar.univom.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mrlacar.univom.api.data.particle.ParticleDefinition;
import com.mrlacar.univom.api.machine.multiblock.LargeHadronColliderModuleType;
import com.mrlacar.univom.common.data.ModItems;
import com.mrlacar.univom.common.data.ModParticles;
import com.mrlacar.univom.common.machine.multiblock.electric.LargeHadronColliderModule;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FilteredBeamHatchPartMachine extends BeamHatchPartMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            FilteredBeamHatchPartMachine.class, BeamHatchPartMachine.MANAGED_FIELD_HOLDER);

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Getter
    @Setter
    @Persisted
    @DescSynced
    private String particleFilter = "";

    public FilteredBeamHatchPartMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, true);
    }

    @Override
    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        return true;
    }

    private List<ParticleDefinition> getValidParticlesForCurrentModule(LargeHadronColliderModuleType type) {
        List<ParticleDefinition> list = new ArrayList<>();
        if (type == null) return list;
        switch (type) {
            case CMS:
                list.add(ModParticles.PHOTON); list.add(ModParticles.ELECTRON); list.add(ModParticles.MUON); list.add(ModParticles.TAU); break;
            case ALICE:
                list.add(ModParticles.PROTON); list.add(ModParticles.NEUTRON); list.add(ModParticles.ETA); list.add(ModParticles.LAMBDA); list.add(ModParticles.OMEGA); list.add(ModParticles.J_PSI); list.add(ModParticles.UPSILON); break;
            case ATLAS:
                list.add(ModParticles.ELECTRON_NEUTRINO); list.add(ModParticles.MUON_NEUTRINO); list.add(ModParticles.TAU_NEUTRINO); list.add(ModParticles.W_BOSON); list.add(ModParticles.Z_BOSON); break;
            case LHCB:
                list.add(ModParticles.HIGGS_BOSON); list.add(ModParticles.GRAVITON); break;
        }
        return list;
    }

    private ItemStack getParticleItem(ParticleDefinition p) {
        if (ModItems.PARTICLE_MODELS.containsKey(p.name)) {
            return ModItems.PARTICLE_MODELS.get(p.name).asStack();
        }
        return Items.ENDER_PEARL.getDefaultInstance();
    }

    @Override
    public Widget createUIWidget() {
        var controllers = getControllers();
        List<ParticleDefinition> validParticles = new ArrayList<>();

        if (controllers != null && !controllers.isEmpty()) {
            IMultiController controller = controllers.first();
            if (controller instanceof LargeHadronColliderModule module) {
                validParticles = getValidParticlesForCurrentModule(module.getModuleType());
            } else {
                validParticles.addAll(ModParticles.getAllParticles());
            }
        } else {
            validParticles.addAll(ModParticles.getAllParticles());
        }

        int rows = Math.max(1, (validParticles.size() - 1) / 8 + 1);
        int height = 55 + rows * 18;

        WidgetGroup group = new WidgetGroup(0, 0, 176, height);

        group.addWidget(new LabelWidget(10, 10, Component.translatable("univom.gui.filtered_beam_hatch.title")));

        group.addWidget(new LabelWidget(10, 22, () -> {
            if (particleFilter == null || particleFilter.isEmpty()) {
                return Component.translatable("univom.gui.filtered_beam_hatch.current_setting").append(Component.translatable("univom.gui.filtered_beam_hatch.unconfigured")).getString();
            } else {
                ParticleDefinition p = ModParticles.get(particleFilter);
                return Component.translatable("univom.gui.filtered_beam_hatch.current_setting").append(Component.translatable("particle.univom." + p.name)).getString();
            }
        }));

        group.addWidget(new LabelWidget(10, 32, Component.translatable("univom.gui.filtered_beam_hatch.cancel_setting").withStyle(ChatFormatting.GRAY)));

        for (int i = 0; i < validParticles.size(); i++) {
            ParticleDefinition p = validParticles.get(i);
            int col = i % 8;
            int row = i / 8;
            int x = 10 + col * 18;
            int y = 45 + row * 18;

            ItemStack particleItem = getParticleItem(p);
            ButtonWidget btn = new ButtonWidget(x, y, 18, 18,
                    new GuiTextureGroup(GuiTextures.BUTTON, new ItemStackTexture(particleItem)),
                    clickData -> {
                        if (p.name.equals(this.getParticleFilter())) {
                            this.setParticleFilter("");
                        } else {
                            this.setParticleFilter(p.name);
                        }
                    }
            );

            btn.setHoverTooltips(
                    Component.translatable("particle.univom." + p.name).withStyle(ChatFormatting.AQUA),
                    Component.translatable("univom.gui.filtered_beam_hatch.filter_setting").append(p.symbol)
            );

            group.addWidget(btn);
        }

        return group;
    }
}
