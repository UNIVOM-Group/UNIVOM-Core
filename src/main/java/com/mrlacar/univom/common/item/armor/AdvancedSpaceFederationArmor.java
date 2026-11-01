package com.mrlacar.univom.common.item.armor;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.item.armor.ArmorUtils;
import com.gregtechceu.gtceu.common.item.armor.AdvancedQuarkTechSuite;
import com.gregtechceu.gtceu.core.IFireImmuneEntity;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.input.SyncedKeyMappings;
import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.config.ConfigHolder;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;

public class AdvancedSpaceFederationArmor extends AdvancedQuarkTechSuite implements IFederationArmor {

    private static final ResourceLocation TEXTURE_1 = UNIVOM.id("textures/armor/space_federation_armor_1.png");
    private static final int CHARGE_INTERVAL = 10;
    public static final int OXYGEN_INTERVAL = 12;
    public static final int OXYGEN_PER_CYCLE = 1;

    public AdvancedSpaceFederationArmor(int energyPerUse, long capacity, int tier) {
        super(energyPerUse, capacity, tier);
    }

    @Override
    public void onArmorTick(Level world, Player player, ItemStack item) {
        IElectricItem cont = GTCapabilityHelper.getElectricItem(item);
        if (cont == null) {
            return;
        }

        CompoundTag data = item.getOrCreateTag();
        if (!data.contains("enabled")) {
            data.putBoolean("enabled", true);
            data.putBoolean("hover", false);
            data.putByte("toggleTimer", (byte) 0);
            data.putBoolean("canShare", false);
        }

        boolean jetpackEnabled = data.getBoolean("enabled");
        boolean hoverMode = data.getBoolean("hover");
        byte toggleTimer = data.getByte("toggleTimer");
        boolean canShare = data.getBoolean("canShare");

        String messageKey = null;
        if (toggleTimer == 0) {
            if (SyncedKeyMappings.JETPACK_ENABLE.isKeyDown(player)) {
                jetpackEnabled = !jetpackEnabled;
                messageKey = "metaarmor.jetpack.flight." + (jetpackEnabled ? "enable" : "disable");
                data.putBoolean("enabled", jetpackEnabled);
            } else if (SyncedKeyMappings.ARMOR_HOVER.isKeyDown(player)) {
                hoverMode = !hoverMode;
                messageKey = "metaarmor.jetpack.hover." + (hoverMode ? "enable" : "disable");
                data.putBoolean("hover", hoverMode);
            } else if (SyncedKeyMappings.ARMOR_CHARGING.isKeyDown(player)) {
                canShare = !canShare;
                if (canShare && cont.getCharge() == 0) {
                    messageKey = "metaarmor.qts.share.error";
                    canShare = false;
                } else {
                    messageKey = "metaarmor.qts.share." + (canShare ? "enable" : "disable");
                }
                data.putBoolean("canShare", canShare);
            }

            if (messageKey != null) {
                toggleTimer = 5;
                if (!world.isClientSide) player.displayClientMessage(Component.translatable(messageKey), true);
            }
        }

        if (toggleTimer > 0) toggleTimer--;
        data.putByte("toggleTimer", toggleTimer);

        performFlying(player, jetpackEnabled, hoverMode, item);

        if (type == ArmorItem.Type.CHESTPLATE && !player.fireImmune()) {
            ((IFireImmuneEntity) player).gtceu$setFireImmune(true);
            if (player.isOnFire()) player.extinguishFire();
        }

        if (!world.isClientSide && player.tickCount % OXYGEN_INTERVAL == 0) {
            supplyOxygen(player, item, cont);
        }

        if (canShare && !world.isClientSide && player.tickCount % CHARGE_INTERVAL == 0) {
            chargeInventory(player, cont);
        }
    }

    private static void supplyOxygen(Player player, ItemStack item, IElectricItem cont) {
        if (!IFederationArmor.hasFullSet(player) || SpaceFederationChestplateItem.hasBreathableAir(player)) {
            return;
        }
        if (SpaceFederationChestplateItem.getOxygenAmount(item) < OXYGEN_PER_CYCLE) {
            return;
        }

        int cost = ConfigHolder.INSTANCE.spaceFederationArmorSetting.oxygenEnergyCost;
        if (!cont.canUse(cost)) {
            return;
        }
        if (!SpaceFederationOxygenTank.consume(item, OXYGEN_PER_CYCLE)) {
            return;
        }

        cont.discharge(cost, cont.getTier(), true, false, false);
    }

    private static void chargeInventory(Player player, IElectricItem cont) {
        List<Pair<NonNullList<ItemStack>, IntList>> inventoryIndexMap = ArmorUtils.getChargeableItem(player,
                cont.getTier());
        if (inventoryIndexMap.isEmpty()) {
            return;
        }

        boolean charged = false;
        for (int i = 0; i < inventoryIndexMap.size(); i++) {
            Pair<NonNullList<ItemStack>, IntList> inventoryMap = inventoryIndexMap.get(i);
            NonNullList<ItemStack> inventory = inventoryMap.getFirst();
            IntList slots = inventoryMap.getSecond();

            for (int j = 0; j < slots.size(); j++) {
                IElectricItem chargable = GTCapabilityHelper.getElectricItem(inventory.get(slots.getInt(j)));
                if (chargable == null || chargable == cont) {
                    continue;
                }

                long attemptedChargeAmount = chargable.getTransferLimit() * CHARGE_INTERVAL;
                if (chargable.getCharge() >= chargable.getMaxCharge() || !cont.canUse(attemptedChargeAmount)) {
                    continue;
                }

                long delta = chargable.charge(attemptedChargeAmount, cont.getTier(), true, false);
                if (delta > 0) {
                    cont.discharge(delta, cont.getTier(), true, false, false);
                    charged = true;
                }
            }
        }

        if (charged) {
            player.inventoryMenu.sendAllDataToRemote();
        }
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        return IFederationArmor.addBonusAttributes(super.getAttributeModifiers(slot, stack), type);
    }

    @Override
    public int getArmorDisplay(Player player, ItemStack armor, EquipmentSlot slot) {
        return IFederationArmor.addBonusArmorDisplay(super.getArmorDisplay(player, armor, slot));
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return TEXTURE_1;
    }

    @Override
    public void addInfo(ItemStack itemStack, List<Component> lines) {
        super.addInfo(itemStack, lines);
        int capacity = SpaceFederationChestplateItem.getOxygenCapacity(itemStack);
        if (capacity > 0) {
            lines.add(Component.translatable("univom.tooltip.space_federation_armor.oxygen_stored",
                    FormattingUtil.formatNumbers(SpaceFederationChestplateItem.getOxygenAmount(itemStack)),
                    FormattingUtil.formatNumbers(capacity)));
        }
        lines.add(Component.translatable("univom.tooltip.space_federation_armor.temperature"));
        lines.add(Component.translatable("univom.tooltip.space_federation_armor.acid_rain"));
        lines.add(Component.translatable("univom.tooltip.space_federation_armor.full_set"));
    }
}
