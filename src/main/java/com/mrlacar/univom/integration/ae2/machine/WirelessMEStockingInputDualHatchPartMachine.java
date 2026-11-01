package com.mrlacar.univom.integration.ae2.machine;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.ae2.slot.*;
import com.gregtechceu.gtceu.integration.ae2.utils.AEUtil;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.DropSaved;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mrlacar.univom.api.machine.fancyconfigurator.AutoStockingDualFancyConfigurator;
import com.mrlacar.univom.integration.ae2.machine.feature.multiblock.IMEStockingDualPart;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WirelessMEStockingInputDualHatchPartMachine extends WirelessMEInputDualHatchPartMachine implements IMEStockingDualPart {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            WirelessMEStockingInputDualHatchPartMachine.class, WirelessMEInputDualHatchPartMachine.MANAGED_FIELD_HOLDER);

    @DescSynced
    @Persisted
    @Getter
    private boolean autoPull;

    @Getter
    @Persisted
    @DropSaved
    private int minStackSize = 1;

    @Getter
    @Persisted
    @DropSaved
    private int fluidMinStackSize = 1;

    @Getter
    @Persisted
    @DropSaved
    private int ticksPerCycle = 40;

    @Setter
    private Predicate<GenericStack> autoPullTest;

    public WirelessMEStockingInputDualHatchPartMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
        this.autoPullTest = $ -> false;
    }

    @Override
    public void setMinStackSize(int newSize) {
        this.minStackSize = Math.max(1, newSize);
    }

    @Override
    public void setFluidMinStackSize(int newSize) {
        this.fluidMinStackSize = Math.max(1, newSize);
    }

    @Override
    public void setTicksPerCycle(int newSize) {
        this.ticksPerCycle = Math.max(minimumTicksPerCycle(), newSize);
    }

    private static int minimumTicksPerCycle() {
        return Math.max(1, ConfigHolder.INSTANCE.compat.ae2.updateIntervals);
    }

    @Override
    public void addedToController(IMultiController controller) {
        super.addedToController(controller);
        IMEStockingDualPart.super.addedToController(controller);
    }

    @Override
    public void removedFromController(IMultiController controller) {
        setAutoPullTest($ -> false);
        if (isAutoPull()) {
            aeItemHandler.clearInventory(0);
            aeFluidHandler.clearInventory(0);
        }
        super.removedFromController(controller);
    }

    @Override
    protected NotifiableItemStackHandler createInventory(Object... args) {
        this.aeItemHandler = new ExportOnlyAEStockingItemList(this, CONFIG_SIZE);
        return this.aeItemHandler;
    }

    @Override
    protected ExportOnlyAEFluidList createFluidHandler(int slots) {
        return new ExportOnlyAEStockingFluidList(this, slots);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void autoIO() {
        super.autoIO();
        if (ticksPerCycle < minimumTicksPerCycle()) ticksPerCycle = minimumTicksPerCycle();
        if (getOffsetTimer() % ticksPerCycle == 0) {
            if (autoPull) {
                refreshList();
            }
            if (updateMEStatus()) {
                syncME();
            }
        }
    }

    @Override
    protected void syncME() {
        IGrid grid = this.getMainNode().getGrid();
        if (grid == null) return;
        MEStorage networkInv = grid.getStorageService().getInventory();

        for (ExportOnlyAEItemSlot slot : this.aeItemHandler.getInventory()) {
            var config = slot.getConfig();
            if (config != null) {
                var key = config.what();
                long extracted = networkInv.extract(key, Long.MAX_VALUE, Actionable.SIMULATE, actionSource);
                if (extracted >= minStackSize) {
                    slot.setStock(new GenericStack(key, extracted));
                    continue;
                }
            }
            slot.setStock(null);
        }

        for (ExportOnlyAEFluidSlot tank : this.aeFluidHandler.getInventory()) {
            var config = tank.getConfig();
            if (config != null) {
                var key = config.what();
                long extracted = networkInv.extract(key, Long.MAX_VALUE, Actionable.SIMULATE, actionSource);
                if (extracted >= fluidMinStackSize) {
                    tank.setStock(new GenericStack(key, extracted));
                    continue;
                }
            }
            tank.setStock(null);
        }
    }

    @Override
    public void attachSideTabs(TabsWidget sideTabs) {
        super.attachSideTabs(sideTabs);
        sideTabs.setMainTab(this);
    }

    @Override
    protected void flushInventory() {
    }

    @Override
    public void setDistinct(boolean isDistinct) {
        super.setDistinct(isDistinct);
        if (!isRemote() && !isDistinct) {
            validateConfig();
        }
    }

    @Override
    public IConfigurableSlotList getSlotList() {
        return aeItemHandler;
    }

    @Override
    public void validateConfig() {
        validateSlotList(aeItemHandler);
        validateSlotList(aeFluidHandler);
    }

    private void validateSlotList(IConfigurableSlotList slots) {
        for (int i = 0; i < slots.getConfigurableSlots(); i++) {
            var slot = slots.getConfigurableSlot(i);
            if (slot.getConfig() != null) {
                GenericStack configuredStack = slot.getConfig();
                if (testConfiguredInOtherPart(configuredStack)) {
                    slot.setConfig(null);
                    slot.setStock(null);
                }
            }
        }
    }

    @Override
    public boolean testConfiguredInOtherPart(@Nullable GenericStack config) {
        if (config == null) return false;
        if (!isFormed() || isDistinct()) return false;

        for (IMultiController controller : getControllers()) {
            for (IMultiPart part : controller.getParts()) {
                if (part instanceof WirelessMEStockingInputDualHatchPartMachine machine) {
                    if (machine == this || machine.isDistinct()) continue;
                    if (machine.aeItemHandler.hasStackInConfig(config, false)) {
                        return true;
                    }
                    if (machine.aeFluidHandler.hasStackInConfig(config, false)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void setAutoPull(boolean autoPull) {
        this.autoPull = autoPull;
        if (!isRemote()) {
            if (!this.autoPull) {
                this.aeItemHandler.clearInventory(0);
                this.aeFluidHandler.clearInventory(0);
            } else if (updateMEStatus()) {
                this.refreshList();
                updateInventorySubscription();
            }
        }
    }

    private void refreshList() {
        IGrid grid = this.getMainNode().getGrid();
        if (grid == null) {
            aeItemHandler.clearInventory(0);
            aeFluidHandler.clearInventory(0);
            return;
        }

        MEStorage networkStorage = grid.getStorageService().getInventory();
        var counter = networkStorage.getAvailableStacks();

        PriorityQueue<Object2LongMap.Entry<AEKey>> topItems = new PriorityQueue<>(
                Comparator.comparingLong(Object2LongMap.Entry<AEKey>::getLongValue));
        PriorityQueue<Object2LongMap.Entry<AEKey>> topFluids = new PriorityQueue<>(
                Comparator.comparingLong(Object2LongMap.Entry<AEKey>::getLongValue));

        for (Object2LongMap.Entry<AEKey> entry : counter) {
            long amount = entry.getLongValue();
            AEKey what = entry.getKey();

            if (amount <= 0) continue;

            PriorityQueue<Object2LongMap.Entry<AEKey>> target;
            if (what instanceof AEItemKey) {
                if (amount < minStackSize) continue;
                target = topItems;
            } else if (what instanceof AEFluidKey) {
                if (amount < fluidMinStackSize) continue;
                target = topFluids;
            } else {
                continue;
            }

            long request = networkStorage.extract(what, amount, Actionable.SIMULATE, actionSource);
            if (request == 0) continue;

            if (autoPullTest != null && !autoPullTest.test(new GenericStack(what, amount))) continue;

            if (target.size() < CONFIG_SIZE) {
                target.offer(entry);
            } else if (amount > target.peek().getLongValue()) {
                target.poll();
                target.offer(entry);
            }
        }

        int index;
        int itemAmount = topItems.size();
        for (index = 0; index < CONFIG_SIZE; index++) {
            if (topItems.isEmpty()) break;
            Object2LongMap.Entry<AEKey> entry = topItems.poll();

            AEKey what = entry.getKey();
            long amount = entry.getLongValue();

            long request = networkStorage.extract(what, amount, Actionable.SIMULATE, actionSource);

            var slot = this.aeItemHandler.getInventory()[itemAmount - index - 1];
            slot.setConfig(new GenericStack(what, 1));
            slot.setStock(new GenericStack(what, request));
        }
        aeItemHandler.clearInventory(index);

        int fluidAmount = topFluids.size();
        for (index = 0; index < CONFIG_SIZE; index++) {
            if (topFluids.isEmpty()) break;
            Object2LongMap.Entry<AEKey> entry = topFluids.poll();

            AEKey what = entry.getKey();
            long amount = entry.getLongValue();

            long request = networkStorage.extract(what, amount, Actionable.SIMULATE, actionSource);

            var tank = this.aeFluidHandler.getInventory()[fluidAmount - index - 1];
            tank.setConfig(new GenericStack(what, 1));
            tank.setStock(new GenericStack(what, request));
        }
        aeFluidHandler.clearInventory(index);
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        IMEStockingDualPart.super.attachConfigurators(configuratorPanel);
        super.attachConfigurators(configuratorPanel);
        configuratorPanel.attachConfigurators(new AutoStockingDualFancyConfigurator(this));
    }

    @Override
    protected InteractionResult onScrewdriverClick(Player playerIn, InteractionHand hand, Direction gridSide,
                                                   BlockHitResult hitResult) {
        if (!isRemote()) {
            setAutoPull(!autoPull);
            if (autoPull) {
                playerIn.sendSystemMessage(
                        Component.translatable("gtceu.machine.me.stocking_auto_pull_enabled"));
            } else {
                playerIn.sendSystemMessage(
                        Component.translatable("gtceu.machine.me.stocking_auto_pull_disabled"));
            }
        }
        return InteractionResult.sidedSuccess(isRemote());
    }

    @Override
    protected CompoundTag writeConfigToTag() {
        if (!autoPull) {
            CompoundTag tag = super.writeConfigToTag();
            tag.putBoolean("AutoPull", false);
            return tag;
        }
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("AutoPull", true);
        tag.putByte("GhostCircuit",
                (byte) IntCircuitBehaviour.getCircuitConfiguration(circuitInventory.getStackInSlot(0)));
        return tag;
    }

    @Override
    protected void readConfigFromTag(CompoundTag tag) {
        if (tag.getBoolean("AutoPull")) {
            this.setAutoPull(true);
            circuitInventory.setStackInSlot(0, IntCircuitBehaviour.stack(tag.getByte("GhostCircuit")));
            return;
        }
        this.setAutoPull(false);
        super.readConfigFromTag(tag);
    }

    private class ExportOnlyAEStockingItemList extends ExportOnlyAEItemList {

        public ExportOnlyAEStockingItemList(MetaMachine holder, int slots) {
            super(holder, slots, ExportOnlyAEStockingItemSlot::new);
        }

        @Override
        public boolean isAutoPull() {
            return autoPull;
        }

        @Override
        public boolean isStocking() {
            return true;
        }

        @Override
        public boolean hasStackInConfig(GenericStack stack, boolean checkExternal) {
            boolean inThisPart = super.hasStackInConfig(stack, false);
            if (inThisPart) return true;
            if (checkExternal) {
                return testConfiguredInOtherPart(stack);
            }
            return false;
        }
    }

    private class ExportOnlyAEStockingItemSlot extends ExportOnlyAEItemSlot {

        public ExportOnlyAEStockingItemSlot() {
            super();
        }

        public ExportOnlyAEStockingItemSlot(@Nullable GenericStack config, @Nullable GenericStack stock) {
            super(config, stock);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == 0 && this.stock != null) {
                if (this.config != null) {
                    if (!isOnline()) return ItemStack.EMPTY;
                    IGrid grid = getMainNode().getGrid();
                    if (grid == null) return ItemStack.EMPTY;
                    MEStorage aeNetwork = grid.getStorageService().getInventory();

                    Actionable action = simulate ? Actionable.SIMULATE : Actionable.MODULATE;
                    var key = config.what();
                    long extracted = aeNetwork.extract(key, amount, action, actionSource);

                    if (extracted > 0) {
                        ItemStack resultStack = key instanceof AEItemKey itemKey ?
                                itemKey.toStack((int) extracted) : ItemStack.EMPTY;
                        if (!simulate) {
                            this.stock = ExportOnlyAESlot.copy(stock, stock.amount() - extracted);
                            if (this.stock.amount() == 0) {
                                this.stock = null;
                            }
                            if (this.onContentsChanged != null) {
                                this.onContentsChanged.run();
                            }
                        }
                        return resultStack;
                    }
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ExportOnlyAEStockingItemSlot copy() {
            return new ExportOnlyAEStockingItemSlot(
                    this.config == null ? null : copy(this.config),
                    this.stock == null ? null : copy(this.stock));
        }
    }

    private class ExportOnlyAEStockingFluidList extends ExportOnlyAEFluidList {

        public ExportOnlyAEStockingFluidList(MetaMachine holder, int slots) {
            super(holder, slots, ExportOnlyAEStockingFluidSlot::new);
        }

        @Override
        public boolean isAutoPull() {
            return autoPull;
        }

        @Override
        public boolean isStocking() {
            return true;
        }

        @Override
        public boolean hasStackInConfig(GenericStack stack, boolean checkExternal) {
            boolean inThisPart = super.hasStackInConfig(stack, false);
            if (inThisPart) return true;
            if (checkExternal) {
                return testConfiguredInOtherPart(stack);
            }
            return false;
        }
    }

    private class ExportOnlyAEStockingFluidSlot extends ExportOnlyAEFluidSlot {

        public ExportOnlyAEStockingFluidSlot() {
            super();
        }

        public ExportOnlyAEStockingFluidSlot(@Nullable GenericStack config, @Nullable GenericStack stock) {
            super(config, stock);
        }

        @Override
        public ExportOnlyAEFluidSlot copy() {
            return new ExportOnlyAEStockingFluidSlot(
                    this.config == null ? null : copy(this.config),
                    this.stock == null ? null : copy(this.stock));
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (this.stock != null && this.config != null) {
                if (!isOnline()) return FluidStack.EMPTY;
                IGrid grid = getMainNode().getGrid();
                if (grid == null) return FluidStack.EMPTY;
                MEStorage aeNetwork = grid.getStorageService().getInventory();

                Actionable actionable = action.simulate() ? Actionable.SIMULATE : Actionable.MODULATE;
                var key = config.what();
                long extracted = aeNetwork.extract(key, maxDrain, actionable, actionSource);

                if (extracted > 0) {
                    FluidStack resultStack = key instanceof AEFluidKey fluidKey ?
                            AEUtil.toFluidStack(fluidKey, extracted) : FluidStack.EMPTY;
                    if (action.execute()) {
                        this.stock = ExportOnlyAESlot.copy(stock, stock.amount() - extracted);
                        if (this.stock.amount() == 0) {
                            this.stock = null;
                        }
                        if (this.onContentsChanged != null) {
                            this.onContentsChanged.run();
                        }
                    }
                    return resultStack;
                }
            }
            return FluidStack.EMPTY;
        }
    }
}
