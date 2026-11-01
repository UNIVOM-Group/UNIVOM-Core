package com.mrlacar.univom.common.blockentity;

import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.pipenet.IPipeNode;
import com.gregtechceu.gtceu.common.blockentity.LaserPipeBlockEntity;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.mrlacar.univom.api.capability.IBeamProvider;
import com.mrlacar.univom.api.capability.forge.ModCapability;
import com.mrlacar.univom.common.block.BeamPipeBlock;
import com.mrlacar.univom.common.pipelike.beam.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.EnumMap;

public class BeamPipeBlockEntity extends PipeBlockEntity<BeamPipeType, BeamPipeProperties> {
    private final EnumMap<Direction, BeamNetHandler> handlers = new EnumMap<>(Direction.class);
    private WeakReference<BeamPipeNet> currentPipeNet = new WeakReference<>(null);
    private BeamNetHandler defaultHandler;

    private final EnumMap<Direction, LazyOptional<IBeamProvider>> handlerOptionals = new EnumMap<>(Direction.class);
    private LazyOptional<IBeamProvider> defaultHandlerOptional = LazyOptional.empty();

    public BeamPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public boolean canHaveBlockedFaces() {
        return false;
    }

    private void initHandlers() {
        BeamPipeNet net = getBeamPipeNet();
        if (net == null) return;

        discardHandlers();

        for (Direction facing : GTUtil.DIRECTIONS) {
            BeamNetHandler handler = new BeamNetHandler(net, this, facing);
            handlers.put(facing, handler);
            handlerOptionals.put(facing, LazyOptional.of(() -> handler));
        }
        defaultHandler = new BeamNetHandler(net, this, null);
        defaultHandlerOptional = LazyOptional.of(() -> defaultHandler);
    }

    private void invalidateOptionals() {
        for (LazyOptional<IBeamProvider> opt : handlerOptionals.values()) {
            if (opt.isPresent()) opt.invalidate();
        }
        if (defaultHandlerOptional.isPresent()) defaultHandlerOptional.invalidate();
    }

    private void discardHandlers() {
        invalidateOptionals();
        handlerOptionals.clear();
        defaultHandlerOptional = LazyOptional.empty();
        handlers.clear();
        defaultHandler = null;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        if (capability == ModCapability.BEAM_PROVIDER) {
            if (level == null || level.isClientSide) return LazyOptional.empty();
            if (facing != null && !isConnected(facing)) return LazyOptional.empty();

            if (handlers.isEmpty() || !defaultHandlerOptional.isPresent()) {
                initHandlers();
            }
            if (handlers.isEmpty()) return LazyOptional.empty();

            checkNetwork();

            LazyOptional<IBeamProvider> optional = facing == null ?
                    defaultHandlerOptional :
                    handlerOptionals.getOrDefault(facing, defaultHandlerOptional);
            return ModCapability.BEAM_PROVIDER.orEmpty(capability, optional);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        invalidateOptionals();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        discardHandlers();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        discardHandlers();
    }

    public void checkNetwork() {
        if (defaultHandler != null) {
            BeamPipeNet current = getBeamPipeNet();
            if (defaultHandler.getNet() != current) {
                defaultHandler.updateNetwork(current);
                for (BeamNetHandler handler : handlers.values()) {
                    handler.updateNetwork(current);
                }
            }
        }
    }

    public BeamPipeNet getBeamPipeNet() {
        if (level == null || level.isClientSide) return null;
        BeamPipeNet currentPipeNet = this.currentPipeNet.get();
        if (currentPipeNet != null && currentPipeNet.isValid() && currentPipeNet.containsNode(getPipePos()))
            return currentPipeNet;

        LevelBeamPipeNet worldNet = ((BeamPipeBlock) getPipeBlock()).getWorldPipeNet((ServerLevel) getPipeLevel());
        currentPipeNet = worldNet.getNetFromPos(getPipePos());
        if (currentPipeNet != null) {
            this.currentPipeNet = new WeakReference<>(currentPipeNet);
        }
        return currentPipeNet;
    }

    @Override
    public boolean canAttachTo(Direction side) {
        return false;
    }

    @Override
    public void setConnection(Direction side, boolean connected, boolean fromNeighbor) {
        if (!getLevel().isClientSide && connected) {
            int connections = getConnections();
            connections &= ~(1 << side.ordinal());
            connections &= ~(1 << side.getOpposite().ordinal());
            if (connections != 0) return;

            BlockEntity tile = getLevel().getBlockEntity(getBlockPos().relative(side));
            if (tile instanceof IPipeNode<?, ?> pipeTile &&
                    pipeTile.getPipeType().getClass() == this.getPipeType().getClass()) {
                connections = pipeTile.getConnections();
                connections &= ~(1 << side.ordinal());
                connections &= ~(1 << side.getOpposite().ordinal());
                if (connections != 0) return;
            }
        }
        super.setConnection(side, connected, fromNeighbor);
    }

    public void setActive(boolean active, int duration) {
        LaserPipeBlockEntity.setPipeActive(this, this.getBlockState(), active, duration);
    }

    @Override
    public GTToolType getPipeTuneTool() {
        return GTToolType.WRENCH;
    }
}
