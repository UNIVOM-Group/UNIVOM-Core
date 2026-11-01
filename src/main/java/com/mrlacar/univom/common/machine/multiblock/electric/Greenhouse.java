package com.mrlacar.univom.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Greenhouse extends WorkableElectricMultiblockMachine {
    private TickableSubscription tickableSubscription;
    private int greeneryTickTimer = 0;

    public Greenhouse(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        if (!isRemote()) {
            tickableSubscription = this.subscribeServerTick(this::greeneryBehavior);
        }
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        if (!isRemote() && tickableSubscription != null) {
            tickableSubscription.unsubscribe();
            tickableSubscription = null;
        }
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        if (!super.beforeWorking(recipe)) return false;

        Level level = this.getLevel();
        if (level == null) return false;

        BlockPos controllerPos = this.getPos();
        Direction facing = this.getFrontFacing();
        Direction backwardFacing = facing.getOpposite();
        Direction right = facing.getClockWise();

        BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos();

        for (int depth = 0; depth <= 6; depth++) {
            for (int width = -2; width <= 2; width++) {
                targetPos.set(controllerPos)
                        .move(backwardFacing, depth)
                        .move(right, width)
                        .move(Direction.UP, 1);

                if (level.getMaxLocalRawBrightness(targetPos) < 9) {
                    return false;
                }
            }
        }

        return true;
    }

    private void greeneryBehavior() {
        if (!this.isFormed() || !getRecipeLogic().isActive()) return;

        if (greeneryTickTimer++ < 20) return;
        greeneryTickTimer = 0;

        Level level = this.getLevel();
        if (level == null) return;

        BlockPos controllerPos = this.getPos();
        Direction facing = this.getFrontFacing();
        Direction backwardFacing = facing.getOpposite();
        Direction right = facing.getClockWise();

        BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos();

        for (int depth = 0; depth <= 6; depth++) {
            for (int width = -2; width <= 2; width++) {
                targetPos.set(controllerPos)
                        .move(backwardFacing, depth)
                        .move(right, width);

                if (level.getBlockState(targetPos).is(Blocks.DIRT)) {
                    level.setBlock(targetPos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                }
            }
        }
    }
}