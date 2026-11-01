package com.mrlacar.univom.common.pipelike.beam;

import com.gregtechceu.gtceu.api.pipenet.PipeNetWalker;
import com.mrlacar.univom.common.blockentity.BeamPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class BeamNetWalker extends PipeNetWalker<BeamPipeBlockEntity, BeamPipeProperties, BeamPipeNet> {
    public static final BeamRoutePath FAILED_MARKER = new BeamRoutePath(null, null, 0);

    @Nullable
    public static BeamRoutePath createNetData(BeamPipeNet world, BlockPos sourcePipe, Direction faceToSourceHandler) {
        BeamNetWalker walker = new BeamNetWalker(world, sourcePipe, 1);
        walker.sourcePipe = sourcePipe;
        walker.facingToHandler = faceToSourceHandler;
        walker.traversePipeNet();
        return walker.isFailed() ? FAILED_MARKER : walker.routePath;
    }

    private BeamRoutePath routePath;
    private BlockPos sourcePipe;
    private Direction facingToHandler;

    protected BeamNetWalker(BeamPipeNet world, BlockPos sourcePipe, int distance) {
        super(world, sourcePipe, distance);
    }

    @Override
    protected PipeNetWalker<BeamPipeBlockEntity, BeamPipeProperties, BeamPipeNet> createSubWalker(BeamPipeNet world, Direction facingToNextPos, BlockPos nextPos, int walkedBlocks) {
        BeamNetWalker walker = new BeamNetWalker(world, nextPos, walkedBlocks);
        walker.facingToHandler = facingToHandler;
        walker.sourcePipe = sourcePipe;
        return walker;
    }

    @Override
    protected void checkPipe(BeamPipeBlockEntity pipeTile, BlockPos pos) {}

    @Override
    protected void checkNeighbour(BeamPipeBlockEntity pipeTile, BlockPos pipePos, Direction faceToNeighbour, @Nullable BlockEntity neighbourTile) {
        if (neighbourTile == null || (pipePos.equals(sourcePipe) && faceToNeighbour == facingToHandler)) {
            return;
        }

        if (((BeamNetWalker) root).routePath == null) {
            if (!(neighbourTile instanceof BeamPipeBlockEntity)) {
                if (BeamCapabilityHelper.getBeamProvider(neighbourTile, faceToNeighbour.getOpposite()) != null) {
                    ((BeamNetWalker) root).routePath = new BeamRoutePath(pipeTile, faceToNeighbour, getWalkedBlocks());
                    stop();
                }
            }
        }
    }

    @Override
    protected Class<BeamPipeBlockEntity> getBasePipeClass() {
        return BeamPipeBlockEntity.class;
    }
}
