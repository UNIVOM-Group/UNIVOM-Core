package com.mrlacar.univom.common.block;

import com.mrlacar.univom.common.pipelike.pressure.LevelPressurePipeNet;
import com.mrlacar.univom.common.pipelike.pressure.PressurePipeNet;
import com.mrlacar.univom.common.pipelike.pressure.PressurePipeType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class VentValveBlock extends PressurePipeBlock {

    public VentValveBlock(BlockBehaviour.Properties properties, PressurePipeType pipeType) {
        super(properties, pipeType);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.POWERED);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.hasNeighborSignal(pos)) {
            BlockState current = level.getBlockState(pos);
            if (current.getBlock() == this) {
                level.setBlock(pos, current.setValue(BlockStateProperties.POWERED, true), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty() && !player.isShiftKeyDown()) {
            if (level instanceof ServerLevel serverLevel) {
                vent(serverLevel, pos);
                player.displayClientMessage(Component.translatable("univom.message.pressure.vented"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
                                boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockState current = level.getBlockState(pos);
        if (current.getBlock() != this) {
            return;
        }
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != current.getValue(BlockStateProperties.POWERED)) {
            level.setBlock(pos, current.setValue(BlockStateProperties.POWERED, powered), Block.UPDATE_CLIENTS);
            if (powered) {
                vent(serverLevel, pos);
            }
        }
    }

    private static void vent(ServerLevel level, BlockPos pos) {
        PressurePipeNet net = LevelPressurePipeNet.getOrCreate(level).getNetFromPos(pos);
        if (net != null) {
            net.vent(pos);
        }
    }
}
