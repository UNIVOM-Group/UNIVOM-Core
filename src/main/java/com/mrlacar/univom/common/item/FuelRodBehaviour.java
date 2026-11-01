package com.mrlacar.univom.common.item;

import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IDurabilityBar;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTUtil;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

import static net.minecraft.nbt.Tag.TAG_INT;

public class FuelRodBehaviour implements IDurabilityBar, IAddInformation {
    public final int heat;
    public final int maxDurability;
    private final Supplier<ItemStack> depletedStackSupplier;

    private static final String NBT_KEY_DURABILITY = "Durability";
    private static final int GRADIENT_LEFT = 0xFFD35400;
    private static final int GRADIENT_RIGHT = 0xFFFFCB6A;
    private static final IntIntPair DURABILITY_COLOR = IntIntPair.of(GRADIENT_LEFT, GRADIENT_RIGHT);

    public FuelRodBehaviour(int heat, int maxDurability, Supplier<ItemStack> depletedStackSupplier) {
        this.heat = heat;
        this.maxDurability = maxDurability;
        this.depletedStackSupplier = depletedStackSupplier;
    }

    public ItemStack getDepletedStack() {
        return depletedStackSupplier.get().copy();
    }

    @Override
    public float getDurabilityForDisplay(ItemStack stack) {
        return Mth.clamp((float) getDurability(stack) / (float) maxDurability, 0.0F, 1.0F);
    }

    @Override
    public int getMaxDurability(ItemStack stack) {
        return maxDurability;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return GRADIENT_RIGHT;
    }

    @Nullable
    @Override
    public IntIntPair getDurabilityColorsForDisplay(ItemStack itemStack) {
        return DURABILITY_COLOR;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        if (GTUtil.isShiftDown()) {
        tooltipComponents.add(Component.translatable("univom.tooltip.fuel_rod.heat", FormattingUtil.formatNumbers(heat)).withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(Component.translatable("univom.tooltip.fuel_rod.durability", FormattingUtil.formatNumbers(getDurability(stack)), FormattingUtil.formatNumbers(maxDurability)).withStyle(ChatFormatting.GREEN));
            return;
        }
        tooltipComponents.add(Component.translatable("univom.tooltip.fuel_rod.description"));
    }

    public final int getDurability(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_KEY_DURABILITY, TAG_INT)) return this.maxDurability;
        return tag.getInt(NBT_KEY_DURABILITY);
    }

    public void setDurability(ItemStack stack, int durability) {
        stack.getOrCreateTag().putInt(NBT_KEY_DURABILITY, Mth.clamp(durability, 0, maxDurability));
    }

    public boolean consumeDurability(ItemStack stack, int amount) {
        if (amount <= 0) return true;
        int current = getDurability(stack);
        if (current <= 0) return false;

        int resultingDurability = current - amount;
        setDurability(stack, resultingDurability);
        return resultingDurability > 0;
    }
}
