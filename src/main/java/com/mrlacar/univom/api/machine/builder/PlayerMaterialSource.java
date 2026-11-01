package com.mrlacar.univom.api.machine.builder;

import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

public class PlayerMaterialSource implements MaterialSource {

    private static final int MAX_NESTING = 3;

    private final Player player;

    public PlayerMaterialSource(Player player) {
        this.player = player;
    }

    @Override
    public int extract(ItemStack template, int amount, boolean simulate) {
        if (template.isEmpty() || amount <= 0) {
            return 0;
        }
        if (player.isCreative()) {
            return amount;
        }
        IItemHandler handler = player.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null);
        return handler == null ? 0 : extractFrom(handler, template, amount, simulate, 0);
    }

    @Override
    public ItemStack give(ItemStack stack) {
        if (stack.isEmpty() || player.isCreative()) {
            return ItemStack.EMPTY;
        }
        if (!player.getInventory().add(stack) && !stack.isEmpty()) {
            player.drop(stack, false);
        }
        return ItemStack.EMPTY;
    }

    private static int extractFrom(IItemHandler handler, ItemStack template, int amount, boolean simulate, int depth) {
        int taken = 0;
        for (int slot = 0; slot < handler.getSlots() && taken < amount; slot++) {
            ItemStack inSlot = handler.getStackInSlot(slot);
            if (inSlot.isEmpty()) {
                continue;
            }
            if (GTUtil.isSameItemSameTags(template, inSlot)) {
                taken += handler.extractItem(slot, amount - taken, simulate).getCount();
                continue;
            }
            if (depth >= MAX_NESTING) {
                continue;
            }
            IItemHandler nested = inSlot.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null);
            if (nested != null) {
                taken += extractFrom(nested, template, amount - taken, simulate, depth + 1);
            }
        }
        return taken;
    }
}
