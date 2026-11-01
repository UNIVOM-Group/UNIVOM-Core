package com.mrlacar.univom.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.item.component.IItemComponent;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mrlacar.univom.api.machine.fancyconfigurator.ControlRodBusConfigurator;
import com.mrlacar.univom.common.item.ControlRodBehaviour;
import com.mrlacar.univom.data.recipes.ModCustomTags;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.gregtechceu.gtceu.api.GTValues.EV;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ControlRodBusPartMachine extends SimpleItemBusPartMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            ControlRodBusPartMachine.class, SimpleItemBusPartMachine.MANAGED_FIELD_HOLDER);

    @Getter
    @Persisted
    private final NotifiableItemStackHandler inventory;

    @Getter
    @Persisted
    private double controlRodInsertion = 0.5;

    @Persisted
    private double controlRodThresholdValue = 0.5;

    @Getter
    @Persisted
    private boolean isRedstonePowered = false;

    private double cachedEfficiency = 0.5;

    public ControlRodBusPartMachine(IMachineBlockEntity holder) {
        super(holder, EV, IO.IN);
        this.inventory = new NotifiableItemStackHandler(this, 1, IO.BOTH);
        this.inventory.setFilter(ItemStack -> ItemStack.is(ModCustomTags.CONTROL_ROD));
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateRedstoneState();
        updateEfficiencyCache();
        this.inventory.addChangedListener(this::updateEfficiencyCache);
    }

    @Override
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChanged(block, fromPos, isMoving);
        updateRedstoneState();
    }

    private void updateRedstoneState() {
        if (getLevel() != null && !getLevel().isClientSide) {
            this.isRedstonePowered = getLevel().hasNeighborSignal(getPos());
        }
    }

    private void updateEfficiencyCache() {
        if (noControlRod()) {
            this.cachedEfficiency = 0.0;
            return;
        }
        ItemStack stack = this.inventory.getStackInSlot(0);
        if (stack.getItem() instanceof ComponentItem componentItem) {
            for (IItemComponent component : componentItem.getComponents()) {
                if (component instanceof ControlRodBehaviour controlRodBehaviour) {
                    this.cachedEfficiency = controlRodBehaviour.controlRodEfficiency;
                    return;
                }
            }
        }
        this.cachedEfficiency = 0.5;
    }

    public int getControlRodPercent() {
        return (int) (this.controlRodInsertion * 100);
    }

    public void setControlRodPercent(int percent) {
        this.controlRodInsertion = Math.max(0.0, Math.min(1.0, percent / 100.0));
    }

    public int getControlRodThresholdValue() {
        return (int) (this.controlRodThresholdValue * 100);
    }

    public void setControlRodThresholdValue(int percent) {
        this.controlRodThresholdValue = Math.max(0.0, Math.min(1.0, percent / 100.0));
    }

    public boolean noControlRod() {
        return this.inventory.getStackInSlot(0).isEmpty();
    }

    public double getControlRodEfficiency() {
        return this.cachedEfficiency;
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        super.attachConfigurators(configuratorPanel);
        configuratorPanel.attachConfigurators(new ControlRodBusConfigurator(this));
    }

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 18 + 16, 18 + 16);
        var container = new WidgetGroup(4, 4, 18 + 8, 18 + 8);
        container.addWidget(new SlotWidget(this.inventory.storage, 0, 4, 4, true, true).setBackground(GuiTextures.SLOT));
        container.setBackground(GuiTextures.BACKGROUND_INVERSE);
        group.addWidget(container);

        group.addWidget(new IntInputWidget(-23, 34, 81, 14, this::getControlRodPercent, this::setControlRodPercent)
                .setMin(0)
                .setMax(100));
        return group;
    }
}