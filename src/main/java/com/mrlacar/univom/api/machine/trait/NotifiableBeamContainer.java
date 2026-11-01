package com.mrlacar.univom.api.machine.trait;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableRecipeHandlerTrait;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mrlacar.univom.api.capability.IBeamProvider;
import com.mrlacar.univom.api.capability.IBeamReceiver;
import com.mrlacar.univom.api.capability.recipe.BeamRecipeCapability;
import com.mrlacar.univom.api.data.particle.BeamPacket;
import com.mrlacar.univom.common.machine.multiblock.part.BeamHatchPartMachine;
import com.mrlacar.univom.common.machine.multiblock.part.FilteredBeamHatchPartMachine;
import com.mrlacar.univom.common.pipelike.beam.BeamCapabilityHelper;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class NotifiableBeamContainer extends NotifiableRecipeHandlerTrait<BeamPacket> implements IBeamReceiver, IBeamProvider {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            NotifiableBeamContainer.class, NotifiableRecipeHandlerTrait.MANAGED_FIELD_HOLDER);

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    protected final IO handlerIO;
    @Getter
    protected final boolean transmitter;

    @Persisted
    @DescSynced
    private String currentParticleName = "";

    @Persisted @DescSynced
    private double currentEnergy = 0.0;

    @Persisted @DescSynced
    private long currentFlux = 0L;

    private final long capacity = 1000000L;
    private TickableSubscription subscription;
    private final Set<IBeamProvider> scratchSeen = new HashSet<>();

    @Getter
    private final LazyOptional<IBeamProvider> providerCapability = LazyOptional.of(() -> this);

    public NotifiableBeamContainer(MetaMachine machine, IO handlerIO, boolean transmitter) {
        super(machine);
        this.handlerIO = handlerIO;
        this.transmitter = transmitter;
    }

    @Override
    public IO getHandlerIO() {
        return this.handlerIO;
    }

    private boolean isParticleAllowed(String incomingParticle) {
        if (this.machine instanceof FilteredBeamHatchPartMachine filteredHatch) {
            String filter = filteredHatch.getParticleFilter();
            return filter.isEmpty() || filter.equals(incomingParticle);
        }
        return true;
    }

    private static boolean loggedListenerFailure = false;

    protected void notifyContentsChanged() {
        this.machine.markDirty();
        try {
            this.notifyListeners();
        } catch (Exception e) {
            if (!loggedListenerFailure) {
                loggedListenerFailure = true;
                com.gregtechceu.gtceu.GTCEu.LOGGER.warn(
                        "Beam container listener threw at {}", this.machine.getPos(), e);
            }
        }

        if (this.machine instanceof BeamHatchPartMachine part) {
            part.onChanged();
            var controllers = part.getControllers();
            if (controllers != null && !controllers.isEmpty()) {
                for (var controller : controllers) {
                    if (controller instanceof com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine logicMachine) {
                        var logic = logicMachine.getRecipeLogic();
                        if (logic != null) {
                            logic.markLastRecipeDirty();
                            if (!logic.isWorking()) {
                                logic.updateTickSubscription();
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onMachineLoad() {
        super.onMachineLoad();
        if (this.handlerIO == IO.IN) {
            this.subscription = getMachine().subscribeServerTick(subscription, this::serverTick);
        }
    }

    @Override
    public void onMachineUnLoad() {
        super.onMachineUnLoad();
        if (this.subscription != null) {
            this.subscription.unsubscribe();
            this.subscription = null;
        }
    }

    protected void serverTick() {
        if (this.machine.getOffsetTimer() % 5 != 0) return;
        if (this.handlerIO != IO.IN) return;

        IBeamProvider provider = getBeamNetProvider();
        if (provider == null) return;

        long space = capacity - this.currentFlux;
        if (space <= 0) return;

        scratchSeen.clear();
        BeamPacket available = provider.requestBeam(space, true, scratchSeen);
        if (available.isEmpty()) return;

        if (!isParticleAllowed(available.particle())) return;

        boolean isEmpty = this.currentParticleName.isEmpty() || this.currentFlux <= 0;
        boolean isMatch = !isEmpty && this.currentParticleName.equals(available.particle()) && this.currentEnergy == available.energy();

        if (isEmpty || isMatch) {
            scratchSeen.clear();
            BeamPacket pulled = provider.requestBeam(space, false, scratchSeen);
            if (!pulled.isEmpty()) {
                this.currentParticleName = pulled.particle();
                this.currentEnergy = pulled.energy();
                this.currentFlux += pulled.flux();
                notifyContentsChanged();
            }
        }
    }

    public long insertBeam(BeamPacket packet, boolean simulate) {
        if (packet == null || packet.isEmpty() || packet.flux() <= 0) return 0;
        if (!isParticleAllowed(packet.particle())) return 0;

        boolean isEmpty = this.currentParticleName.isEmpty() || this.currentFlux <= 0;
        boolean isMatch = !isEmpty && this.currentParticleName.equals(packet.particle()) && this.currentEnergy == packet.energy();

        if (isEmpty || isMatch) {
            long space = this.capacity - (isEmpty ? 0 : this.currentFlux);
            long toInsert = Math.min(space, packet.flux());

            if (toInsert > 0 && !simulate) {
                this.currentParticleName = packet.particle();
                this.currentEnergy = packet.energy();
                this.currentFlux = (isEmpty ? 0 : this.currentFlux) + toInsert;
                notifyContentsChanged();
            }
            return toInsert;
        }
        return 0;
    }

    @Override
    public @NotNull BeamPacket requestBeam(long maxFlux, boolean simulate, @NotNull Collection<IBeamProvider> seen) {
        if (seen.contains(this)) return BeamPacket.EMPTY;
        seen.add(this);

        if (this.currentParticleName.isEmpty() || this.currentFlux <= 0) {
            return BeamPacket.EMPTY;
        }

        long fluxToProvide = Math.min(this.currentFlux, maxFlux);
        BeamPacket packet = new BeamPacket(this.currentParticleName, this.currentEnergy, fluxToProvide);

        if (!simulate) {
            this.currentFlux -= fluxToProvide;
            if (this.currentFlux <= 0) {
                this.currentParticleName = "";
                this.currentEnergy = 0.0;
                this.currentFlux = 0L;
            }
            notifyContentsChanged();
        }
        return packet;
    }

    @Override
    public long getMaxFlux(@NotNull Collection<IBeamProvider> seen) {
        return this.currentFlux;
    }

    @Override
    public boolean canBridge(@NotNull Collection<IBeamProvider> seen) {
        return false;
    }

    @Nullable
    @Override
    public IBeamProvider getBeamProvider() {
        return this;
    }

    @Nullable
    private IBeamProvider getBeamNetProvider() {
        BlockPos machinePos = getMachine().getPos();
        net.minecraft.world.level.Level level = getMachine().getLevel();
        if (level == null) return null;

        Direction facing = null;
        if (machine instanceof BeamHatchPartMachine part) {
            facing = part.getFrontFacing();
        }

        for (Direction direction : GTUtil.DIRECTIONS) {
            if (facing != null && direction != facing) continue;

            BlockEntity adjBlock = level.getBlockEntity(machinePos.relative(direction));
            if (adjBlock != null) {
                IBeamProvider provider = BeamCapabilityHelper.getBeamProvider(adjBlock, direction.getOpposite());
                if (provider != null && provider != this) {
                    return provider;
                }
            }
        }
        return null;
    }

    public void invalidateCaps() {
        this.providerCapability.invalidate();
    }

    @Override
    public List<BeamPacket> handleRecipeInner(IO io, GTRecipe recipe, List<BeamPacket> left, boolean simulate) {
        if (left == null || left.isEmpty()) return left;

        if (io == IO.OUT && this.handlerIO == IO.OUT) {
            BeamPacket output = left.get(0);
            if (output.isEmpty()) return left;

            if (!isParticleAllowed(output.particle())) return left;

            boolean isEmpty = this.currentParticleName.isEmpty() || this.currentFlux <= 0;
            boolean isMatch = !isEmpty && this.currentParticleName.equals(output.particle()) && this.currentEnergy == output.energy();

            if (isEmpty || isMatch) {
                if (!simulate) {
                    this.currentParticleName = output.particle();
                    this.currentEnergy = output.energy();
                    this.currentFlux += output.flux();
                    notifyContentsChanged();
                }
                return left.size() > 1 ? left.subList(1, left.size()) : null;
            }
            return left;
        }
        else if (io == IO.IN && this.handlerIO == IO.IN) {
            List<BeamPacket> remaining = new ArrayList<>();
            for (BeamPacket req : left) {
                boolean hasEnough = !this.currentParticleName.isEmpty()
                        && this.currentParticleName.equals(req.particle())
                        && this.currentFlux >= req.flux();

                if (hasEnough) {
                    if (!simulate) {
                        this.currentFlux -= req.flux();
                        if (this.currentFlux <= 0) {
                            this.currentParticleName = "";
                            this.currentEnergy = 0.0;
                            this.currentFlux = 0L;
                        }
                        notifyContentsChanged();
                    }
                } else {
                    remaining.add(req);
                }
            }
            return remaining.isEmpty() ? null : remaining;
        }

        return left;
    }

    @Override
    public @NotNull List<Object> getContents() {
        if (currentParticleName.isEmpty() || currentFlux <= 0) return Collections.emptyList();
        return Collections.singletonList((Object) new BeamPacket(currentParticleName, currentEnergy, currentFlux));
    }

    @Override
    public double getTotalContentAmount() {
        return currentFlux;
    }

    @Override
    public RecipeCapability<BeamPacket> getCapability() {
        return BeamRecipeCapability.CAP;
    }
}
