package com.mrlacar.univom.common.item;

import com.mrlacar.univom.api.item.ToolKit;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory;
import com.gregtechceu.gtceu.api.item.component.IRecipeRemainder;

import com.mrlacar.univom.world.player.ToolKitCrafting;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ToolKitBehaviour implements IItemUIFactory, IRecipeRemainder, IAddInformation {

    public static final ToolKitBehaviour INSTANCE = new ToolKitBehaviour();

    private static final int UI_WIDTH = 176;
    private static final int UI_HEIGHT = 166;
    private static final int TEXT_COLOR = 0xFAF9F6;
    private static final int SLOT_SIZE = 18;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int SLOT_Y = (PLAYER_INVENTORY_Y - SLOT_SIZE) / 2;

    private ToolKitBehaviour() {}

    @Override
    public ModularUI createUI(HeldItemUIFactory.HeldItemHolder holder, Player entityPlayer) {
        ItemStack kit = holder.getPlayer().getItemInHand(holder.getHand());
        ItemStackTransfer inventory = ToolKit.inventory(kit);
        ModularUI ui = new ModularUI(UI_WIDTH, UI_HEIGHT, holder, entityPlayer)
                .background(GuiTextures.BACKGROUND)
                .widget(new LabelWidget(7, 7, "univom.tool_kit.title").setTextColor(TEXT_COLOR));
        int originX = (UI_WIDTH - ToolKit.SLOTS * SLOT_SIZE) / 2;
        for (int slot = 0; slot < ToolKit.SLOTS; slot++) {
            ui.widget(new SlotWidget(inventory, slot, originX + slot * SLOT_SIZE, SLOT_Y, true, true)
                    .setBackgroundTexture(GuiTextures.SLOT));
        }
        return ui.widget(UITemplate.bindPlayerInventory(entityPlayer.getInventory(), GuiTextures.SLOT, 7,
                PLAYER_INVENTORY_Y, true));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        ItemStackTransfer inventory = ToolKit.read(stack);
        List<Component> tools = new ArrayList<>(ToolKit.SLOTS);
        for (int slot = 0; slot < ToolKit.SLOTS; slot++) {
            ItemStack tool = inventory.getStackInSlot(slot);
            if (!tool.isEmpty()) {
                tools.add(Component.literal(" ").append(tool.getHoverName())
                        .withStyle(ToolKit.isUsable(tool) ? ChatFormatting.AQUA : ChatFormatting.DARK_RED));
            }
        }
        if (tools.isEmpty()) {
            tooltipComponents.add(Component.translatable("univom.tool_kit.empty")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltipComponents.add(Component.translatable("univom.tool_kit.contents", tools.size(), ToolKit.SLOTS)
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.addAll(tools);
    }

    @Override
    public ItemStack getRecipeRemained(ItemStack itemStack) {
        int slot = ToolKitCrafting.matchedSlot(itemStack);
        if (slot < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack remainder = itemStack.copy();
        remainder.setCount(1);
        ToolKit.damageTool(remainder, slot, null, ToolKit.craftingDamage(itemStack, slot));
        return remainder;
    }
}
