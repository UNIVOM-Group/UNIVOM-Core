package com.mrlacar.univom.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.mrlacar.univom.api.machine.feature.IPressureEndpoint;
import com.mrlacar.univom.common.pipelike.pressure.LevelPressurePipeNet;
import com.mrlacar.univom.common.pipelike.pressure.PressurePipeNet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressurePortPartMachine extends MultiblockPartMachine implements IPressureEndpoint, IMachineLife {

    public PressurePortPartMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    public BlockPos getNodePos() {
        return getPos().relative(getFrontFacing());
    }

    @Override
    public boolean canConnectPressure(Direction side) {
        return side == getFrontFacing();
    }

    @Override
    public boolean canShared() {
        return false;
    }

    @Override
    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        return false;
    }

    @Override
    public void onMachineRemoved() {
        release(getFrontFacing());
    }

    @Override
    public void onRotated(Direction oldFacing, Direction newFacing) {
        super.onRotated(oldFacing, newFacing);
        if (oldFacing != newFacing) {
            release(oldFacing);
        }
    }

    private void release(Direction facing) {
        if (getLevel() instanceof ServerLevel level) {
            BlockPos node = getPos().relative(facing);
            PressurePipeNet net = LevelPressurePipeNet.getOrCreate(level).getNetFromPos(node);
            if (net != null && net.isAttachedVia(getPos(), node)) {
                net.detach(getPos());
            }
        }
    }
}
