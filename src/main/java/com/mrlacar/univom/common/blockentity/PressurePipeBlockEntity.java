package com.mrlacar.univom.common.blockentity;

import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.pipenet.Node;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.mojang.datafixers.util.Pair;
import com.mrlacar.univom.common.block.PressurePipeBlock;
import com.mrlacar.univom.common.pipelike.pressure.LevelPressurePipeNet;
import com.mrlacar.univom.common.pipelike.pressure.PressurePipeNet;
import com.mrlacar.univom.common.pipelike.pressure.PressurePipeProperties;
import com.mrlacar.univom.common.pipelike.pressure.PressurePipeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Set;

public class PressurePipeBlockEntity extends PipeBlockEntity<PressurePipeType, PressurePipeProperties> {

    private static final int[] SAFETY_STEPS = { 50, 60, 70, 80, 90, 95, 99 };

    private WeakReference<PressurePipeNet> currentPipeNet = new WeakReference<>(null);

    public PressurePipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public boolean canHaveBlockedFaces() {
        return false;
    }

    @Override
    public boolean canAttachTo(Direction side) {
        return level != null && level.isLoaded(getPipePos().relative(side)) &&
                getPipeBlock().canPipeConnectToBlock(this, side, getNeighbor(side));
    }

    @Nullable
    public PressurePipeNet getPressureNet() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        PressurePipeNet net = currentPipeNet.get();
        if (net != null && net.isValid() && net.containsNode(getPipePos())) {
            return net;
        }
        net = ((PressurePipeBlock) getPipeBlock()).getWorldPipeNet(serverLevel).getNetFromPos(getPipePos());
        if (net != null) {
            currentPipeNet = new WeakReference<>(net);
        }
        return net;
    }

    @Nullable
    public ResourceLocation effectiveGas(@Nullable Direction ignoredSide) {
        PressurePipeNet net = getPressureNet();
        if (net != null) {
            return net.getGas();
        }
        for (Direction side : GTUtil.DIRECTIONS) {
            if (side != ignoredSide && isConnected(side) && getNeighbor(side) instanceof PressurePipeBlockEntity other) {
                PressurePipeNet otherNet = other.getPressureNet();
                if (otherNet != null && otherNet.getGas() != null) {
                    return otherNet.getGas();
                }
            }
        }
        return null;
    }

    @Override
    public void setConnection(Direction side, boolean connected, boolean fromNeighbor) {
        if (connected && level != null && !level.isClientSide && blocksGasJoin(side)) {
            return;
        }
        super.setConnection(side, connected, fromNeighbor);
    }

    @Override
    public Pair<GTToolType, InteractionResult> onToolClick(Set<GTToolType> toolTypes, ItemStack itemStack,
                                                           UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || level == null) {
            return super.onToolClick(toolTypes, itemStack, context);
        }
        boolean client = level.isClientSide;
        if (getPipeType().isSafetyValve() && toolTypes.contains(GTToolType.SCREWDRIVER)) {
            if (!client) {
                cycleSafetyPercent(player);
            }
            return Pair.of(GTToolType.SCREWDRIVER, InteractionResult.sidedSuccess(client));
        }
        if (!client && toolTypes.contains(getPipeTuneTool()) && blocksGasJoin(gridSide(context))) {
            player.displayClientMessage(Component.translatable("univom.message.pressure.incompatible_gas"), true);
            return Pair.of(getPipeTuneTool(), InteractionResult.SUCCESS);
        }
        return super.onToolClick(toolTypes, itemStack, context);
    }

    private boolean blocksGasJoin(Direction side) {
        return !isConnected(side) && getNeighbor(side) instanceof PressurePipeBlockEntity other &&
                !PressurePipeNet.compatible(effectiveGas(side), other.effectiveGas(side.getOpposite()));
    }

    private void cycleSafetyPercent(Player player) {
        LevelPressurePipeNet worldNet = LevelPressurePipeNet.getOrCreate((ServerLevel) level);
        PressurePipeNet net = worldNet.getNetFromPos(getPipePos());
        Node<PressurePipeProperties> node = net == null ? null : net.getNodeAt(getPipePos());
        if (node != null) {
            int next = nextSafetyPercent(node.data.safetyPercent());
            worldNet.updateData(getPipePos(), node.data.withSafetyPercent(next));
            player.displayClientMessage(Component.translatable("univom.message.pressure.safety_valve_set", next),
                    true);
        }
    }

    private static Direction gridSide(UseOnContext context) {
        BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(),
                context.getClickedPos(), false);
        Direction side = ICoverable.determineGridSideHit(hit);
        return side == null ? hit.getDirection() : side;
    }

    private static int nextSafetyPercent(int current) {
        for (int step : SAFETY_STEPS) {
            if (step > current) {
                return step;
            }
        }
        return SAFETY_STEPS[0];
    }
}
