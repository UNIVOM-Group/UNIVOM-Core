package com.mrlacar.univom.common.item;

import com.gregtechceu.gtceu.api.item.ComponentItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class FoilComponentItem extends ComponentItem {

    public FoilComponentItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }
}
