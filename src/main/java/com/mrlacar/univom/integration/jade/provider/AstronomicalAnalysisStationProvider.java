package com.mrlacar.univom.integration.jade.provider;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.client.util.TooltipHelper;
import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.common.machine.multiblock.electric.AstronomicalAnalysisStation;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class AstronomicalAnalysisStationProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    @Override
    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            if (blockEntity.getMetaMachine() instanceof AstronomicalAnalysisStation) {
                CompoundTag serverData = blockAccessor.getServerData();

                if (!serverData.getBoolean("formed")) return;

                if (!serverData.getBoolean("bound")) {
                    iTooltip.add(Component.translatable("univom.jade.astronomical_analysis_station_not_bound").withStyle(ChatFormatting.RED));
                    return;
                }

                int[] pos = serverData.getIntArray("pos");
                iTooltip.add(Component.translatable("univom.jade.astronomical_analysis_station_bound_pos", pos[0], pos[1], pos[2])
                        .withStyle(TooltipHelper.RAINBOW_HSL_SLOW));
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            if (blockEntity.getMetaMachine() instanceof AstronomicalAnalysisStation station) {
                if (!station.isFormed()) {
                    compoundTag.putBoolean("formed", false);
                    return;
                }
                compoundTag.putBoolean("formed", true);

                var boundPos = station.getBoundRailgunPos();
                if (boundPos == null) {
                    compoundTag.putBoolean("bound", false);
                } else {
                    compoundTag.putBoolean("bound", true);
                    compoundTag.putIntArray("pos", new int[] { boundPos.getX(), boundPos.getY(), boundPos.getZ() });
                }
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UNIVOM.id("astronomical_analysis_station");
    }
}