package com.mrlacar.univom.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mrlacar.univom.api.data.particle.BeamPacket;
import com.mrlacar.univom.api.machine.fancyconfigurator.SynchrotronConfigurator;
import com.mrlacar.univom.api.machine.trait.NotifiableBeamContainer;
import com.mrlacar.univom.common.machine.multiblock.part.BeamHatchPartMachine;
import com.mrlacar.univom.common.machine.trait.SynchrotronLogic;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Synchrotron extends WorkableElectricMultiblockMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            Synchrotron.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Getter
    @Setter
    @Persisted
    @DescSynced
    private long targetEUt = 122880L;

    @Getter @Setter
    @Persisted @DescSynced
    private int targetDuration = 600;

    @Getter @Setter
    @Persisted @DescSynced
    private long targetFlux = 800L;

    public Synchrotron(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new SynchrotronLogic(this);
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        configuratorPanel.attachConfigurators(new SynchrotronConfigurator(this));
        super.attachConfigurators(configuratorPanel);
    }

    @Override
    public void addDisplayText(@NotNull List<Component> textList) {
        super.addDisplayText(textList);
        if (!this.isFormed()) return;

        for (var part : getParts()) {
            if (part instanceof BeamHatchPartMachine beamHatch && !beamHatch.isTransmitter()) {
                NotifiableBeamContainer container = beamHatch.getBeamContainer();
                List<Object> contents = container.getContents();

                if (contents.isEmpty()) {
                    textList.add(Component.translatable("univom.multiblock.synchrotron.energy", "0.00 MeV").withStyle(ChatFormatting.GREEN));
                    textList.add(Component.translatable("univom.multiblock.synchrotron.flux", 0).withStyle(ChatFormatting.LIGHT_PURPLE));
                } else {
                    if (contents.get(0) instanceof BeamPacket packet) {
                        double e = packet.energy();
                        String energyStr;
                        if (e >= 1000000.0) {
                            energyStr = String.format("%.2f TeV", e / 1000000.0);
                        } else if (e >= 1000.0) {
                            energyStr = String.format("%.2f GeV", e / 1000.0);
                        } else {
                            energyStr = String.format("%.2f MeV", e);
                        }

                        textList.add(Component.translatable("univom.multiblock.synchrotron.energy", energyStr).withStyle(ChatFormatting.GREEN));
                        textList.add(Component.translatable("univom.multiblock.synchrotron.flux", packet.flux()).withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                }
            }
        }
        textList.add(Component.empty());
        textList.add(Component.translatable("univom.multiblock.synchrotron.config_eut", targetEUt));
        textList.add(Component.translatable("univom.multiblock.synchrotron.config_duration", targetDuration / 20));
        textList.add(Component.translatable("univom.multiblock.synchrotron.config_flux", targetFlux));
    }
}
