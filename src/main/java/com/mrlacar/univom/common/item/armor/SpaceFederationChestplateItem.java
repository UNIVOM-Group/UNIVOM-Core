package com.mrlacar.univom.common.item.armor;

import com.gregtechceu.gtceu.api.item.armor.ArmorComponentItem;

import lombok.Getter;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@Getter
public class SpaceFederationChestplateItem extends ArmorComponentItem {

    private static Predicate<LivingEntity> breathableAir = entity -> true;

    private final int tankCapacity;

    public SpaceFederationChestplateItem(ArmorMaterial material, Properties properties, int tankCapacity) {
        super(material, ArmorItem.Type.CHESTPLATE, properties);
        this.tankCapacity = tankCapacity;
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new SpaceFederationOxygenTank(stack, tankCapacity, super.initCapabilities(stack, nbt));
    }

    @Override
    public void fillItemCategory(CreativeModeTab category, NonNullList<ItemStack> items) {
        int start = items.size();
        super.fillItemCategory(category, items);
        for (int i = start; i < items.size(); i++) {
            SpaceFederationOxygenTank.fill(items.get(i), tankCapacity);
        }
    }

    public static void setBreathableAirCheck(Predicate<LivingEntity> check) {
        breathableAir = check;
    }

    public static boolean hasBreathableAir(LivingEntity entity) {
        return breathableAir.test(entity);
    }

    public static int getOxygenAmount(LivingEntity entity) {
        return getOxygenAmount(entity.getItemBySlot(EquipmentSlot.CHEST));
    }

    public static int getOxygenAmount(ItemStack stack) {
        return stack.getItem() instanceof SpaceFederationChestplateItem ?
                SpaceFederationOxygenTank.readAmount(stack) : 0;
    }

    public static int getOxygenCapacity(ItemStack stack) {
        return stack.getItem() instanceof SpaceFederationChestplateItem chestplate ?
                chestplate.getTankCapacity() : 0;
    }
}
