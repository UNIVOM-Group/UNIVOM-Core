package com.mrlacar.univom.world.player;

import com.mrlacar.univom.api.item.ToolKit;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public final class ToolKitCrafting {

    private static final ThreadLocal<Match> LAST_MATCH = new ThreadLocal<>();

    private ToolKitCrafting() {}

    public static boolean test(Ingredient ingredient, ItemStack stack) {
        int slot = ToolKit.matchingSlot(ingredient, stack);
        if (slot < 0) {
            return false;
        }
        LAST_MATCH.set(new Match(stack, slot));
        return true;
    }

    public static int matchedSlot(ItemStack stack) {
        Match match = LAST_MATCH.get();
        return match != null && match.stack() == stack ? match.slot() : -1;
    }

    private record Match(ItemStack stack, int slot) {}
}
