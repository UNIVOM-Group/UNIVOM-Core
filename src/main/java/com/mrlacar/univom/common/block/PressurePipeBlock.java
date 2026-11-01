package com.mrlacar.univom.common.block;

import com.gregtechceu.gtceu.api.block.PipeBlock;
import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.pipenet.IPipeNode;
import com.gregtechceu.gtceu.api.registry.registrate.provider.GTBlockstateProvider;
import com.gregtechceu.gtceu.client.model.pipe.PipeModel;
import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.api.machine.feature.IPressureEndpoint;
import com.mrlacar.univom.common.blockentity.PressurePipeBlockEntity;
import com.mrlacar.univom.common.data.ModBlockEntities;
import com.mrlacar.univom.common.pipelike.pressure.LevelPressurePipeNet;
import com.mrlacar.univom.common.pipelike.pressure.PressurePipeNet;
import com.mrlacar.univom.common.pipelike.pressure.PressurePipeProperties;
import com.mrlacar.univom.common.pipelike.pressure.PressurePipeType;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PressurePipeBlock extends PipeBlock<PressurePipeType, PressurePipeProperties, LevelPressurePipeNet> {

    public PressurePipeBlock(BlockBehaviour.Properties properties, PressurePipeType pipeType) {
        super(properties, pipeType);
    }

    public PressurePipeType getPressureType() {
        return pipeType;
    }

    @Override
    public @NotNull PipeModel createPipeModel(GTBlockstateProvider provider) {
        return new PipeModel(this, provider, pipeType.getThickness(), UNIVOM.id("block/pipe/pipe_pressure_side"),
                UNIVOM.id("block/pipe/pipe_pressure_in"));
    }

    @Override
    public LevelPressurePipeNet getWorldPipeNet(ServerLevel level) {
        return LevelPressurePipeNet.getOrCreate(level);
    }

    @Override
    public BlockEntityType<? extends PipeBlockEntity<PressurePipeType, PressurePipeProperties>> getBlockEntityType() {
        return ModBlockEntities.PRESSURE_PIPE.get();
    }

    @Override
    public PressurePipeProperties createRawData(BlockState pState, @Nullable ItemStack pStack) {
        return pipeType.getProperties();
    }

    @Override
    public PressurePipeProperties createProperties(@NotNull IPipeNode<PressurePipeType, PressurePipeProperties> pipeTile) {
        return pipeType.getProperties();
    }

    @Override
    public PressurePipeProperties getFallbackType() {
        return PressurePipeType.HV_PIPE.getProperties();
    }

    @Override
    public boolean canPipesConnect(IPipeNode<PressurePipeType, PressurePipeProperties> selfTile, Direction side,
                                   IPipeNode<PressurePipeType, PressurePipeProperties> sideTile) {
        return selfTile instanceof PressurePipeBlockEntity self && sideTile instanceof PressurePipeBlockEntity other &&
                PressurePipeNet.compatible(self.effectiveGas(side), other.effectiveGas(side.getOpposite()));
    }

    @Override
    public boolean canPipeConnectToBlock(IPipeNode<PressurePipeType, PressurePipeProperties> selfTile, Direction side,
                                         @Nullable BlockEntity tile) {
        return tile instanceof IMachineBlockEntity machine &&
                machine.getMetaMachine() instanceof IPressureEndpoint endpoint &&
                endpoint.canConnectPressure(side.getOpposite());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(state.getBlock())) {
            super.onPlace(state, level, pos, oldState, isMoving);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (getWorldPipeNet(level).getNetFromPos(pos) == null) {
            super.tick(state, level, pos, random);
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> blockEntityType) {
        return null;
    }

    @Override
    public boolean canPlaceLiquid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return pipeType.isPipe() && !state.getValue(BlockStateProperties.WATERLOGGED) && fluid == Fluids.WATER;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return pipeType.isPipe() ? super.getShape(state, level, pos, context) : Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return pipeType.isPipe() ? super.getCollisionShape(state, level, pos, context) : Shapes.block();
    }

    @Override
    public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return !pipeType.isPipe();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("univom.tooltip.pressure.rating", pipeType.getProperties().ratingBar()));
        if (pipeType.isVentValve()) {
            tooltip.add(Component.translatable("univom.tooltip.pressure.vent_valve.0"));
            tooltip.add(Component.translatable("univom.tooltip.pressure.vent_valve.1"));
        } else if (pipeType.isSafetyValve()) {
            tooltip.add(Component.translatable("univom.tooltip.pressure.safety_valve.0",
                    PressurePipeType.DEFAULT_SAFETY_PERCENT));
            tooltip.add(Component.translatable("univom.tooltip.pressure.safety_valve.1"));
        } else {
            tooltip.add(Component.translatable("univom.tooltip.pressure.volume", pipeType.getProperties().volume()));
            tooltip.add(Component.translatable("univom.tooltip.pressure.single_gas"));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static BlockColor tintedColor() {
        return (blockState, level, blockPos, index) -> index <= 1 &&
                blockState.getBlock() instanceof PressurePipeBlock block ? block.tierColor() : -1;
    }

    @OnlyIn(Dist.CLIENT)
    public static ItemColor tintedItemColor() {
        return (itemStack, index) -> index <= 1 && itemStack.getItem() instanceof BlockItem item &&
                item.getBlock() instanceof PressurePipeBlock block ? block.tierColor() : -1;
    }

    private int tierColor() {
        return pipeType.isPipe() || pipeType.isTank() ? pipeType.getMaterial().getMaterialRGB() : -1;
    }
}
