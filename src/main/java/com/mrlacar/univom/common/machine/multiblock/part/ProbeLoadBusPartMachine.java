package com.mrlacar.univom.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mrlacar.univom.common.data.ModItems;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.gregtechceu.gtceu.api.GTValues.HV;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ProbeLoadBusPartMachine extends SimpleItemBusPartMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(ProbeLoadBusPartMachine.class,
            TieredIOPartMachine.MANAGED_FIELD_HOLDER);

    @Getter
    @Persisted
    private final NotifiableItemStackHandler inventory;

    public ProbeLoadBusPartMachine(IMachineBlockEntity holder) {
        super(holder, HV, IO.IN);
        this.inventory = new ProbeLoadBusHandler(this);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public Widget createUIWidget() {
        int rowSize = 4;
        int colSize = 1;

        var group = new WidgetGroup(0, 0, 18 * rowSize + 16, 18 * colSize + 16);
        var container = new WidgetGroup(4, 4, 18 * rowSize + 8, 18 * colSize + 8);

        for (int x = 0; x < rowSize; x++) {
            IGuiTexture overlayTexture = switch (x) {
                case 0 -> GuiTextures.BOX_OVERLAY;
                case 1 -> GuiTextures.DATA_ORB_OVERLAY;
                case 2 -> GuiTextures.CIRCUIT_OVERLAY;
                case 3 -> GuiTextures.LENS_OVERLAY;
                default -> null;
            };

            var slotWidget = new SlotWidget(getInventory(), x, 4 + x * 18, 4, true, true).setIngredientIO(IngredientIO.INPUT);

            slotWidget.setBackground(GuiTextures.SLOT, overlayTexture);
            container.addWidget(slotWidget);
        }

        container.setBackground(GuiTextures.BACKGROUND_INVERSE);
        group.addWidget(container);
        return group;
    }

    private static class ProbeLoadBusHandler extends NotifiableItemStackHandler {

        public ProbeLoadBusHandler(MetaMachine metaTileEntity) {
            super(metaTileEntity, 4, IO.IN, IO.BOTH, size -> new CustomItemStackHandler(size) {
                @Override
                public int getSlotLimit(int slot) {
                    return 1;
                }
            });
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (stack.isEmpty()) return true;
            return switch (slot) {
                case 0 -> stack.is(ModItems.PROBE_CONTAINER.get());
                case 1 -> stack.is(CustomTags.FIELD_GENERATORS);
                case 2 -> stack.is(ModItems.BASIC_SURFACE_MAPPING_PROBE.get());
                case 3 -> stack.is(ModItems.ADVANCED_MULTISPECTRAL_RECON_PROBE.get());
                default -> false;
            };
        }
    }
}
