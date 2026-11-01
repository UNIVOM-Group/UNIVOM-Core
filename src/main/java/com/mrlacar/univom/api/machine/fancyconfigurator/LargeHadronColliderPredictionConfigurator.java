package com.mrlacar.univom.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.mrlacar.univom.api.data.particle.ParticleDefinition;
import com.mrlacar.univom.api.gui.ModGuiTextures;
import com.mrlacar.univom.api.machine.multiblock.LargeHadronColliderModuleType;
import com.mrlacar.univom.common.data.ModItems;
import com.mrlacar.univom.common.data.ModParticles;
import com.mrlacar.univom.common.machine.multiblock.electric.LargeHadronCollider;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class LargeHadronColliderPredictionConfigurator implements IFancyConfigurator {
    @Getter
    private final LargeHadronCollider machine;

    private String simulatedEnergyInput;
    private double simulatedCollisionEnergy;
    private final Map<ParticleDefinition, Double> cachedProbabilities = new HashMap<>();
    private final DecimalFormat df = new DecimalFormat("0.00");

    private enum CollisionMode {
        DIRTY,
        PURE,
        DIS
    }

    private CollisionMode currentMode = CollisionMode.DIRTY;

    public LargeHadronColliderPredictionConfigurator(LargeHadronCollider machine) {
        this.machine = machine;
        double e1 = machine.getTargetEnergy1();
        double e2 = machine.getTargetEnergy2();
        this.simulatedCollisionEnergy = 2.0 * Math.sqrt(e1 * e2);

        if (Double.isNaN(this.simulatedCollisionEnergy) || this.simulatedCollisionEnergy < 0) {
            this.simulatedCollisionEnergy = 0.0;
        }

        this.simulatedEnergyInput = String.format("%.0f", this.simulatedCollisionEnergy);
        updateProbabilities();
    }

    private void updateProbabilities() {
        try {
            simulatedCollisionEnergy = Double.parseDouble(simulatedEnergyInput);
        } catch (NumberFormatException e) {
            simulatedCollisionEnergy = 0.0;
        }

        cachedProbabilities.clear();
        double totalWeight = 0.0;
        Map<ParticleDefinition, Double> weights = new HashMap<>();

        for (ParticleDefinition p : ModParticles.getAllParticles()) {
            if (p.lhcWeight <= 0) continue;

            if (currentMode == CollisionMode.PURE && "STRONG".equals(p.forceType)) continue;
            if (currentMode == CollisionMode.DIS &&
                    (p.name.equals("higgs_boson") || p.name.equals("graviton") ||
                            p.name.equals("w_boson") || p.name.equals("z_boson"))) continue;

            double effectiveMass = p.restMass;
            if (p.name.equals("photon")) effectiveMass = 0.0;
            else if (p.name.contains("neutrino")) effectiveMass = 10.0;
            else if (p.name.equals("graviton")) effectiveMass = 5000000.0;

            if (simulatedCollisionEnergy >= effectiveMass) {
                double massFactor = Math.max(1.0, effectiveMass);
                double overEnergy = simulatedCollisionEnergy / massFactor;
                double scaling = Math.sqrt(massFactor) * Math.log10(1.0 + overEnergy);
                double dynWeight = p.lhcWeight * (1.0 + scaling);

                weights.put(p, dynWeight);
                totalWeight += dynWeight;
            }
        }

        if (totalWeight > 0) {
            for (Map.Entry<ParticleDefinition, Double> entry : weights.entrySet()) {
                cachedProbabilities.put(entry.getKey(), entry.getValue() / totalWeight);
            }
        }
    }

    @Override
    public Component getTitle() {
        return Component.translatable("univom.gui.large_hadron_collider_prediction.title");
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

    private LargeHadronColliderModuleType getTargetModule(ParticleDefinition particle) {
        if (particle == ModParticles.PHOTON || particle == ModParticles.ELECTRON ||
                particle == ModParticles.MUON || particle == ModParticles.TAU) {
            return LargeHadronColliderModuleType.CMS;
        }
        if (particle == ModParticles.ELECTRON_NEUTRINO || particle == ModParticles.MUON_NEUTRINO ||
                particle == ModParticles.TAU_NEUTRINO || particle == ModParticles.W_BOSON || particle == ModParticles.Z_BOSON) {
            return LargeHadronColliderModuleType.ATLAS;
        }
        if (particle == ModParticles.HIGGS_BOSON || particle == ModParticles.GRAVITON) {
            return LargeHadronColliderModuleType.LHCB;
        }
        return LargeHadronColliderModuleType.ALICE;
    }

    @Override
    public Widget createConfigurator() {
        WidgetGroup group = new WidgetGroup(0, 0, 220, 160);

        group.addWidget(new LabelWidget(5, 6, Component.translatable("univom.gui.large_hadron_collider_prediction.collision_energy")));
        group.addWidget(new TextFieldWidget(66, 4, 145, 14,
                () -> simulatedEnergyInput,
                val -> {
                    simulatedEnergyInput = val.trim();
                    updateProbabilities();
                }));

        group.addWidget(new ButtonWidget(8, 20, 64, 14,
                new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture(() -> currentMode == CollisionMode.DIRTY ? ChatFormatting.GREEN + "p-p" : "p-p")),
                cd -> { currentMode = CollisionMode.DIRTY; updateProbabilities(); }));

        group.addWidget(new ButtonWidget(78, 20, 64, 14,
                new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture(() -> currentMode == CollisionMode.PURE ? ChatFormatting.GREEN + "e-e" : "e-e")),
                cd -> { currentMode = CollisionMode.PURE; updateProbabilities(); }));

        group.addWidget(new ButtonWidget(148, 20, 64, 14,
                new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture(() -> currentMode == CollisionMode.DIS ? ChatFormatting.GREEN + "e-p" : "e-p")),
                cd -> { currentMode = CollisionMode.DIS; updateProbabilities(); }));

        int colCMS = 35;
        int colATLAS = 82;
        int colALICE = 132;
        int colLHCB = 180;

        group.addWidget(new LabelWidget(colCMS, 38, ChatFormatting.GRAY + "CMS"));
        group.addWidget(new LabelWidget(colATLAS, 38, ChatFormatting.GRAY + "ATLAS"));
        group.addWidget(new LabelWidget(colALICE, 38, ChatFormatting.GRAY + "ALICE"));
        group.addWidget(new LabelWidget(colLHCB, 38, ChatFormatting.GRAY + "LHCb"));

        DraggableScrollableWidgetGroup scroll = new DraggableScrollableWidgetGroup(2, 50, 216, 106);
        scroll.setYScrollBarWidth(4).setYBarStyle(GuiTextures.BACKGROUND_INVERSE, GuiTextures.BUTTON);

        int yOffset = 0;
        for (ParticleDefinition p : ModParticles.getAllParticles()) {
            if (p.lhcWeight <= 0) continue;

            ButtonWidget btn = new ButtonWidget(4, yOffset, 18, 18,
                    new GuiTextureGroup(GuiTextures.BUTTON, new ItemStackTexture(getParticleItem(p))),
                    cd -> {}
            );
            btn.setHoverTooltips(Component.translatable("particle.univom." + p.name).withStyle(ChatFormatting.YELLOW));
            scroll.addWidget(btn);

            LargeHadronColliderModuleType route = getTargetModule(p);

            scroll.addWidget(new LabelWidget(colCMS, yOffset + 5, () -> {
                if (route != LargeHadronColliderModuleType.CMS) return ChatFormatting.DARK_GRAY + "--";
                double prob = cachedProbabilities.getOrDefault(p, 0.0);
                return ChatFormatting.WHITE + (prob > 0 ? df.format(prob * 100) + "%%" : "0.00%%");
            }));

            scroll.addWidget(new LabelWidget(colATLAS, yOffset + 5, () -> {
                if (route != LargeHadronColliderModuleType.ATLAS) return ChatFormatting.DARK_GRAY + "--";
                double prob = cachedProbabilities.getOrDefault(p, 0.0);
                return ChatFormatting.WHITE + (prob > 0 ? df.format(prob * 100) + "%%" : "0.00%%");
            }));

            scroll.addWidget(new LabelWidget(colALICE, yOffset + 5, () -> {
                if (route != LargeHadronColliderModuleType.ALICE) return ChatFormatting.DARK_GRAY + "--";
                double prob = cachedProbabilities.getOrDefault(p, 0.0);
                return ChatFormatting.WHITE + (prob > 0 ? df.format(prob * 100) + "%%" : "0.00%%");
            }));

            scroll.addWidget(new LabelWidget(colLHCB, yOffset + 5, () -> {
                if (route != LargeHadronColliderModuleType.LHCB) return ChatFormatting.DARK_GRAY + "--";
                double prob = cachedProbabilities.getOrDefault(p, 0.0);
                return ChatFormatting.WHITE + (prob > 0 ? df.format(prob * 100) + "%%" : "0.00%%");
            }));

            yOffset += 20;
        }

        group.addWidget(scroll);
        return group;
    }
}
