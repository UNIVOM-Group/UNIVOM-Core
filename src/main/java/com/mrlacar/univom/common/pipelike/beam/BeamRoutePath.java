package com.mrlacar.univom.common.pipelike.beam;

import com.gregtechceu.gtceu.api.pipenet.IRoutePath;
import com.mrlacar.univom.api.capability.IBeamProvider;
import com.mrlacar.univom.common.blockentity.BeamPipeBlockEntity;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BeamRoutePath implements IRoutePath<IBeamProvider> {
    @Getter
    private final BeamPipeBlockEntity targetPipe;
    @Getter
    private final Direction targetFacing;
    @Getter
    private final int distance;

    public BeamRoutePath(BeamPipeBlockEntity targetPipe, Direction targetFacing, int distance) {
        this.targetPipe = targetPipe;
        this.targetFacing = targetFacing;
        this.distance = distance;
    }

    @Override
    public @NotNull BlockPos getTargetPipePos() {
        return targetPipe.getPipePos();
    }

    @Nullable
    @Override
    public IBeamProvider getHandler(Level world) {
        BlockPos targetPos = getTargetPipePos().relative(targetFacing);
        BlockEntity be = world.getBlockEntity(targetPos);
        return BeamCapabilityHelper.getBeamProvider(be, targetFacing.getOpposite());
    }
}
