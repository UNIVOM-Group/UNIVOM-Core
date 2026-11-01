package com.mrlacar.univom.mixin.gtceu.api.machine.multiblock;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderFluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = MultiblockDisplayText.Builder.class, remap = false)
public abstract class MultiblockDisplayTextMixin {
    @Shadow
    @Final
    private List<Component> textList;
    @Shadow @Final private boolean isStructureFormed;
    @Shadow private boolean isActive;

    @Inject(method = "addOutputLines", at = @At("HEAD"), cancellable = true)
    private void injectCustomOutputLines(GTRecipe recipe, CallbackInfoReturnable<MultiblockDisplayText.Builder> cir) {
        if (!this.isStructureFormed || !this.isActive || recipe == null) {
            cir.setReturnValue((MultiblockDisplayText.Builder) (Object) this);
            return;
        }

        int recipeTier = RecipeHelper.getPreOCRecipeEuTier(recipe);
        int chanceTier = recipeTier + recipe.ocLevel;
        var function = recipe.getType().getChanceFunction();

        double maxDurationSec = Math.max(0.05, (double) recipe.duration / 20.0);

        var itemOutputs = recipe.getOutputContents(ItemRecipeCapability.CAP);
        var fluidOutputs = recipe.getOutputContents(FluidRecipeCapability.CAP);
        int runs = recipe.getTotalRuns();

        for (var item : itemOutputs) {
            boolean rounded = false;
            ItemStack stack;
            int count = 0;
            double countD = 1;
            Component displaycount;

            if (item.content instanceof IntProviderIngredient provider) {
                rounded = true;
                stack = provider.getMaxSizeStack();
                displaycount = Component.translatable("gtceu.gui.content.range",
                        provider.getCountProvider().getMinValue(),
                        provider.getCountProvider().getMaxValue());
                if (item.chance < item.maxChance) {
                    countD = countD * runs * function.getBoostedChance(item, recipeTier, chanceTier) / item.maxChance;
                }
                countD = countD * provider.getMidRoll();
            } else {
                var stacks = ItemRecipeCapability.CAP.of(item.content).getItems();
                if (stacks.length == 0) continue;
                stack = stacks[0];
                count = stack.getCount();
                countD *= count;
                if (item.chance < item.maxChance) {
                    rounded = true;
                    countD = countD * runs * function.getBoostedChance(item, recipeTier, chanceTier) / item.maxChance;
                }
                count = Math.max(1, (int) Math.round(countD));
                displaycount = Component.literal(String.valueOf(count));
            }

            double safeCountD = Math.max(0.0001, countD);

            if (safeCountD < maxDurationSec) {
                String key = "gtceu.multiblock.output_line." + (rounded ? "2" : "0");
                textList.add(Component.translatable(key, stack.getHoverName(), displaycount,
                                FormattingUtil.formatNumber2Places(maxDurationSec / safeCountD))
                        .withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(stack)))));
            } else {
                String key = "gtceu.multiblock.output_line." + (rounded ? "3" : "1");
                textList.add(Component.translatable(key, stack.getHoverName(), displaycount,
                                FormattingUtil.formatNumber2Places(safeCountD / maxDurationSec))
                        .withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(stack)))));
            }
        }

        for (var fluid : fluidOutputs) {
            boolean rounded = false;
            FluidStack stack;
            int amount = 0;
            double amountD = 1;
            Component displaycount;

            if (fluid.content instanceof IntProviderFluidIngredient provider) {
                rounded = true;
                stack = provider.getMaxSizeStack();
                displaycount = Component.translatable("gtceu.gui.content.range",
                        provider.getCountProvider().getMinValue(),
                        provider.getCountProvider().getMaxValue());
                if (fluid.chance < fluid.maxChance) {
                    amountD = amountD * runs * function.getBoostedChance(fluid, recipeTier, chanceTier) / fluid.maxChance;
                }
                amountD = amountD * provider.getMidRoll();
            } else {
                var stacks = FluidRecipeCapability.CAP.of(fluid.content).getStacks();
                if (stacks.length == 0) continue;
                stack = stacks[0];
                amount = stack.getAmount();
                amountD *= amount;
                if (fluid.chance < fluid.maxChance) {
                    rounded = true;
                    amountD = amountD * runs * function.getBoostedChance(fluid, recipeTier, chanceTier) / fluid.maxChance;
                }
                amount = Math.max(1, (int) Math.round(amountD));
                displaycount = Component.literal(String.valueOf(amount));
            }

            Item bucketItem = stack.getFluid().getBucket();
            ItemStack bucketStack = (bucketItem == null || bucketItem == Items.AIR) ? ItemStack.EMPTY : new ItemStack(bucketItem);

            final int finalAmount = amount;
            double safeAmountD = Math.max(0.0001, amountD);

            if (safeAmountD < maxDurationSec) {
                String key = "gtceu.multiblock.output_line." + (rounded ? "2" : "0");
                textList.add(Component.translatable(key, stack.getDisplayName(), displaycount,
                                FormattingUtil.formatNumber2Places(maxDurationSec / safeAmountD))
                        .withStyle(style -> !bucketStack.isEmpty()
                                ? style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(bucketStack)))
                                : style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(finalAmount + " mB")))));
            } else {
                String key = "gtceu.multiblock.output_line." + (rounded ? "3" : "1");
                textList.add(Component.translatable(key, stack.getDisplayName(), displaycount,
                                FormattingUtil.formatNumber2Places(safeAmountD / maxDurationSec))
                        .withStyle(style -> !bucketStack.isEmpty()
                                ? style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(bucketStack)))
                                : style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(finalAmount + " mB")))));
            }
        }

        cir.setReturnValue((MultiblockDisplayText.Builder) (Object) this);
    }
}
