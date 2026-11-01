package com.mrlacar.univom.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SystemInfoCardBehaviour extends Item {
    public SystemInfoCardBehaviour(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return (stack.hasTag() && stack.getTag().contains("SystemID")) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag isAdvanced) {
        if (itemStack.hasTag() && itemStack.getTag().contains("SystemID")) {
            String systemId = itemStack.getTag().getString("SystemID");
            ResourceLocation systemInfo = ResourceLocation.tryParse(systemId);

            if (systemInfo != null) {
                Component systemName = Component.translatable("univom.gui.system_selection." + systemInfo.getPath());
                tooltip.add(Component.translatable("univom.tooltip.system_info_card.system").append(systemName));
            } else {
                tooltip.add(Component.translatable("univom.tooltip.system_info_card.system").append(systemId));
            }

            switch (systemId) {
                case "solar_system":
                    for (int i = 0; i < 6; i++) {
                        if (i == 0) tooltip.add(Component.translatable("univom.tooltip.system_info_card.solar_system." + i).withStyle(ChatFormatting.GRAY));
                        if (i > 0) tooltip.add(Component.translatable("univom.tooltip.system_info_card.solar_system." + i).withStyle(ChatFormatting.AQUA));
                    }
                    break;

                case "proxima_centauri":
                    for (int i = 0; i < 2; i++) {
                        if (i == 0) tooltip.add(Component.translatable("univom.tooltip.system_info_card.proxima_centauri." + i).withStyle(ChatFormatting.GRAY));
                        if (i > 0) tooltip.add(Component.translatable("univom.tooltip.system_info_card.proxima_centauri." + i).withStyle(ChatFormatting.AQUA));
                    }
                    break;

                case "ex_68_system":
                    for (int i = 0; i < 2; i++) {
                        if (i == 0) tooltip.add(Component.translatable("univom.tooltip.system_info_card.ex_68_system." + i).withStyle(ChatFormatting.GRAY));
                        if (i > 0) tooltip.add(Component.translatable("univom.tooltip.system_info_card.ex_68_system." + i).withStyle(ChatFormatting.AQUA));
                    }
                    break;

                default:
                    tooltip.add(Component.translatable("univom.tooltip.system_info_card.error").withStyle(ChatFormatting.ITALIC));
                    break;
            }

        } else {
            tooltip.add(Component.translatable("univom.tooltip.system_info_card.empty.0").withStyle(ChatFormatting.ITALIC));
            tooltip.add(Component.translatable("univom.tooltip.system_info_card.empty.1").withStyle(ChatFormatting.ITALIC));
        }

        super.appendHoverText(itemStack, level, tooltip, isAdvanced);
    }
}
