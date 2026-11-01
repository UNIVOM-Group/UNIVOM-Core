package com.mrlacar.univom.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.item.component.IItemComponent;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mrlacar.univom.common.item.FuelRodBehaviour;
import com.mrlacar.univom.data.recipes.ModCustomTags;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.gregtechceu.gtceu.api.GTValues.EV;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FuelRodBusPartMachine extends SimpleItemBusPartMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            FuelRodBusPartMachine.class, SimpleItemBusPartMachine.MANAGED_FIELD_HOLDER);

    @Getter
    @Persisted
    private final NotifiableItemStackHandler inventory;

    private final FuelRodBehaviour[] cachedBehaviors = new FuelRodBehaviour[16];

    public record FuelHeatResult(double totalHeat, int activeCount) {}

    public FuelRodBusPartMachine(IMachineBlockEntity holder) {
        super(holder, EV, IO.BOTH);
        this.inventory = new NotifiableItemStackHandler(this, 16, IO.BOTH);
        this.inventory.setFilter(ItemStack -> ItemStack.is(ModCustomTags.FUEL_ROD));
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateBehaviorCache();
        this.inventory.addChangedListener(this::updateBehaviorCache);
    }

    private void updateBehaviorCache() {
        for (int i = 0; i < 16; i++) {
            ItemStack stack = this.inventory.getStackInSlot(i);
            this.cachedBehaviors[i] = null;
            if (!stack.isEmpty() && stack.getItem() instanceof ComponentItem componentItem) {
                for (IItemComponent component : componentItem.getComponents()) {
                    if (component instanceof FuelRodBehaviour fuelRodBehaviour) {
                        this.cachedBehaviors[i] = fuelRodBehaviour;
                        break;
                    }
                }
            }
        }
    }

    public FuelHeatResult processFuelAndGetHeat(double effectiveInsertion) {
        double totalBaseHeat = 0.0;
        int activeCount = 0;

        double fissionRate = Math.max(0.0, 1.0 - effectiveInsertion);
        boolean isSecondTick = (this.getOffsetTimer() % 20 == 0);

        int consumeAmount = (int) Math.round(20.0 * fissionRate);

        if (consumeAmount == 0 && fissionRate > 0) consumeAmount = 1;

        boolean durabilityChanged = false;
        int slots = getInventory().getSlots();
        for (int i = 0; i < slots; i++) {
            FuelRodBehaviour behaviour = this.cachedBehaviors[i];

            if (behaviour != null && behaviour.heat > 0) {
                ItemStack stack = getInventory().getStackInSlot(i);
                if (stack.isEmpty()) continue;

                totalBaseHeat += behaviour.heat;
                activeCount++;

                if (isSecondTick && consumeAmount > 0) {
                    if (!behaviour.consumeDurability(stack, consumeAmount)) {
                        getInventory().setStackInSlot(i, behaviour.getDepletedStack());
                    } else {
                        durabilityChanged = true;
                    }
                }
            }
        }

        if (durabilityChanged) {
            getInventory().onContentsChanged();
        }
        return new FuelHeatResult(totalBaseHeat, activeCount);
    }

    @Override
    public Widget createUIWidget() {
        int rowSize = 4;
        int colSize = 4;
        var group = new WidgetGroup(0, 0, 18 * rowSize + 16, 18 * colSize + 16);
        var container = new WidgetGroup(4, 4, 18 * rowSize + 8, 18 * colSize + 8);

        for (int y = 0; y < colSize; y++) {
            for (int x = 0; x < rowSize; x++) {
                int index = y * rowSize + x;
                container.addWidget(new SlotWidget(getInventory().storage, index, 4 + x * 18, 4 + y * 18, true, true).setIngredientIO(IngredientIO.BOTH).setBackground(GuiTextures.SLOT));
            }
        }
        container.setBackground(GuiTextures.BACKGROUND_INVERSE);
        group.addWidget(container);
        return group;
    }
}
