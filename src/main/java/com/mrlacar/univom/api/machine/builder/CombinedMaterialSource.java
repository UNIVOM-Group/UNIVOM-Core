package com.mrlacar.univom.api.machine.builder;

import net.minecraft.world.item.ItemStack;

public class CombinedMaterialSource implements MaterialSource {

    private final MaterialSource primary;
    private final MaterialSource fallback;

    public CombinedMaterialSource(MaterialSource primary, MaterialSource fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public int extract(ItemStack template, int amount, boolean simulate) {
        int taken = primary.extract(template, amount, simulate);
        if (taken < amount) {
            taken += fallback.extract(template, amount - taken, simulate);
        }
        return taken;
    }

    @Override
    public ItemStack give(ItemStack stack) {
        ItemStack remainder = primary.give(stack);
        return remainder.isEmpty() ? remainder : fallback.give(remainder);
    }

}
