package com.mrlacar.univom.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.IRecipeHandlerTrait;
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mrlacar.univom.api.capability.IBeamProvider;
import com.mrlacar.univom.api.capability.recipe.BeamRecipeCapability;
import com.mrlacar.univom.api.data.particle.BeamPacket;
import com.mrlacar.univom.api.machine.trait.NotifiableBeamContainer;
import com.mrlacar.univom.common.machine.multiblock.part.BeamHatchPartMachine;
import com.mrlacar.univom.common.machine.trait.BeamDirectedImpactChamberLogic;
import com.mrlacar.univom.utils.AccumulatedBeamUtils;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BeamDirectedImpactChamber extends WorkableElectricMultiblockMachine {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            BeamDirectedImpactChamber.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    public final Map<String, AccumulatedBeamUtils> internalBeams = new HashMap<>();

    private final Set<IBeamProvider> scratchSeen = new HashSet<>();

    @Persisted
    @DescSynced
    private CompoundTag clientSyncTag = new CompoundTag();
    public boolean needsClientSync = false;

    private TickableSubscription autoPullSubscription;

    @Getter
    private final SynthesizerBeamTrait beamTrait;

    private final Map<String, AccumulatedBeamUtils> cachedDisplayBeams = new HashMap<>();
    private CompoundTag lastSyncTag = null;

    public BeamDirectedImpactChamber(IMachineBlockEntity holder) {
        super(holder);
        this.beamTrait = new SynthesizerBeamTrait(this);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            autoPullSubscription = subscribeServerTick(autoPullSubscription, this::hardwareTick);
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (autoPullSubscription != null) {
            autoPullSubscription.unsubscribe();
            autoPullSubscription = null;
        }
    }

    @Override
    public void saveCustomPersistedData(@NotNull CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);
        ListTag list = new ListTag();
        for (AccumulatedBeamUtils beam : internalBeams.values()) {
            if (beam.getCurrentFlux() > 0) list.add(beam.serializeNBT());
        }
        if (!list.isEmpty()) tag.put("InternalBeams", list);
    }

    @Override
    public void loadCustomPersistedData(@NotNull CompoundTag tag) {
        super.loadCustomPersistedData(tag);
        internalBeams.clear();
        if (tag.contains("InternalBeams")) {
            ListTag list = tag.getList("InternalBeams", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                AccumulatedBeamUtils beam = AccumulatedBeamUtils.deserializeNBT(list.getCompound(i));
                if (beam.getCurrentFlux() > 0) internalBeams.put(beam.getParticleName(), beam);
            }
        }
        updateClientSync();
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new BeamDirectedImpactChamberLogic(this);
    }

    private void hardwareTick() {
        if (!isFormed()) return;

        if (getOffsetTimer() % 5 == 0) {
            suckBeamsFromHatches();
        }

        if (this.needsClientSync && getOffsetTimer() % 20 == 0) {
            updateClientSync();
            this.needsClientSync = false;
        }
    }

    public void suckBeamsFromHatches() {
        boolean changed = false;
        for (IMultiPart part : getParts()) {
            if (part instanceof BeamHatchPartMachine hatch && !hatch.isTransmitter()) {
                NotifiableBeamContainer container = hatch.getBeamContainer();
                scratchSeen.clear();
                BeamPacket available = container.requestBeam(Long.MAX_VALUE, true, scratchSeen);

                if (!available.isEmpty()) {
                    AccumulatedBeamUtils targetBeam = internalBeams.computeIfAbsent(available.particle(), k -> new AccumulatedBeamUtils(available.particle(), 0.0, 0L));
                    long space = AccumulatedBeamUtils.MAX_FLUX - targetBeam.getCurrentFlux();

                    if (space > 0) {
                        scratchSeen.clear();
                        BeamPacket pulled = container.requestBeam(space, false, scratchSeen);
                        if (!pulled.isEmpty()) {
                            targetBeam.merge(pulled.energy(), pulled.flux());
                            changed = true;
                        }
                    }
                }
            }
        }

        if (changed) {
            this.needsClientSync = true;
            this.markDirty();
            this.onChanged();

            var logic = this.getRecipeLogic();
            if (logic != null) {
                logic.markLastRecipeDirty();
                if (!logic.isWorking()) {
                    logic.updateTickSubscription();
                }
            }
        }
    }

    public void updateClientSync() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (AccumulatedBeamUtils beam : internalBeams.values()) {
            if (beam.getCurrentFlux() > 0) list.add(beam.serializeNBT());
        }
        if (!list.isEmpty()) tag.put("InternalBeams", list);
        this.clientSyncTag = tag;
    }

    public Map<String, AccumulatedBeamUtils> getDisplayBeams() {
        if (getLevel() != null && getLevel().isClientSide) {
            if (this.lastSyncTag != this.clientSyncTag) {
                this.cachedDisplayBeams.clear();
                this.lastSyncTag = this.clientSyncTag;
                if (clientSyncTag.contains("InternalBeams")) {
                    ListTag list = clientSyncTag.getList("InternalBeams", Tag.TAG_COMPOUND);
                    for (int i = 0; i < list.size(); i++) {
                        AccumulatedBeamUtils beam = AccumulatedBeamUtils.deserializeNBT(list.getCompound(i));
                        if (beam.getCurrentFlux() > 0) cachedDisplayBeams.put(beam.getParticleName(), beam);
                    }
                }
            }
            return cachedDisplayBeams;
        }
        return internalBeams;
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) return;

        boolean hasContents = false;
        for (AccumulatedBeamUtils beam : getDisplayBeams().values()) {
            if (beam.getCurrentFlux() > 0) {
                hasContents = true;
                textList.add(Component.translatable("univom.multiblock.beam_directed_impact_chamber.storage").withStyle(ChatFormatting.LIGHT_PURPLE)
                        .append(Component.literal(beam.getParticleName() + " ").withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(String.valueOf(beam.getCurrentFlux())).withStyle(ChatFormatting.AQUA)));
            }
        }

        if (!hasContents) textList.add(Component.translatable("univom.multiblock.beam_directed_impact_chamber.empty").withStyle(ChatFormatting.RED));

    }

    public static class SynthesizerBeamTrait extends MachineTrait implements IRecipeHandlerTrait<BeamPacket> {
        public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
                SynthesizerBeamTrait.class);

        @Override
        public ManagedFieldHolder getFieldHolder() {
            return MANAGED_FIELD_HOLDER;
        }

        private final BeamDirectedImpactChamber machine;

        public SynthesizerBeamTrait(BeamDirectedImpactChamber machine) {
            super(machine);
            this.machine = machine;
        }

        @Override
        public ISubscription addChangedListener(Runnable listener) {
            return null;
        }

        @Override
        public IO getHandlerIO() {
            return IO.IN;
        }

        @Override
        public RecipeCapability<BeamPacket> getCapability() {
            return BeamRecipeCapability.CAP;
        }

        @NotNull
        @Override
        public List<Object> getContents() {
            List<Object> contents = new ArrayList<>();
            for (AccumulatedBeamUtils beam : machine.internalBeams.values()) {
                if (beam.getCurrentFlux() > 0) {
                    contents.add(new BeamPacket(beam.getParticleName(), beam.getCurrentEnergy(), beam.getCurrentFlux()));
                }
            }
            return contents;
        }

        @Override
        public double getTotalContentAmount() {
            return machine.internalBeams.values().stream().mapToLong(AccumulatedBeamUtils::getCurrentFlux).sum();
        }

        @Override
        public List<BeamPacket> handleRecipeInner(IO io, GTRecipe recipe, List<BeamPacket> left, boolean simulate) {
            if (io != IO.IN || left == null || left.isEmpty()) {
                return left;
            }
            for (int i = 0; i < left.size(); i++) {
                String particle = left.get(i).particle();
                if (seenBefore(left, particle, i)) {
                    continue;
                }
                long required = 0L;
                for (int j = i; j < left.size(); j++) {
                    if (particle.equals(left.get(j).particle())) {
                        required += left.get(j).flux();
                    }
                }
                AccumulatedBeamUtils beam = machine.internalBeams.get(particle);
                if (beam == null || beam.getCurrentFlux() < required) {
                    return left;
                }
            }
            if (simulate) {
                return null;
            }
            for (BeamPacket request : left) {
                AccumulatedBeamUtils beam = machine.internalBeams.get(request.particle());
                if (beam == null) {
                    continue;
                }
                beam.extractFlux(request.flux());
                if (beam.getCurrentFlux() <= 0) {
                    machine.internalBeams.remove(request.particle());
                }
            }
            machine.needsClientSync = true;
            machine.markDirty();
            return null;
        }

        private static boolean seenBefore(List<BeamPacket> packets, String particle, int index) {
            for (int i = 0; i < index; i++) {
                if (particle.equals(packets.get(i).particle())) {
                    return true;
                }
            }
            return false;
        }
    }
}
