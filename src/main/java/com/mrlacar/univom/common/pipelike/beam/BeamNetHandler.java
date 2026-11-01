package com.mrlacar.univom.common.pipelike.beam;

import com.mrlacar.univom.api.capability.IBeamProvider;
import com.mrlacar.univom.api.data.particle.BeamPacket;
import com.mrlacar.univom.common.blockentity.BeamPipeBlockEntity;
import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class BeamNetHandler implements IBeamProvider {
    private final BeamPipeBlockEntity pipe;
    private final Level world;
    private final Direction facing;

    @Getter
    private BeamPipeNet net;

    private long lastActiveTime = 0;

    public BeamNetHandler(BeamPipeNet net, @NotNull BeamPipeBlockEntity pipe, @Nullable Direction facing) {
        this.net = net;
        this.pipe = pipe;
        this.facing = facing;
        this.world = pipe.getLevel();
    }

    public void updateNetwork(BeamPipeNet net) {
        this.net = net;
    }

    @Override
    public @NotNull BeamPacket requestBeam(long maxFlux, boolean simulate, @NotNull Collection<IBeamProvider> seen) {
        IBeamProvider provider = getComputationProvider(seen);
        if (provider == null) return BeamPacket.EMPTY;

        BeamPacket packet = provider.requestBeam(maxFlux, simulate, seen);

        if (!packet.isEmpty() && !simulate) {
            if (world.getGameTime() - lastActiveTime >= 20) {
                setPipesActive();
                lastActiveTime = world.getGameTime();
            }
        }
        return packet;
    }

    @Override
    public long getMaxFlux(@NotNull Collection<IBeamProvider> seen) {
        IBeamProvider provider = getComputationProvider(seen);
        return provider == null ? 0L : provider.getMaxFlux(seen);
    }

    @Override
    public boolean canBridge(@NotNull Collection<IBeamProvider> seen) {
        IBeamProvider provider = getComputationProvider(seen);
        return provider == null || provider.canBridge(seen);
    }

    private void setPipesActive() {
        for (net.minecraft.core.BlockPos pos : net.getAllNodes().keySet()) {
            if (world.isLoaded(pos)) {
                if (world.getBlockEntity(pos) instanceof BeamPipeBlockEntity beamPipe) {
                    beamPipe.setActive(true, 100);
                }
            }
        }
    }

    @Nullable
    private IBeamProvider getComputationProvider(@NotNull Collection<IBeamProvider> seen) {
        if (net == null || pipe == null || pipe.isInValid()) return null;

        BeamRoutePath path = net.getNetData(pipe.getPipePos(), facing);
        if (path == null) return null;

        IBeamProvider hatch = path.getHandler(world);
        if (hatch == null || seen.contains(hatch)) return null;
        return hatch;
    }
}
