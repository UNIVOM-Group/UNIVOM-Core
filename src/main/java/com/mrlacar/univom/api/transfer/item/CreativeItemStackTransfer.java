package com.mrlacar.univom.api.transfer.item;

import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class CreativeItemStackTransfer extends CustomItemStackHandler {
    public CreativeItemStackTransfer(int size) {
        super(size);
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        ItemStack stack = super.getStackInSlot(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;

        ItemStack copy = stack.copy();
        copy.setCount(Integer.MAX_VALUE);
        return copy;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack stack = super.getStackInSlot(slot);
        if (stack.isEmpty() || amount <= 0) return ItemStack.EMPTY;

        ItemStack copy = stack.copy();
        copy.setCount(amount);

        return copy;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }

    @Override
    protected int getStackLimit(int slot, @NotNull ItemStack stack) {
        return Integer.MAX_VALUE;
    }
}
