package com.mrlacar.univom.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mrlacar.univom.api.data.particle.BeamPacket;
import com.mrlacar.univom.api.data.particle.ParticleDefinition;
import com.mrlacar.univom.api.machine.fancyconfigurator.BeamVacuumMagneticConfinementRingConfigurator;
import com.mrlacar.univom.api.machine.trait.NotifiableBeamContainer;
import com.mrlacar.univom.common.data.ModParticles;
import com.mrlacar.univom.common.machine.multiblock.part.BeamHatchPartMachine;
import com.mrlacar.univom.common.machine.trait.BeamVacuumMagneticConfinementRingLogic;
import com.mrlacar.univom.utils.AccumulatedBeamUtils;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;
import com.mrlacar.univom.api.capability.IBeamProvider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BeamVacuumMagneticConfinementRing extends WorkableElectricMultiblockMachine {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            BeamVacuumMagneticConfinementRing.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    @Getter
    private final Map<String, AccumulatedBeamUtils> storedBeams = new HashMap<>();
    public final Map<String, Integer> particleThresholds = new HashMap<>();

    @Persisted
    @DescSynced
    private CompoundTag clientSyncTag = new CompoundTag();
    public boolean needsClientSync = false;

    private TickableSubscription autoPullSubscription;

    private final Map<String, AccumulatedBeamUtils> cachedDisplayBeams = new HashMap<>();
    private final Set<IBeamProvider> scratchSeen = new HashSet<>();
    private CompoundTag lastSyncTag = null;

    public BeamVacuumMagneticConfinementRing(IMachineBlockEntity holder) {
        super(holder);
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
    public void saveCustomPersistedData(@org.jetbrains.annotations.NotNull CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);

        ListTag beamsList = new ListTag();
        for (AccumulatedBeamUtils beam : storedBeams.values()) {
            if (beam.getCurrentFlux() > 0) beamsList.add(beam.serializeNBT());
        }
        if (!beamsList.isEmpty()) tag.put("AccumulatedBeams", beamsList);

        CompoundTag thresholdsTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : particleThresholds.entrySet()) {
            thresholdsTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("ParticleThresholds", thresholdsTag);
    }

    @Override
    public void loadCustomPersistedData(@org.jetbrains.annotations.NotNull CompoundTag tag) {
        super.loadCustomPersistedData(tag);

        storedBeams.clear();
        if (tag.contains("AccumulatedBeams")) {
            ListTag beamsList = tag.getList("AccumulatedBeams", Tag.TAG_COMPOUND);
            for (int i = 0; i < beamsList.size(); i++) {
                AccumulatedBeamUtils beam = AccumulatedBeamUtils.deserializeNBT(beamsList.getCompound(i));
                if (beam.getCurrentFlux() > 0) storedBeams.put(beam.getParticleName(), beam);
            }
        }

        particleThresholds.clear();
        if (tag.contains("ParticleThresholds")) {
            CompoundTag thresholdsTag = tag.getCompound("ParticleThresholds");
            for (String key : thresholdsTag.getAllKeys()) {
                particleThresholds.put(key, thresholdsTag.getInt(key));
            }
        }

        updateClientSync();
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new BeamVacuumMagneticConfinementRingLogic(this);
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        super.attachConfigurators(configuratorPanel);
        configuratorPanel.attachConfigurators(new BeamVacuumMagneticConfinementRingConfigurator(this));
    }

    private void hardwareTick() {
        if (!isFormed()) return;

        boolean changed = false;

        if (getOffsetTimer() % 5 == 0) {
            for (IMultiPart part : getParts()) {
                if (part instanceof BeamHatchPartMachine hatch) {
                    NotifiableBeamContainer container = hatch.getBeamContainer();

                    if (!hatch.isTransmitter()) {
                        List<Object> contents = container.getContents();
                        if (contents != null && !contents.isEmpty() && contents.get(0) instanceof BeamPacket packet) {
                            if (packet.flux() > 0) {
                                long inserted = this.insertBeamIntoAccumulator(packet);
                                if (inserted > 0) {
                                    scratchSeen.clear();
                                    container.requestBeam(inserted, false, scratchSeen);
                                    changed = true;
                                }
                            }
                        }
                    } else {
                        for (AccumulatedBeamUtils beam : this.getStoredBeams().values()) {
                            ParticleDefinition def = ModParticles.get(beam.getParticleName());
                            if (def != null && beam.getCurrentFlux() > 0 && beam.getCurrentEnergy() <= def.restMass) {
                                BeamPacket toPush = new BeamPacket(beam.getParticleName(), beam.getCurrentEnergy(), beam.getCurrentFlux());
                                long inserted = container.insertBeam(toPush, false);

                                if (inserted > 0) {
                                    beam.extractFlux(inserted);
                                    changed = true;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (changed) {
            this.needsClientSync = true;
            this.markDirty();
        }

        if (this.needsClientSync && getOffsetTimer() % 20 == 0) {
            updateClientSync();
            this.needsClientSync = false;
        }
    }

    public int getParticleThreshold(String name) {
        if (getLevel() != null && getLevel().isClientSide) {
            if (clientSyncTag.contains("ParticleThresholds")) {
                CompoundTag thresholdsTag = clientSyncTag.getCompound("ParticleThresholds");
                if (thresholdsTag.contains(name)) {
                    return thresholdsTag.getInt(name);
                }
            }
            return 100;
        }
        return particleThresholds.getOrDefault(name, 100);
    }

    public void setParticleThreshold(String name, int threshold) {
        if (threshold < 0) threshold = 0;
        if (threshold > 100) threshold = 100;
        particleThresholds.put(name, threshold);

        AccumulatedBeamUtils beam = storedBeams.get(name);
        if (beam != null) {
            long maxAllowed = (long) (AccumulatedBeamUtils.MAX_FLUX * (threshold / 100.0));
            if (beam.getCurrentFlux() > maxAllowed) {
                long excess = beam.getCurrentFlux() - maxAllowed;
                beam.extractFlux(excess);
            }
        }

        this.needsClientSync = true;
        markDirty();
    }

    public void updateClientSync() {
        CompoundTag tag = new CompoundTag();
        ListTag beamsList = new ListTag();
        for (AccumulatedBeamUtils beam : storedBeams.values()) {
            if (beam.getCurrentFlux() > 0) {
                beamsList.add(beam.serializeNBT());
            }
        }
        if (!beamsList.isEmpty()) {
            tag.put("AccumulatedBeams", beamsList);
        }

        CompoundTag thresholdsTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : particleThresholds.entrySet()) {
            thresholdsTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("ParticleThresholds", thresholdsTag);

        this.clientSyncTag = tag;
    }

    public Map<String, AccumulatedBeamUtils> getDisplayBeams() {
        if (getLevel() != null && getLevel().isClientSide) {
            if (this.lastSyncTag != this.clientSyncTag) {
                this.cachedDisplayBeams.clear();
                this.lastSyncTag = this.clientSyncTag;
                if (clientSyncTag.contains("AccumulatedBeams")) {
                    ListTag list = clientSyncTag.getList("AccumulatedBeams", Tag.TAG_COMPOUND);
                    for (int i = 0; i < list.size(); i++) {
                        AccumulatedBeamUtils beam = AccumulatedBeamUtils.deserializeNBT(list.getCompound(i));
                        if (beam.getCurrentFlux() > 0) cachedDisplayBeams.put(beam.getParticleName(), beam);
                    }
                }
            }
            return cachedDisplayBeams;
        }
        return storedBeams;
    }

    public long insertBeamIntoAccumulator(BeamPacket packet) {
        if (packet == null || packet.isEmpty()) return 0;
        String pName = packet.particle();

        int thresholdPercent = particleThresholds.getOrDefault(pName, 100);
        long maxAllowed = (long) (AccumulatedBeamUtils.MAX_FLUX * (thresholdPercent / 100.0));

        AccumulatedBeamUtils beam = storedBeams.computeIfAbsent(pName, k -> new AccumulatedBeamUtils(pName, 0.0, 0L));

        long current = beam.getCurrentFlux();
        long spaceToMax = maxAllowed - current;

        if (spaceToMax <= 0) {
            return packet.flux();
        }

        long toStore = Math.min(spaceToMax, packet.flux());
        long inserted = beam.merge(packet.energy(), toStore);

        if (inserted > 0 || packet.flux() > toStore) {
            this.needsClientSync = true;
            markDirty();
        }

        return packet.flux();
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        if (!isFormed()) return;

        if (!isWorkingEnabled() || !getRecipeLogic().isWorking()) {
            textList.add(Component.translatable("univom.multiblock.beam_vacuum_magnetic_confinement_ring.cooling_paused").withStyle(ChatFormatting.YELLOW));
        } else {
            textList.add(Component.translatable("univom.multiblock.beam_vacuum_magnetic_confinement_ring.cooling_active").withStyle(ChatFormatting.GREEN));
        }

        boolean hasBeams = false;
        long baseLHeDemand = 2000;
        long extraCoolingDemand = 0;

        for (AccumulatedBeamUtils beam : getDisplayBeams().values()) {
            if (beam.getCurrentFlux() > 0) {
                hasBeams = true;
                ParticleDefinition def = ModParticles.get(beam.getParticleName());
                if (def != null) {
                    extraCoolingDemand += beam.calculateCoolingDemand(def.restMass);
                }

                String energyStr = String.format("%.2f MeV", beam.getCurrentEnergy());
                textList.add(Component.literal(" ▸ ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(beam.getParticleName() + ": ").withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(beam.getCurrentFlux() + " | ").withStyle(ChatFormatting.LIGHT_PURPLE))
                        .append(Component.literal(energyStr).withStyle(ChatFormatting.GREEN)));
            }
        }

        long totalLHeDemand = baseLHeDemand + extraCoolingDemand;

        textList.add(Component.translatable("univom.multiblock.beam_vacuum_magnetic_confinement_ring.liquid_helium_consumption").withStyle(ChatFormatting.AQUA)
                .append(Component.translatable(totalLHeDemand + " mB/s").withStyle(ChatFormatting.WHITE)));

        if (!hasBeams) {
            textList.add(Component.translatable("univom.multiblock.beam_vacuum_magnetic_confinement_ring.empty").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
