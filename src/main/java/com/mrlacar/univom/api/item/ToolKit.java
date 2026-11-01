package com.mrlacar.univom.api.item;

import com.mrlacar.univom.common.data.ModItems;

import com.gregtechceu.gtceu.api.item.IGTTool;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;

import org.jetbrains.annotations.Nullable;

public final class ToolKit {

    public static final GTToolType[] TOOLS = {
            GTToolType.WRENCH,
            GTToolType.SCREWDRIVER,
            GTToolType.SOFT_MALLET,
            GTToolType.HARD_HAMMER,
            GTToolType.WIRE_CUTTER,
            GTToolType.CROWBAR
    };

    public static final int SLOTS = TOOLS.length;

    private static final String ROOT_TAG = "tool_kit";
    private static final String ITEMS_TAG = "items";

    private ToolKit() {}

    public static boolean isToolKit(ItemStack stack) {
        return stack.is(ModItems.TOOL_KIT.get());
    }

    public static int slotFor(ItemStack tool) {
        if (tool.isEmpty()) {
            return -1;
        }
        for (int slot = 0; slot < SLOTS; slot++) {
            if (ToolHelper.is(tool, TOOLS[slot])) {
                return slot;
            }
        }
        return -1;
    }

    public static ItemStackTransfer read(ItemStack kit) {
        ToolKitInventory inventory = new ToolKitInventory();
        CompoundTag tag = kit.getTagElement(ROOT_TAG);
        if (tag != null && tag.contains(ITEMS_TAG)) {
            inventory.deserializeNBT(tag.getCompound(ITEMS_TAG));
        }
        return inventory;
    }

    public static ItemStackTransfer inventory(ItemStack kit) {
        ItemStackTransfer inventory = read(kit);
        inventory.setOnContentsChanged(() -> save(kit, inventory));
        return inventory;
    }

    public static ItemStack getTool(ItemStack kit, int slot) {
        return read(kit).getStackInSlot(slot);
    }

    public static boolean isUsable(ItemStack tool) {
        return !tool.isEmpty() && ToolHelper.canUse(tool);
    }

    public static int craftingDamage(ItemStack kit, int slot) {
        ItemStack tool = getTool(kit, slot);
        return tool.getItem() instanceof IGTTool gtTool ?
                gtTool.getToolStats().getToolDamagePerCraft(tool) : 1;
    }

    public static boolean hasUsableTool(ItemStack kit, int slot) {
        return isUsable(read(kit).getStackInSlot(slot));
    }

    public static void damageTool(ItemStack kit, int slot, @Nullable ServerPlayer player, int damage) {
        ItemStackTransfer handler = inventory(kit);
        ItemStack tool = handler.getStackInSlot(slot);
        if (tool.isEmpty()) {
            return;
        }
        ToolHelper.damageItem(tool, player, damage);
        handler.setStackInSlot(slot, tool.isEmpty() || !ToolHelper.canUse(tool) ? ItemStack.EMPTY : tool);
        save(kit, handler);
    }

    public static void save(ItemStack kit, ItemStackTransfer handler) {
        kit.getOrCreateTagElement(ROOT_TAG).put(ITEMS_TAG, handler.serializeNBT());
    }

    @Nullable
    public static ItemStack findHeldOrWorn(Player player) {
        for (ItemStack held : player.getHandSlots()) {
            if (isToolKit(held)) {
                return held;
            }
        }
        return ToolKitCurios.find(player);
    }

    public static int matchingSlot(Ingredient ingredient, ItemStack kit) {
        ItemStackTransfer handler = read(kit);
        for (int slot = 0; slot < SLOTS; slot++) {
            if (isUsable(handler.getStackInSlot(slot)) && accepts(ingredient, TOOLS[slot])) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean accepts(Ingredient ingredient, GTToolType toolType) {
        if (toolType.craftingTags.isEmpty()) {
            return false;
        }
        for (ItemStack candidate : ingredient.getItems()) {
            if (candidate.isEmpty()) {
                continue;
            }
            Item item = candidate.getItem();
            for (TagKey<Item> tag : toolType.craftingTags) {
                if (item.builtInRegistryHolder().is(tag)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final class ToolKitInventory extends ItemStackTransfer {

        private ToolKitInventory() {
            super(SLOTS);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.isEmpty() || slotFor(stack) == slot;
        }
    }
}
