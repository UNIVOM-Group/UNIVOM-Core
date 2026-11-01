package com.mrlacar.univom.common.item;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;
import com.mrlacar.univom.api.machine.feature.IMachineConnectorInteractable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;

public class MachineConnectorBehaviour implements IInteractionItem, IAddInformation {
    @Override
    public InteractionResultHolder<ItemStack> use(Item item, Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (level.isClientSide) return InteractionResultHolder.pass(stack);

        IElectricItem electricItem = GTCapabilityHelper.getElectricItem(stack);
        if (electricItem == null) return InteractionResultHolder.pass(stack);

        int energyCost = player.isCreative() ? 0 : 1000;

        if (player.isShiftKeyDown()) {
            if (energyCost == 0 || drainEnergy(stack, energyCost, true)) {

                int permLevel = 0;
                for (int i = 4; i >= 1; i--) {
                    if (player.hasPermissions(i)) {
                        permLevel = i;
                        break;
                    }
                }

                stack.getOrCreateTag().putString("boundPlayerName", Component.Serializer.toJson(player.getDisplayName()));
                stack.getOrCreateTag().putInt("boundPlayerPermLevel", permLevel);
                stack.getOrCreateTag().putString("boundPlayerUUID", player.getStringUUID());

                boolean hasCleared = false;

                if (stack.getOrCreateTag().contains("astronomical_analysis_station_pos")) {
                    stack.getOrCreateTag().remove("astronomical_analysis_station_pos");
                    stack.getOrCreateTag().remove("astronomical_analysis_station_dim");
                    hasCleared = true;
                }

                if (hasCleared) {
                    player.displayClientMessage(Component.translatable("univom.message.machine_connector.bind_cleared"), true);
                } else {
                    player.displayClientMessage(Component.translatable("univom.message.machine_connector.player_bound"), true);
                }

                if (energyCost > 0) {
                    drainEnergy(stack, energyCost, false);
                }

                return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
            } else {
                player.sendSystemMessage(Component.translatable("behavior.prospector.not_enough_energy"));
                return InteractionResultHolder.fail(stack);
            }
        }
        return IInteractionItem.super.use(item, level, player, usedHand);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack itemStack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Level level = context.getLevel();

        IElectricItem electricItem = GTCapabilityHelper.getElectricItem(itemStack);
        if (electricItem == null) return InteractionResult.PASS;

        int energyCost = player.isCreative() ? 0 : 1000;

        if (energyCost > 0 && !drainEnergy(itemStack, energyCost, true)) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.translatable("behavior.prospector.not_enough_energy"));
            }
            return InteractionResult.FAIL;
        }

        IMachineConnectorInteractable interactable = null;

        ICoverable coverable = GTCapabilityHelper.getCoverable(level, context.getClickedPos(), context.getClickedFace());
        if (coverable != null && coverable.getCoverAtSide(context.getClickedFace()) instanceof IMachineConnectorInteractable covInteractable) {
            interactable = covInteractable;
        } else if (level.getBlockEntity(context.getClickedPos()) instanceof MetaMachineBlockEntity blockEntity) {
            var machine = blockEntity.getMetaMachine();
            if (!MachineOwner.canOpenOwnerMachine(player, machine)) {
                return InteractionResult.FAIL;
            }
            if (machine instanceof IMachineConnectorInteractable machInteractable) {
                interactable = machInteractable;
            }
        }

        if (interactable != null) {
            InteractionResult result;
            if (context.isSecondaryUseActive()) {
                result = interactable.onMachineConnectorShiftUse(player, itemStack);
            } else {
                result = interactable.onMachineConnectorUse(player, itemStack);
            }

            if (result.consumesAction()) {
                if (!level.isClientSide && energyCost > 0) {
                    drainEnergy(itemStack, energyCost, false);
                }
            }
            return result;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        if (stack.getOrCreateTag().contains("boundPlayerName")) {
            MutableComponent name = Component.Serializer.fromJson(stack.getOrCreateTag().getString("boundPlayerName"));
            tooltipComponents.add(Component.translatable("gtceu.tooltip.player_bind", name));
        }

        if (stack.getOrCreateTag().contains("astronomical_analysis_station_pos", Tag.TAG_LONG)) {
            long posLong = stack.getOrCreateTag().getLong("astronomical_analysis_station_pos");
            BlockPos pos = BlockPos.of(posLong);

            tooltipComponents.add(Component.translatable(
                    "univom.tooltip.machine_connector.astronomical_analysis_station_bind",
                    Component.literal("" + pos.getX()).withStyle(ChatFormatting.LIGHT_PURPLE),
                    Component.literal("" + pos.getY()).withStyle(ChatFormatting.LIGHT_PURPLE),
                    Component.literal("" + pos.getZ()).withStyle(ChatFormatting.LIGHT_PURPLE)
            ));
        }
    }

    protected boolean drainEnergy(@Nonnull ItemStack stack, int amount, boolean simulate) {
        IElectricItem electricItem = GTCapabilityHelper.getElectricItem(stack);
        if (electricItem == null) return false;
        return electricItem.discharge(amount, Integer.MAX_VALUE, true, false, simulate) >= amount;
    }
}
