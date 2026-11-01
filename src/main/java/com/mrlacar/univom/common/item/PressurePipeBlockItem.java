package com.mrlacar.univom.common.item;

import com.gregtechceu.gtceu.api.block.PipeBlock;
import com.gregtechceu.gtceu.api.item.PipeBlockItem;
import com.gregtechceu.gtceu.api.pipenet.IPipeNode;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.mrlacar.univom.common.block.PressurePipeBlock;
import com.mrlacar.univom.common.pipelike.pressure.PressurePipeProperties;
import com.mrlacar.univom.common.pipelike.pressure.PressurePipeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PressurePipeBlockItem extends PipeBlockItem {

    public PressurePipeBlockItem(PipeBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public boolean placeBlock(BlockPlaceContext context, BlockState state) {
        if (!(getBlock() instanceof PressurePipeBlock block) || block.getPressureType().isPipe()) {
            return super.placeBlock(context, state);
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.setBlock(pos, state, Block.UPDATE_ALL_IMMEDIATE)) {
            return false;
        }
        if (level.isClientSide) {
            return true;
        }
        IPipeNode<PressurePipeType, PressurePipeProperties> selfTile = block.getPipeTile(level, pos);
        if (selfTile == null) {
            return true;
        }
        Direction clicked = context.getClickedFace().getOpposite();
        if (block.canConnect(selfTile, clicked)) {
            selfTile.setConnection(clicked, true, false);
        }
        for (Direction facing : GTUtil.DIRECTIONS) {
            BlockEntity neighbour = selfTile.getNeighbor(facing);
            if (neighbour instanceof IPipeNode otherPipe) {
                if (otherPipe.isConnected(facing.getOpposite())) {
                    if (otherPipe.getPipeBlock().canPipesConnect(otherPipe, facing.getOpposite(), selfTile)) {
                        selfTile.setConnection(facing, true, true);
                    } else {
                        otherPipe.setConnection(facing.getOpposite(), false, true);
                    }
                }
            } else if (!ConfigHolder.INSTANCE.machines.gt6StylePipesCables &&
                    block.canPipeConnectToBlock(selfTile, facing, neighbour)) {
                selfTile.setConnection(facing, true, false);
            }
        }
        return true;
    }
}
