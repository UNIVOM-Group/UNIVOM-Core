package com.mrlacar.univom.integration.ae2.machine;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.list.AEListGridWidget;
import com.gregtechceu.gtceu.integration.ae2.utils.KeyStorage;
import com.gregtechceu.gtceu.utils.GTMath;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import lombok.NoArgsConstructor;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WirelessMEOutputDualHatchPartMachine extends WirelessMEBusPartMachine implements IMachineLife, IInteractedMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            WirelessMEOutputDualHatchPartMachine.class, WirelessMEBusPartMachine.MANAGED_FIELD_HOLDER);

    protected final static int LIST_ROWS = 3;
    protected final static int SECTION_HEIGHT = LIST_ROWS * 18;
    protected final static int SECTION_SPACING = 10;

    @Persisted
    private KeyStorage internalBuffer;

    @Persisted
    private KeyStorage internalFluidBuffer;

    private NotifiableFluidTank fluidTank;

    @Nullable
    protected ISubscription fluidTankSubs;

    public WirelessMEOutputDualHatchPartMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, IO.OUT, args);
        this.internalFluidBuffer = new KeyStorage();
        this.fluidTank = new InaccessibleInfiniteTank(this);
    }

    @Override
    protected NotifiableItemStackHandler createInventory(Object... args) {
        this.internalBuffer = new KeyStorage();
        return new InaccessibleInfiniteHandler(this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        fluidTankSubs = fluidTank.addChangedListener(this::updateInventorySubscription);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (fluidTankSubs != null) {
            fluidTankSubs.unsubscribe();
            fluidTankSubs = null;
        }
    }

    @Override
    public void onMachineRemoved() {
        var grid = getMainNode().getGrid();
        if (grid != null) {
            MEStorage networkInv = grid.getStorageService().getInventory();
            if (!internalBuffer.isEmpty()) {
                for (var entry : internalBuffer) {
                    networkInv.insert(entry.getKey(), entry.getLongValue(), Actionable.MODULATE, actionSource);
                }
            }
            if (!internalFluidBuffer.isEmpty()) {
                for (var entry : internalFluidBuffer) {
                    networkInv.insert(entry.getKey(), entry.getLongValue(), Actionable.MODULATE, actionSource);
                }
            }
        }
        super.onMachineRemoved();
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected boolean shouldSubscribe() {
        return super.shouldSubscribe() &&
                (!internalBuffer.storage.isEmpty() || !internalFluidBuffer.storage.isEmpty());
    }

    @Override
    public void autoIO() {
        if (!this.shouldSyncME()) return;
        if (this.updateMEStatus()) {
            var grid = getMainNode().getGrid();
            if (grid != null) {
                MEStorage networkInv = grid.getStorageService().getInventory();
                if (!internalBuffer.isEmpty()) {
                    internalBuffer.insertInventory(networkInv, actionSource);
                }
                if (!internalFluidBuffer.isEmpty()) {
                    internalFluidBuffer.insertInventory(networkInv, actionSource);
                }
            }
            this.updateInventorySubscription();
        }
    }

    @Override
    public Widget createUIWidget() {
        int fluidSectionY = 20 + SECTION_HEIGHT + SECTION_SPACING;

        WidgetGroup group = new WidgetGroup(0, 0, 170, fluidSectionY + SECTION_HEIGHT);
        group.addWidget(new LabelWidget(5, 0, () -> this.isOnline ?
                "gtceu.gui.me_network.online" :
                "gtceu.gui.me_network.offline"));

        group.addWidget(new LabelWidget(5, 10, "gtceu.gui.waiting_list"));
        group.addWidget(new AEListGridWidget.Item(5, 20, LIST_ROWS, this.internalBuffer));

        group.addWidget(new AEListGridWidget.Fluid(5, fluidSectionY, LIST_ROWS, this.internalFluidBuffer));

        return group;
    }

    private class InaccessibleInfiniteHandler extends NotifiableItemStackHandler {

        public InaccessibleInfiniteHandler(MetaMachine holder) {
            super(holder, 1, IO.OUT, IO.NONE, ItemStackHandlerDelegate::new);
            internalBuffer.setOnContentsChanged(this::onContentsChanged);
        }

        @Override
        public @NotNull List<Object> getContents() {
            return Collections.emptyList();
        }

        @Override
        public double getTotalContentAmount() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }
    }

    @NoArgsConstructor
    private class ItemStackHandlerDelegate extends CustomItemStackHandler {

        public ItemStackHandlerDelegate(Integer integer) {
            super();
        }

        @Override
        public int getSlots() {
            return Short.MAX_VALUE;
        }

        @Override
        public int getSlotLimit(int slot) {
            return Integer.MAX_VALUE;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {}

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            var key = AEItemKey.of(stack);
            int count = stack.getCount();
            long oldValue = internalBuffer.storage.getOrDefault(key, 0);
            long changeValue = Math.min(Long.MAX_VALUE - oldValue, count);
            if (changeValue > 0) {
                if (!simulate) {
                    internalBuffer.storage.put(key, oldValue + changeValue);
                    internalBuffer.onChanged();
                }
                return stack.copyWithCount((int) (count - changeValue));
            } else {
                return ItemStack.EMPTY;
            }
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }
    }

    private class InaccessibleInfiniteTank extends NotifiableFluidTank {

        FluidStorageDelegate storage;

        public InaccessibleInfiniteTank(MetaMachine holder) {
            super(holder, List.of(new FluidStorageDelegate()), IO.OUT, IO.NONE);
            internalFluidBuffer.setOnContentsChanged(this::onContentsChanged);
            storage = (FluidStorageDelegate) getStorages()[0];
            allowSameFluids = true;
        }

        @Override
        public int getTanks() {
            return 128;
        }

        @Override
        public @NotNull List<Object> getContents() {
            return Collections.emptyList();
        }

        @Override
        public double getTotalContentAmount() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public void setFluidInTank(int tank, @NotNull FluidStack fluidStack) {}

        @Override
        public int getTankCapacity(int tank) {
            return storage.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return true;
        }

        @Override
        @Nullable
        public List<FluidIngredient> handleRecipeInner(IO io, GTRecipe recipe, List<FluidIngredient> left,
                                                       boolean simulate) {
            if (io != IO.OUT) return left;
            FluidAction action = simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE;
            for (var it = left.iterator(); it.hasNext();) {
                var ingredient = it.next();
                if (ingredient.isEmpty()) {
                    it.remove();
                    continue;
                }

                var fluids = ingredient.getStacks();
                if (fluids.length == 0 || fluids[0].isEmpty()) {
                    it.remove();
                    continue;
                }

                FluidStack output = fluids[0];
                ingredient.shrink(storage.fill(output, action));
                if (ingredient.getAmount() <= 0) it.remove();
            }
            return left.isEmpty() ? null : left;
        }
    }

    private class FluidStorageDelegate extends CustomFluidTank {

        public FluidStorageDelegate() {
            super(0);
        }

        @Override
        public int getCapacity() {
            return Integer.MAX_VALUE;
        }

        @Override
        public void setFluid(FluidStack fluid) {}

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            var key = AEFluidKey.of(resource.getFluid(), resource.getTag());
            int amount = resource.getAmount();
            int oldValue = GTMath.saturatedCast(internalFluidBuffer.storage.getOrDefault(key, 0));
            int changeValue = Math.min(Integer.MAX_VALUE - oldValue, amount);
            if (changeValue > 0 && action.execute()) {
                internalFluidBuffer.storage.put(key, oldValue + changeValue);
                internalFluidBuffer.onChanged();
            }
            return changeValue;
        }

        @Override
        public boolean supportsFill(int tank) {
            return false;
        }

        @Override
        public boolean supportsDrain(int tank) {
            return false;
        }
    }
}
