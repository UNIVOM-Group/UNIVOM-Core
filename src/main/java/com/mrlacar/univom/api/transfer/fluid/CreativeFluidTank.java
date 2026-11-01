package com.mrlacar.univom.api.transfer.fluid;

import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

public class CreativeFluidTank extends CustomFluidTank {
    public CreativeFluidTank(int capacity) {
        super(capacity);
    }

    @Override
    public @NotNull FluidStack getFluid() {
        FluidStack stack = super.getFluid();
        if (stack.isEmpty()) return FluidStack.EMPTY;
        FluidStack copy = stack.copy();
        copy.setAmount(Integer.MAX_VALUE);
        return copy;
    }

    @Override
    public int getFluidAmount() {
        return super.getFluid().isEmpty() ? 0 : Integer.MAX_VALUE;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack stack = super.getFluid();
        if (stack.isEmpty() || maxDrain <= 0) return FluidStack.EMPTY;
        FluidStack copy = stack.copy();
        copy.setAmount(maxDrain);
        return copy;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !resource.isFluidEqual(super.getFluid())) return FluidStack.EMPTY;
        FluidStack copy = super.getFluid().copy();
        copy.setAmount(resource.getAmount());
        return copy;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    public void setGhostFluid(FluidStack stack) {
        super.setFluid(stack);
    }

    public FluidStack getGhostFluid() {
        return super.getFluid();
    }
}
