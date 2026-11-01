package com.mrlacar.univom.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mrlacar.univom.api.data.particle.BeamPacket;
import com.mrlacar.univom.api.machine.multiblock.LargeHadronColliderModuleType;
import com.mrlacar.univom.api.machine.trait.NotifiableBeamContainer;
import com.mrlacar.univom.common.machine.multiblock.part.BeamHatchPartMachine;
import com.mrlacar.univom.common.machine.multiblock.part.FilteredBeamHatchPartMachine;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class LargeHadronColliderModule extends WorkableElectricMultiblockMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            LargeHadronColliderModule.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    @DescSynced
    @Getter
    private @Nullable BlockPos boundHostPos = null;

    @Persisted
    @DescSynced
    @Getter
    private boolean isDuplicate = false;

    public LargeHadronColliderModule(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (!isRemote() && this.boundHostPos != null) {
            MetaMachine hostMachine = MetaMachine.getMachine(getLevel(), this.boundHostPos);
            if (hostMachine instanceof LargeHadronCollider lhc) {
                lhc.forceUnbindModule(getPos());
            }
        }
    }

    @Override
    public boolean isRecipeLogicAvailable() {
        return super.isRecipeLogicAvailable() && this.boundHostPos != null && !this.isDuplicate;
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        if (isFormed()) {
            if (this.isDuplicate) {
                textList.add(Component.translatable("univom.multiblock.large_hadron_collider_module.same_bound").withStyle(ChatFormatting.RED));
            }
            else if (this.boundHostPos != null) {
                MetaMachine hostMachine = MetaMachine.getMachine(getLevel(), this.boundHostPos);
                if (hostMachine instanceof LargeHadronCollider lhc) {
                    if (lhc.isFormed()) {
                        textList.add(Component.translatable("univom.multiblock.large_hadron_collider_module.bound",
                                        boundHostPos.getX(), boundHostPos.getY(), boundHostPos.getZ())
                                .withStyle(ChatFormatting.GREEN));
                    } else {
                        textList.add(Component.translatable("univom.multiblock.large_hadron_collider_module.host_invalid")
                                .withStyle(ChatFormatting.YELLOW));
                    }
                } else {
                    textList.add(Component.translatable("univom.multiblock.large_hadron_collider_module.unbound")
                            .withStyle(ChatFormatting.RED));
                }
            }
            else {
                textList.add(Component.translatable("univom.multiblock.large_hadron_collider_module.unbound")
                        .withStyle(ChatFormatting.RED));
            }
        }
    }

    public abstract LargeHadronColliderModuleType getModuleType();

    public void bindToHost(BlockPos hostPos) {
        this.boundHostPos = hostPos;
        markDirty();
    }

    public void unbindFromHost() {
        this.boundHostPos = null;
        markDirty();
    }

    public void setDuplicate(boolean isDuplicate) {
        if (this.isDuplicate != isDuplicate) {
            this.isDuplicate = isDuplicate;
            markDirty();
        }
    }

    public void injectBeamFromLHC(BeamPacket packet) {
        if (packet == null || packet.flux() <= 0) return;

        long remainingFlux = packet.flux();

        for (IMultiPart part : this.getParts()) {
            if (part instanceof BeamHatchPartMachine beamHatch && beamHatch.isTransmitter()) {

                if (beamHatch instanceof FilteredBeamHatchPartMachine filteredHatch) {
                    String filter = filteredHatch.getParticleFilter();
                    if (!filter.isEmpty() && !filter.equals(packet.particle())) {
                        continue;
                    }
                }

                NotifiableBeamContainer container = beamHatch.getBeamContainer();
                BeamPacket toInsert = new BeamPacket(packet.particle(), packet.energy(), remainingFlux);

                long insertedFlux = container.insertBeam(toInsert, false);
                remainingFlux -= insertedFlux;

                if (remainingFlux <= 0) {
                    break;
                }
            }
        }
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {}
}
