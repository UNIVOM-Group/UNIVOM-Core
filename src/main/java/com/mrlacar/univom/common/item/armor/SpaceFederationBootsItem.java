package com.mrlacar.univom.common.item.armor;

import com.gregtechceu.gtceu.api.item.armor.ArmorComponentItem;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

public class SpaceFederationBootsItem extends ArmorComponentItem {

    public SpaceFederationBootsItem(ArmorMaterial material, Properties properties) {
        super(material, ArmorItem.Type.BOOTS, properties);
    }

    @Override
    public boolean canWalkOnPowderedSnow(ItemStack stack, LivingEntity wearer) {
        return true;
    }
}
