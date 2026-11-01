package com.mrlacar.univom.common.item.armor;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.item.armor.ArmorComponentItem;
import com.gregtechceu.gtceu.api.item.armor.IArmorLogic;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import java.util.Map;

public interface IFederationArmor {

    float BONUS_ARMOR = 2.0F;
    float KNOCKBACK_RESISTANCE = 0.25F;

    static Multimap<Attribute, AttributeModifier> addBonusAttributes(Multimap<Attribute, AttributeModifier> modifiers,
                                                                    ArmorItem.Type type) {
        if (modifiers.isEmpty()) {
            return modifiers;
        }
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entries()) {
            AttributeModifier modifier = entry.getValue();
            if (entry.getKey() == Attributes.ARMOR &&
                    modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                builder.put(entry.getKey(), new AttributeModifier(modifier.getId(), modifier.getName(),
                        modifier.getAmount() + BONUS_ARMOR, modifier.getOperation()));
            } else {
                builder.put(entry.getKey(), modifier);
            }
        }
        builder.put(Attributes.KNOCKBACK_RESISTANCE,
                new AttributeModifier(IArmorLogic.ARMOR_MODIFIER_UUID_PER_TYPE.get(type),
                        "Armor knockback resistance", KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADDITION));
        return builder.build();
    }

    static int addBonusArmorDisplay(int display) {
        return display > 0 ? display + (int) BONUS_ARMOR : display;
    }

    static boolean isFederationPiece(ItemStack stack) {
        return stack.getItem() instanceof ArmorComponentItem armor &&
                armor.getArmorLogic() instanceof IFederationArmor;
    }

    static boolean hasFullSet(LivingEntity entity) {
        return isFederationPiece(entity.getItemBySlot(EquipmentSlot.HEAD)) &&
                isFederationPiece(entity.getItemBySlot(EquipmentSlot.CHEST)) &&
                isFederationPiece(entity.getItemBySlot(EquipmentSlot.LEGS)) &&
                isFederationPiece(entity.getItemBySlot(EquipmentSlot.FEET));
    }

    static boolean canUseEnergy(LivingEntity entity, EquipmentSlot slot, long amount) {
        if (amount <= 0) {
            return true;
        }
        IElectricItem item = GTCapabilityHelper.getElectricItem(entity.getItemBySlot(slot));
        return item != null && item.canUse(amount);
    }

    static boolean consumeEnergy(LivingEntity entity, EquipmentSlot slot, long amount) {
        if (amount <= 0) {
            return true;
        }
        IElectricItem item = GTCapabilityHelper.getElectricItem(entity.getItemBySlot(slot));
        if (item == null || !item.canUse(amount)) {
            return false;
        }
        item.discharge(amount, item.getTier(), true, false, false);
        return true;
    }
}
