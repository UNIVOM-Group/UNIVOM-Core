package com.mrlacar.univom.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.IPaintable;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.widget.PhantomFluidWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CircuitFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.IHasCircuitSlot;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mrlacar.univom.api.transfer.fluid.CreativeFluidTank;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeFluidHatchPartMachine extends TieredIOPartMachine implements IMachineLife, IHasCircuitSlot, IPaintable {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            CreativeFluidHatchPartMachine.class, TieredIOPartMachine.MANAGED_FIELD_HOLDER);

    @Getter
    @Persisted
    public final NotifiableFluidTank tank;

    @Getter
    @Setter
    @Persisted
    @DescSynced
    protected boolean circuitSlotEnabled;

    @Getter
    @Persisted
    protected final NotifiableItemStackHandler circuitInventory;

    public CreativeFluidHatchPartMachine(IMachineBlockEntity holder) {
        super(holder, GTValues.MAX, IO.IN);

        List<CustomFluidTank> creativeTanks = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            creativeTanks.add(new CreativeFluidTank(Integer.MAX_VALUE));
        }

        this.tank = new NotifiableFluidTank(this, creativeTanks, IO.IN);

        this.circuitSlotEnabled = true;
        this.circuitInventory = createCircuitItemHandler(IO.IN).shouldSearchContent(false);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    protected NotifiableItemStackHandler createCircuitItemHandler(Object... args) {
        if (args.length > 0 && args[0] instanceof IO io && io == IO.IN) {
            return new NotifiableItemStackHandler(this, 1, IO.IN, IO.NONE)
                    .setFilter(IntCircuitBehaviour::isIntegratedCircuit);
        } else {
            return new NotifiableItemStackHandler(this, 0, IO.NONE);
        }
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
        super.attachConfigurators(configuratorPanel);
        if (isCircuitSlotEnabled() && this.io == IO.IN) {
            configuratorPanel.attachConfigurators(new CircuitFancyConfigurator(circuitInventory.storage));
        }
    }

    @Override
    public Widget createUIWidget() {
        int rowSize = (int) Math.sqrt(16);
        int colSize = rowSize;

        var group = new WidgetGroup(0, 0, 18 * rowSize + 16, 18 * colSize + 16);
        var container = new WidgetGroup(4, 4, 18 * rowSize + 8, 18 * colSize + 8);

        CustomFluidTank[] storages = this.tank.getStorages();
        CustomFluidTank[] uiTanks = new CustomFluidTank[16];

        for (int i = 0; i < 16; i++) {
            CreativeFluidTank realTank = (CreativeFluidTank) storages[i];
            uiTanks[i] = new CustomFluidTank(1000) {
                @Override
                public @NotNull FluidStack getFluid() {
                    FluidStack real = realTank.getGhostFluid();
                    if (real.isEmpty()) return FluidStack.EMPTY;
                    FluidStack copy = real.copy();
                    copy.setAmount(1);
                    return copy;
                }
            };
        }

        int index = 0;
        for (int y = 0; y < colSize; y++) {
            for (int x = 0; x < rowSize; x++) {
                int finalIndex = index++;
                CreativeFluidTank realTank = (CreativeFluidTank) storages[finalIndex];

                container.addWidget(new PhantomFluidWidget(
                        uiTanks[finalIndex], 0,
                        4 + x * 18, 4 + y * 18, 18, 18,
                        () -> uiTanks[finalIndex].getFluid(),
                        (fluid) -> {
                            if (fluid.isEmpty()) {
                                realTank.setGhostFluid(FluidStack.EMPTY);
                            } else {
                                FluidStack ghost = fluid.copy();
                                ghost.setAmount(1);
                                realTank.setGhostFluid(ghost);
                            }
                            CreativeFluidHatchPartMachine.this.markDirty();
                        })
                        .setShowAmount(false)
                        .setBackground(GuiTextures.FLUID_SLOT));
            }
        }

        container.setBackground(GuiTextures.BACKGROUND_INVERSE);
        group.addWidget(container);

        return group;
    }
}