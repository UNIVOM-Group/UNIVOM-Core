package com.mrlacar.univom.integration.adastra;

import com.mrlacar.univom.common.item.armor.AdvancedSpaceFederationArmor;
import com.mrlacar.univom.common.item.armor.IFederationArmor;
import com.mrlacar.univom.common.item.armor.SpaceFederationChestplateItem;
import com.mrlacar.univom.config.ConfigHolder;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import earth.terrarium.adastra.api.events.AdAstraEvents;
import earth.terrarium.adastra.api.systems.OxygenApi;

public final class SpaceFederationEnvironmentHandler {

    private SpaceFederationEnvironmentHandler() {}

    public static void init() {
        SpaceFederationChestplateItem.setBreathableAirCheck(OxygenApi.API::hasOxygen);
        AdAstraEvents.OxygenTickEvent.register(SpaceFederationEnvironmentHandler::onOxygenTick);
        AdAstraEvents.HotTemperatureTickEvent.register(SpaceFederationEnvironmentHandler::onTemperatureTick);
        AdAstraEvents.ColdTemperatureTickEvent.register(SpaceFederationEnvironmentHandler::onTemperatureTick);
        AdAstraEvents.AcidRainTickEvent.register(SpaceFederationEnvironmentHandler::onAcidRainTick);
    }

    private static boolean onOxygenTick(ServerLevel level, LivingEntity entity) {
        if (!IFederationArmor.hasFullSet(entity)) {
            return true;
        }
        if (SpaceFederationChestplateItem.getOxygenAmount(entity) < AdvancedSpaceFederationArmor.OXYGEN_PER_CYCLE) {
            return true;
        }
        return !IFederationArmor.canUseEnergy(entity, EquipmentSlot.CHEST,
                ConfigHolder.INSTANCE.spaceFederationArmorSetting.oxygenEnergyCost);
    }

    private static boolean onTemperatureTick(ServerLevel level, LivingEntity entity) {
        if (!IFederationArmor.hasFullSet(entity)) {
            return true;
        }
        return !IFederationArmor.consumeEnergy(entity, EquipmentSlot.CHEST,
                ConfigHolder.INSTANCE.spaceFederationArmorSetting.temperatureEnergyCost);
    }

    private static boolean onAcidRainTick(ServerLevel level, LivingEntity entity) {
        if (!IFederationArmor.hasFullSet(entity)) {
            return true;
        }
        return !IFederationArmor.consumeEnergy(entity, EquipmentSlot.CHEST,
                ConfigHolder.INSTANCE.spaceFederationArmorSetting.acidRainEnergyCost);
    }
}
