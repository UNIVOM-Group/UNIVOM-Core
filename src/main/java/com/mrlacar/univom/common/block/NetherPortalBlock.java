package com.mrlacar.univom.common.block;

import com.mrlacar.univom.world.level.portal.NetherPortalShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class NetherPortalBlock extends net.minecraft.world.level.block.NetherPortalBlock {
    public NetherPortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        Direction.Axis direction$axis = facing.getAxis();
        Direction.Axis direction$axis1 = state.getValue(AXIS);
        boolean flag = direction$axis1 != direction$axis && direction$axis.isHorizontal();

        if (!flag && !facingState.is(this) && !(new NetherPortalShape(level, currentPos, direction$axis1)).isComplete()) {
            return Blocks.AIR.defaultBlockState();
        }

        return state;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity.canChangeDimensions()) {
            entity.handleInsidePortal(pos);
        }
    }
}
