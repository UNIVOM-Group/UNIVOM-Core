package com.mrlacar.univom.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.blockentity.IPaintable;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.utils.GTTransferUtils;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SimpleItemBusPartMachine extends TieredIOPartMachine implements IMachineLife, IPaintable {
    @Nullable
    protected TickableSubscription autoIOSubs;
    @Nullable
    protected ISubscription inventorySubs;

    public SimpleItemBusPartMachine(IMachineBlockEntity holder, int tier, IO io) {
        super(holder, tier, io);
    }

    @Nullable
    public NotifiableItemStackHandler getInventory() {
        return null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, this::updateInventorySubscription));
        }
        getHandlerList().setColor(getPaintingColor());

        if (getInventory() != null) {
            inventorySubs = getInventory().addChangedListener(this::updateInventorySubscription);
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (inventorySubs != null) {
            inventorySubs.unsubscribe();
            inventorySubs = null;
        }
        if (autoIOSubs != null) {
            autoIOSubs.unsubscribe();
            autoIOSubs = null;
        }
    }

    @Override
    public void onMachineRemoved() {
        if (getInventory() != null) {
            clearInventory(getInventory().storage);
        }
    }

    @Override
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChanged(block, fromPos, isMoving);
        updateInventorySubscription();
    }

    @Override
    public void onRotated(Direction oldFacing, Direction newFacing) {
        super.onRotated(oldFacing, newFacing);
        updateInventorySubscription(newFacing);
    }

    protected void updateInventorySubscription() {
        updateInventorySubscription(getFrontFacing());
    }

    protected void updateInventorySubscription(Direction newFacing) {
        if (!isWorkingEnabled() || getInventory() == null || getLevel() == null) {
            if (autoIOSubs != null) {
                autoIOSubs.unsubscribe();
                autoIOSubs = null;
            }
            return;
        }

        boolean canImport = this.io.support(IO.IN) && GTTransferUtils.hasAdjacentItemHandler(getLevel(), getPos(), newFacing);
        boolean canExport = this.io.support(IO.OUT) && !getInventory().isEmpty() && GTTransferUtils.hasAdjacentItemHandler(getLevel(), getPos(), newFacing.getOpposite());

        if (canImport || canExport) {
            autoIOSubs = subscribeServerTick(autoIOSubs, this::autoIO);
        } else if (autoIOSubs != null) {
            autoIOSubs.unsubscribe();
            autoIOSubs = null;
        }
    }

    protected void autoIO() {
        if (getOffsetTimer() % 5 == 0 && isWorkingEnabled() && getInventory() != null) {
            boolean handled = false;

            if (this.io.support(IO.OUT) && !getInventory().isEmpty()) {
                getInventory().exportToNearby(getFrontFacing().getOpposite());
                handled = true;
            }

            if (this.io.support(IO.IN)) {
                getInventory().importFromNearby(getFrontFacing());
                handled = true;
            }

            if (handled) {
                updateInventorySubscription();
            }
        }
    }

    @Override
    public void setWorkingEnabled(boolean workingEnabled) {
        super.setWorkingEnabled(workingEnabled);
        updateInventorySubscription();
    }

    @Override
    public void onPaintingColorChanged(int color) {
        getHandlerList().setColor(color, true);
    }

    @Override
    public int tintColor(int index) {
        if (index == 9) return getRealColor();
        return -1;
    }
}