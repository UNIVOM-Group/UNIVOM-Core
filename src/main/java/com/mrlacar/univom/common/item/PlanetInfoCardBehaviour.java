package com.mrlacar.univom.common.item;

import earth.terrarium.adastra.api.planets.Planet;
import earth.terrarium.adastra.common.planets.AdAstraData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.minecraft.resources.ResourceKey.create;

public class PlanetInfoCardBehaviour extends Item {
    public PlanetInfoCardBehaviour(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return (stack.hasTag() && stack.getTag().contains("PlanetID")) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag isAdvanced) {
        if (itemStack.hasTag() && itemStack.getTag().contains("PlanetInfo")) {
            String targetTag = itemStack.getTag().getString("PlanetInfo");
            ResourceLocation planetInfo = ResourceLocation.tryParse(targetTag);

            ResourceKey<Level> dimensionKey = create(Registries.DIMENSION, planetInfo);

            Component planetName = Component.translatableWithFallback("planet." + planetInfo.getNamespace() + "." + planetInfo.getPath(), planetInfo.getPath());

            tooltip.add(Component.translatable("univom.tooltip.planet_info_card.planet").append(planetName));

            Planet planet = AdAstraData.planets().get(dimensionKey);

            if (planet != null) {
                Component solarSystemName = Component.translatable("univom.gui.system_selection." + planet.solarSystem().getPath());

                tooltip.add(Component.translatable("univom.tooltip.planet_info_card.solar_system").append(solarSystemName).withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.translatable("univom.tooltip.planet_info_card.rocket_tier", planet.tier()));
                tooltip.add(Component.translatable("univom.tooltip.planet_info_card.gravity", planet.gravity() + " G"));
                tooltip.add(Component.translatable("univom.tooltip.planet_info_card.temperature", planet.temperature() + " °C"));
                tooltip.add(Component.translatable("univom.tooltip.planet_info_card.oxygen").append(planet.oxygen() ? Component.translatable("univom.tooltip.planet_info_card.oxygen.present") : Component.translatable("univom.tooltip.planet_info_card.oxygen.none")));
            } else {
                tooltip.add(Component.translatable("univom.tooltip.planet_info_card.error").withStyle(ChatFormatting.ITALIC));
            }

        } else {
            tooltip.add(Component.translatable("univom.tooltip.planet_info_card.empty.0").withStyle(ChatFormatting.ITALIC));
            tooltip.add(Component.translatable("univom.tooltip.planet_info_card.empty.1").withStyle(ChatFormatting.ITALIC));
        }

        super.appendHoverText(itemStack, level, tooltip, isAdvanced);
    }
}
