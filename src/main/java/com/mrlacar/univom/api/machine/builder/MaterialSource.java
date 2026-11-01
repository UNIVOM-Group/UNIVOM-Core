package com.mrlacar.univom.api.machine.builder;

import net.minecraft.world.item.ItemStack;

public interface MaterialSource {

    int extract(ItemStack template, int amount, boolean simulate);

    ItemStack give(ItemStack stack);
}
