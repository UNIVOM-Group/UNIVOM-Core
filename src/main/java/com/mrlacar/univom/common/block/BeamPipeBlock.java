package com.mrlacar.univom.common.block;

import com.gregtechceu.gtceu.api.block.PipeBlock;
import com.gregtechceu.gtceu.api.block.property.GTBlockStateProperties;
import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.pipenet.IPipeNode;
import com.gregtechceu.gtceu.api.registry.registrate.provider.GTBlockstateProvider;
import com.gregtechceu.gtceu.client.model.pipe.ActivablePipeModel;
import com.gregtechceu.gtceu.client.model.pipe.PipeModel;
import com.mrlacar.univom.common.blockentity.BeamPipeBlockEntity;
import com.mrlacar.univom.common.data.ModBlockEntities;
import com.mrlacar.univom.common.pipelike.beam.BeamCapabilityHelper;
import com.mrlacar.univom.common.pipelike.beam.BeamPipeProperties;
import com.mrlacar.univom.common.pipelike.beam.BeamPipeType;
import com.mrlacar.univom.common.pipelike.beam.LevelBeamPipeNet;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BeamPipeBlock extends PipeBlock<BeamPipeType, BeamPipeProperties, LevelBeamPipeNet> {
    private final BeamPipeProperties properties;

    public BeamPipeBlock(BlockBehaviour.Properties properties, BeamPipeType pipeType) {
        super(properties, pipeType);
        this.properties = BeamPipeProperties.INSTANCE;
        registerDefaultState(defaultBlockState().setValue(GTBlockStateProperties.ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(GTBlockStateProperties.ACTIVE);
    }

    @Override
    public @NotNull PipeModel createPipeModel(GTBlockstateProvider provider) {
        ActivablePipeModel pipeModel = new ActivablePipeModel(this, pipeType.getThickness(),
                new ResourceLocation("univom", "block/pipe/pipe_beam_side"),
                new ResourceLocation("univom", "block/pipe/pipe_beam_in"), provider);
        pipeModel.setSideOverlay(new ResourceLocation("univom", "block/pipe/pipe_beam_side_overlay"));
        pipeModel.setSideOverlayActive(new ResourceLocation("univom", "block/pipe/pipe_beam_side_overlay_active"));
        return pipeModel;
    }

    @Override
    public LevelBeamPipeNet getWorldPipeNet(ServerLevel level) {
        return LevelBeamPipeNet.getOrCreate(level);
    }

    @Override
    public BlockEntityType<? extends PipeBlockEntity<BeamPipeType, BeamPipeProperties>> getBlockEntityType() {
        return ModBlockEntities.BEAM_PIPE.get();
    }

    @Override
    public BeamPipeProperties createRawData(BlockState pState, @Nullable ItemStack pStack) {
        return null;
    }

    @Override
    public BeamPipeProperties createProperties(@NotNull IPipeNode<BeamPipeType, BeamPipeProperties> pipeTile) {
        return this.properties;
    }

    @Override
    public BeamPipeProperties getFallbackType() {
        return BeamPipeProperties.INSTANCE;
    }

    @OnlyIn(Dist.CLIENT)
    public static BlockColor tintedColor() {
        return (blockState, level, blockPos, index) -> {
            if (blockPos != null && level != null &&
                    level.getBlockEntity(blockPos) instanceof PipeBlockEntity<?, ?> pipe) {
                if (!pipe.getFrameMaterial().isNull()) {
                    if (index == 3) {
                        return pipe.getFrameMaterial().getMaterialRGB();
                    } else if (index == 4) {
                        return pipe.getFrameMaterial().getMaterialSecondaryRGB();
                    }
                }
                if (pipe.isPainted()) {
                    return pipe.getRealColor();
                }
            }
            return -1;
        };
    }

    @Override
    public boolean canPipesConnect(IPipeNode<BeamPipeType, BeamPipeProperties> selfTile, Direction side, IPipeNode<BeamPipeType, BeamPipeProperties> sideTile) {
        return selfTile instanceof BeamPipeBlockEntity && sideTile instanceof BeamPipeBlockEntity;
    }

    @Override
    public boolean canPipeConnectToBlock(IPipeNode<BeamPipeType, BeamPipeProperties> selfTile, Direction side, @Nullable BlockEntity tile) {
        if (tile == null) return false;
        return BeamCapabilityHelper.getBeamProvider(tile, side.getOpposite()) != null;
    }

    @Override
    public GTToolType getPipeTuneTool() {
        return GTToolType.WRENCH;
    }
}
