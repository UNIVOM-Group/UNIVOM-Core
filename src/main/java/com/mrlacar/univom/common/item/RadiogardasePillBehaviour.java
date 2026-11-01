package com.mrlacar.univom.common.item;

import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.utils.GTUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RadiogardasePillBehaviour implements IAddInformation {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        if (GTUtil.isShiftDown()) {
            tooltipComponents.add(Component.translatable("gtceu.medical_condition.antidote.description_shift"));
            tooltipComponents.add(Component.translatable("effect.univom.irradiation").withStyle(ChatFormatting.GREEN));
            return;
        }
        tooltipComponents.add(Component.translatable("gtceu.medical_condition.antidote.description"));
    }
}
