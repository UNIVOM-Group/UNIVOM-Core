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

public class ControlRodBehaviour implements IAddInformation {
    public final double controlRodEfficiency;

    public ControlRodBehaviour(double controlRodEfficiency) {
        this.controlRodEfficiency = controlRodEfficiency;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        int displayPercent = (int) (this.controlRodEfficiency * 100);
        if (GTUtil.isShiftDown()) {
            tooltipComponents.add(Component.translatable("univom.tooltip.control_rod.efficiency", displayPercent).withStyle(ChatFormatting.AQUA));
            return;
        }
        tooltipComponents.add(Component.translatable("univom.tooltip.control_rod.description"));
    }
}
