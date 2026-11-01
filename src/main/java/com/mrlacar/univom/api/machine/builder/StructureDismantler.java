package com.mrlacar.univom.api.machine.builder;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Lock;

public final class StructureDismantler {

    private StructureDismantler() {}

    public static DismantleResult dismantle(ServerPlayer player, IMultiController controller, MaterialSource source) {
        if (!controller.isFormed()) {
            return DismantleResult.NOT_FORMED;
        }
        Level world = player.level();
        BlockPos controllerPos = controller.self().getPos();
        List<BlockPos> targets;
        Lock lock = controller.getPatternLock();
        lock.lock();
        try {
            Collection<BlockPos> cache = controller.getMultiblockState().getCache();
            targets = new ArrayList<>(cache.size());
            for (BlockPos pos : cache) {
                if (!pos.equals(controllerPos) && !world.isEmptyBlock(pos)) {
                    targets.add(pos.immutable());
                }
            }
        } finally {
            lock.unlock();
        }
        if (targets.size() > BuilderSettings.MAX_BLOCKS_PER_OPERATION) {
            return DismantleResult.TOO_LARGE;
        }
        for (BlockPos pos : targets) {
            if (!isPartEmpty(world, pos)) {
                return DismantleResult.PART_NOT_EMPTY;
            }
        }
        for (BlockPos pos : targets) {
            if (!player.mayInteract(world, pos) ||
                    ForgeHooks.onBlockBreakEvent(world, player.gameMode.getGameModeForPlayer(), player, pos) == -1) {
                return DismantleResult.NO_PERMISSION;
            }
        }
        for (BlockPos pos : targets) {
            BlockState state = world.getBlockState(pos);
            ItemStack drop = new ItemStack(state.getBlock());
            if (world.removeBlock(pos, false) && !drop.isEmpty()) {
                ItemStack remainder = source.give(drop);
                if (!remainder.isEmpty()) {
                    player.drop(remainder, false);
                }
            }
        }
        return DismantleResult.OK;
    }

    private static boolean isPartEmpty(Level world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity == null) {
            return true;
        }
        IItemHandler items = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null);
        if (items != null) {
            for (int slot = 0; slot < items.getSlots(); slot++) {
                ItemStack stack = items.getStackInSlot(slot);
                if (!stack.isEmpty() && !IntCircuitBehaviour.isIntegratedCircuit(stack)) {
                    return false;
                }
            }
        }
        IFluidHandler fluids = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER).resolve().orElse(null);
        if (fluids != null) {
            for (int tank = 0; tank < fluids.getTanks(); tank++) {
                if (!fluids.getFluidInTank(tank).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    public enum DismantleResult {
        OK,
        NOT_FORMED,
        TOO_LARGE,
        NO_PERMISSION,
        PART_NOT_EMPTY
    }
}
