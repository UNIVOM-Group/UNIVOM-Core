package com.mrlacar.univom.common.item;

import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.mrlacar.univom.api.data.EntityBlacklist;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class MobContainerBehaviour implements IInteractionItem, IAddInformation {
    private static final String NBT_STORED_ENTITY = "StoredEntity";

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        Level level = player.level();

        if (!interactionTarget.isAlive()) {
            return InteractionResult.PASS;
        }

        if (hasStoredEntity(stack)) {
            if (level.isClientSide) {
                player.displayClientMessage(Component.translatable("univom.message.mob_container.already_full").withStyle(net.minecraft.ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }

        if (EntityBlacklist.isBlacklisted(interactionTarget)) {
            if (level.isClientSide) {
                player.displayClientMessage(Component.translatable("univom.message.mob_container.cannot_capture").withStyle(net.minecraft.ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        interactionTarget.stopRiding();
        interactionTarget.ejectPassengers();

        CompoundTag entityData = new CompoundTag();
        if (interactionTarget.save(entityData)) {
            ItemStack newStack = stack.copy();
            newStack.getOrCreateTag().put(NBT_STORED_ENTITY, entityData);

            player.setItemInHand(usedHand, newStack);

            interactionTarget.discard();
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (!hasStoredEntity(stack)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos spawnPos = clickedPos.relative(face);

        CompoundTag entityData = stack.getTag().getCompound(NBT_STORED_ENTITY).copy();
        entityData.remove("UUID");

        Entity entity = EntityType.loadEntityRecursive(entityData, level, (e) -> {
            e.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, e.getYRot(), e.getXRot());
            return e;
        });

        if (entity != null) {
            level.addFreshEntity(entity);

            if (player != null && player.isCreative()) {
                ItemStack newStack = stack.copy();
                newStack.removeTagKey(NBT_STORED_ENTITY);
                player.setItemInHand(context.getHand(), newStack);
            } else {
                stack.shrink(1);
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    public static boolean hasStoredEntity(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(NBT_STORED_ENTITY);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        if (hasStoredEntity(stack)) {
            CompoundTag entityData = stack.getTag().getCompound(NBT_STORED_ENTITY);
            String entityId = entityData.getString("id");
            Optional<EntityType<?>> entityType = EntityType.byString(entityId);

            if (entityType.isPresent()) {
                Component entityName = Component.translatable(entityType.get().getDescriptionId()).withStyle(net.minecraft.ChatFormatting.GREEN);
                tooltipComponents.add(Component.translatable("univom.tooltip.mob_container.contains_entity", entityName));
            } else {
                tooltipComponents.add(Component.literal("ID: " + entityId).withStyle(net.minecraft.ChatFormatting.GREEN));
            }
            tooltipComponents.add(Component.translatable("univom.tooltip.mob_container.release").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));

        } else {
            tooltipComponents.add(Component.translatable("univom.tooltip.mob_container.empty_container").withStyle(net.minecraft.ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("univom.tooltip.mob_container.capture").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
    }
}
