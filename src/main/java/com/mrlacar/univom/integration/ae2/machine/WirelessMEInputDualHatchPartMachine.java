package com.mrlacar.univom.integration.ae2.machine;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.feature.IHasCircuitSlot;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.AEFluidConfigWidget;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.AEItemConfigWidget;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidList;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidSlot;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemList;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemSlot;
import com.gregtechceu.gtceu.utils.GTMath;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.Position;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WirelessMEInputDualHatchPartMachine extends WirelessMEBusPartMachine implements IDataStickInteractable, IMachineLife, IHasCircuitSlot {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            WirelessMEInputDualHatchPartMachine.class, WirelessMEBusPartMachine.MANAGED_FIELD_HOLDER);

    protected final static int CONFIG_SIZE = 16;

    protected final static int SECTION_HEIGHT = 18 * 4 + 2;
    protected final static int SECTION_SPACING = 10;

    protected ExportOnlyAEItemList aeItemHandler;

    @Persisted
    protected ExportOnlyAEFluidList aeFluidHandler;

    public WirelessMEInputDualHatchPartMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, IO.IN, args);
        this.aeFluidHandler = createFluidHandler(CONFIG_SIZE);
    }

    @Override
    public void onMachineRemoved() {
        flushInventory();
        super.onMachineRemoved();
    }

    @Override
    protected NotifiableItemStackHandler createInventory(Object... args) {
        this.aeItemHandler = new ExportOnlyAEItemList(this, CONFIG_SIZE);
        return this.aeItemHandler;
    }

    protected ExportOnlyAEFluidList createFluidHandler(int slots) {
        return new ExportOnlyAEFluidList(this, slots);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void autoIO() {
        if (!this.isWorkingEnabled()) return;
        if (!this.shouldSyncME()) return;

        if (this.updateMEStatus()) {
            this.syncME();
            this.updateInventorySubscription();
        }
    }

    protected void syncME() {
        IGrid grid = this.getMainNode().getGrid();
        if (grid == null) return;
        MEStorage networkInv = grid.getStorageService().getInventory();
        syncItems(networkInv);
        syncFluids(networkInv);
    }

    protected void syncItems(MEStorage networkInv) {
        for (ExportOnlyAEItemSlot aeSlot : this.aeItemHandler.getInventory()) {
            GenericStack exceedItem = aeSlot.exceedStack();
            if (exceedItem != null) {
                long total = exceedItem.amount();
                long inserted = networkInv.insert(exceedItem.what(), exceedItem.amount(), Actionable.MODULATE,
                        this.actionSource);
                if (inserted > 0) {
                    aeSlot.extractItem(0, GTMath.saturatedCast(inserted), false);
                    continue;
                } else {
                    aeSlot.extractItem(0, GTMath.saturatedCast(total), false);
                }
            }
            GenericStack reqItem = aeSlot.requestStack();
            if (reqItem != null) {
                long extracted = networkInv.extract(reqItem.what(), reqItem.amount(), Actionable.MODULATE,
                        this.actionSource);
                if (extracted != 0) {
                    aeSlot.addStack(new GenericStack(reqItem.what(), extracted));
                }
            }
        }
    }

    protected void syncFluids(MEStorage networkInv) {
        for (ExportOnlyAEFluidSlot aeTank : this.aeFluidHandler.getInventory()) {
            GenericStack exceedFluid = aeTank.exceedStack();
            if (exceedFluid != null) {
                int total = GTMath.saturatedCast(exceedFluid.amount());
                int inserted = GTMath
                        .saturatedCast(networkInv.insert(exceedFluid.what(), exceedFluid.amount(), Actionable.MODULATE,
                                this.actionSource));
                if (inserted > 0) {
                    aeTank.drain(inserted, IFluidHandler.FluidAction.EXECUTE);
                    continue;
                } else {
                    aeTank.drain(total, IFluidHandler.FluidAction.EXECUTE);
                }
            }
            GenericStack reqFluid = aeTank.requestStack();
            if (reqFluid != null) {
                long extracted = networkInv.extract(reqFluid.what(), reqFluid.amount(), Actionable.MODULATE,
                        this.actionSource);
                if (extracted > 0) {
                    aeTank.addStack(new GenericStack(reqFluid.what(), extracted));
                }
            }
        }
    }

    protected void flushInventory() {
        var grid = getMainNode().getGrid();
        if (grid != null) {
            MEStorage networkInv = grid.getStorageService().getInventory();
            for (var aeSlot : aeItemHandler.getInventory()) {
                GenericStack stock = aeSlot.getStock();
                if (stock != null) {
                    networkInv.insert(stock.what(), stock.amount(), Actionable.MODULATE, actionSource);
                }
            }
            for (var aeTank : aeFluidHandler.getInventory()) {
                GenericStack stock = aeTank.getStock();
                if (stock != null) {
                    networkInv.insert(stock.what(), stock.amount(), Actionable.MODULATE, actionSource);
                }
            }
        }
    }

    @Override
    public Widget createUIWidget() {
        WidgetGroup group = new WidgetGroup(new Position(0, 0));
        group.addWidget(new LabelWidget(3, 0, () -> this.isOnline ?
                "gtceu.gui.me_network.online" :
                "gtceu.gui.me_network.offline"));

        group.addWidget(new AEItemConfigWidget(3, SECTION_SPACING, this.aeItemHandler));
        group.addWidget(new AEFluidConfigWidget(3, SECTION_SPACING + SECTION_HEIGHT + SECTION_SPACING,
                this.aeFluidHandler));

        return group;
    }

    @Override
    public final InteractionResult onDataStickShiftUse(Player player, ItemStack dataStick) {
        if (!isRemote()) {
            CompoundTag tag = new CompoundTag();
            tag.put("MEDualInput", writeConfigToTag());
            dataStick.setTag(tag);
            dataStick.setHoverName(Component.translatable("univom.machine.me.dual_import.data_stick.name"));
            player.sendSystemMessage(Component.translatable("gtceu.machine.me.import_copy_settings"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public final InteractionResult onDataStickUse(Player player, ItemStack dataStick) {
        CompoundTag tag = dataStick.getTag();
        if (tag == null || !tag.contains("MEDualInput")) {
            return InteractionResult.PASS;
        }

        if (!isRemote()) {
            readConfigFromTag(tag.getCompound("MEDualInput"));
            this.updateInventorySubscription();
            player.sendSystemMessage(Component.translatable("gtceu.machine.me.import_paste_settings"));
        }
        return InteractionResult.sidedSuccess(isRemote());
    }

    protected CompoundTag writeConfigToTag() {
        CompoundTag tag = new CompoundTag();
        CompoundTag configStacks = new CompoundTag();
        tag.put("ConfigStacks", configStacks);
        for (int i = 0; i < CONFIG_SIZE; i++) {
            var slot = this.aeItemHandler.getInventory()[i];
            GenericStack config = slot.getConfig();
            if (config == null) {
                continue;
            }
            CompoundTag stackTag = GenericStack.writeTag(config);
            configStacks.put(Integer.toString(i), stackTag);
        }
        CompoundTag fluidConfigStacks = new CompoundTag();
        tag.put("FluidConfigStacks", fluidConfigStacks);
        for (int i = 0; i < CONFIG_SIZE; i++) {
            var tank = this.aeFluidHandler.getInventory()[i];
            GenericStack config = tank.getConfig();
            if (config == null) {
                continue;
            }
            CompoundTag stackTag = GenericStack.writeTag(config);
            fluidConfigStacks.put(Integer.toString(i), stackTag);
        }
        tag.putByte("GhostCircuit",
                (byte) IntCircuitBehaviour.getCircuitConfiguration(circuitInventory.getStackInSlot(0)));
        tag.putBoolean("DistinctBuses", isDistinct());
        return tag;
    }

    protected void readConfigFromTag(CompoundTag tag) {
        if (tag.contains("ConfigStacks")) {
            CompoundTag configStacks = tag.getCompound("ConfigStacks");
            for (int i = 0; i < CONFIG_SIZE; i++) {
                String key = Integer.toString(i);
                if (configStacks.contains(key)) {
                    CompoundTag configTag = configStacks.getCompound(key);
                    this.aeItemHandler.getInventory()[i].setConfig(GenericStack.readTag(configTag));
                } else {
                    this.aeItemHandler.getInventory()[i].setConfig(null);
                }
            }
        }
        if (tag.contains("FluidConfigStacks")) {
            CompoundTag fluidConfigStacks = tag.getCompound("FluidConfigStacks");
            for (int i = 0; i < CONFIG_SIZE; i++) {
                String key = Integer.toString(i);
                if (fluidConfigStacks.contains(key)) {
                    CompoundTag configTag = fluidConfigStacks.getCompound(key);
                    this.aeFluidHandler.getInventory()[i].setConfig(GenericStack.readTag(configTag));
                } else {
                    this.aeFluidHandler.getInventory()[i].setConfig(null);
                }
            }
        }
        if (tag.contains("GhostCircuit")) {
            circuitInventory.setStackInSlot(0, IntCircuitBehaviour.stack(tag.getByte("GhostCircuit")));
        }
        if (tag.contains("DistinctBuses")) {
            setDistinct(tag.getBoolean("DistinctBuses"));
        }
    }
}
