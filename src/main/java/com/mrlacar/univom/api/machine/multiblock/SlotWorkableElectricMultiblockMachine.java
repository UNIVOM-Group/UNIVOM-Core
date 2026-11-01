package com.mrlacar.univom.api.machine.multiblock;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.Size;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SlotWorkableElectricMultiblockMachine extends WorkableElectricMultiblockMachine implements IMachineLife {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            SlotWorkableElectricMultiblockMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    @Getter
    @DescSynced
    @Persisted
    private final NotifiableItemStackHandler storageSlot;

    public SlotWorkableElectricMultiblockMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
        this.storageSlot = new NotifiableItemStackHandler(this, 1, IO.NONE, IO.BOTH);
    }

    protected void setSlotFilter(Predicate<ItemStack> filter) {
        this.storageSlot.setFilter(filter);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public Widget createUIWidget() {
        Widget widget = super.createUIWidget();
        if (widget instanceof WidgetGroup group) {
            Size size = group.getSize();
            group.addWidget(new SlotWidget(this.storageSlot.storage, 0, size.width - 25, size.height - 25, true, true)
                    .setBackground(GuiTextures.SLOT));

            group.setBackground(GuiTextures.BACKGROUND_INVERSE);
        }
        return widget;
    }

    @Override
    public void onMachineRemoved() {
        this.clearInventory(this.storageSlot.storage);
    }
}
