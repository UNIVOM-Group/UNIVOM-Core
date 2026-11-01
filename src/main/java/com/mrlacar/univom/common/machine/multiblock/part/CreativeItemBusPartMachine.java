package com.mrlacar.univom.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.IPaintable;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.widget.PhantomSlotWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CircuitFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.IHasCircuitSlot;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDistinctPart;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mrlacar.univom.api.transfer.item.CreativeItemStackTransfer;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeItemBusPartMachine extends TieredIOPartMachine implements IDistinctPart, IPaintable, IHasCircuitSlot, IMachineLife {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(CreativeItemBusPartMachine.class,
            TieredIOPartMachine.MANAGED_FIELD_HOLDER);

    @Getter
    @Persisted
    private final NotifiableItemStackHandler inventory;

    @Getter
    @Setter
    @Persisted
    @DescSynced
    protected boolean circuitSlotEnabled;

    @Getter
    @Persisted
    protected final NotifiableItemStackHandler circuitInventory;

    public CreativeItemBusPartMachine(IMachineBlockEntity holder) {
        super(holder, GTValues.MAX, IO.IN);

        this.inventory = new NotifiableItemStackHandler(this, 16, IO.IN, IO.IN, CreativeItemStackTransfer::new);

        this.circuitSlotEnabled = true;
        this.circuitInventory = new NotifiableItemStackHandler(this, 1, IO.IN, IO.NONE)
                .setFilter(IntCircuitBehaviour::isIntegratedCircuit).shouldSearchContent(false);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onMachineRemoved() {
        if (!ConfigHolder.INSTANCE.machines.ghostCircuit) {
            clearInventory(circuitInventory.storage);
        }
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

    @Override
    public boolean isDistinct() {
        return getInventory().isDistinct() && circuitInventory.isDistinct();
    }

    @Override
    public void setDistinct(boolean isDistinct) {
        getInventory().setDistinct(isDistinct);
        circuitInventory.setDistinct(isDistinct);
    }

    @Override
    public void addedToController(IMultiController controller) {
        if (!controller.allowCircuitSlots()) {
            if (!ConfigHolder.INSTANCE.machines.ghostCircuit) {
                clearInventory(circuitInventory.storage);
            } else {
                circuitInventory.setStackInSlot(0, ItemStack.EMPTY);
            }
            setCircuitSlotEnabled(false);
        }
        super.addedToController(controller);
    }

    @Override
    public void removedFromController(IMultiController controller) {
        super.removedFromController(controller);
        for (var c : controllers) {
            if (!c.allowCircuitSlots()) {
                return;
            }
        }
        setCircuitSlotEnabled(true);
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        IDistinctPart.super.attachConfigurators(configuratorPanel);
        if (isCircuitSlotEnabled()) {
            configuratorPanel.attachConfigurators(new CircuitFancyConfigurator(circuitInventory.storage));
        }
    }

    @Override
    public Widget createUIWidget() {
        int rowSize = 4;
        int colSize = 4;
        var group = new WidgetGroup(0, 0, 18 * rowSize + 16, 18 * colSize + 16);
        var container = new WidgetGroup(4, 4, 18 * rowSize + 8, 18 * colSize + 8);

        CustomItemStackHandler uiStorage = new CustomItemStackHandler(16) {
            @Override
            public @NotNull ItemStack getStackInSlot(int slot) {
                ItemStack stack = inventory.storage.getStackInSlot(slot);
                if (stack.isEmpty()) return ItemStack.EMPTY;
                ItemStack copy = stack.copy();
                copy.setCount(1);
                return copy;
            }

            @Override
            public void setStackInSlot(int slot, @NotNull ItemStack stack) {
                inventory.storage.setStackInSlot(slot, stack);
            }
        };

        int index = 0;
        for (int y = 0; y < colSize; y++) {
            for (int x = 0; x < rowSize; x++) {
                int finalIndex = index++;
                container.addWidget(
                        new PhantomSlotWidget(uiStorage, finalIndex, 4 + x * 18, 4 + y * 18) {
                            @Override
                            public ItemStack slotClickPhantom(Slot slot, int mouseButton, ClickType clickTypeIn, ItemStack stackHeld) {
                                if (stackHeld.isEmpty() || mouseButton == 2 || mouseButton == 1) {
                                    uiStorage.setStackInSlot(finalIndex, ItemStack.EMPTY);
                                } else {
                                    ItemStack ghost = stackHeld.copy();
                                    ghost.setCount(1);
                                    uiStorage.setStackInSlot(finalIndex, ghost);
                                }
                                CreativeItemBusPartMachine.this.markDirty();
                                return stackHeld;
                            }
                        }.setBackground(GuiTextures.SLOT)
                );
            }
        }

        container.setBackground(GuiTextures.BACKGROUND_INVERSE);
        group.addWidget(container);

        return group;
    }
}