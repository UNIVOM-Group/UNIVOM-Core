package com.mrlacar.univom.integration.jade.provider;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.client.util.TooltipHelper;
import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.common.machine.multiblock.part.WirelessOpticalComputationHatchMachine;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class WirelessOpticalComputationHatchMachineProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    @Override
    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            if (blockEntity.getMetaMachine() instanceof WirelessOpticalComputationHatchMachine) {
                CompoundTag serverData = blockAccessor.getServerData();

                String channel = serverData.getString("channel");
                int status = serverData.getInt("connection_status");

                if (channel != null && !channel.isEmpty()) {
                    iTooltip.add(Component.translatable("univom.jade.wireless_optical_hatch.channel", channel)
                            .withStyle(TooltipHelper.RAINBOW_HSL_SLOW));
                }

                switch (status) {
                    case 1 -> iTooltip.add(Component.translatable("univom.jade.wireless_optical_hatch.offline")
                            .withStyle(ChatFormatting.GRAY));
                    case 2 -> iTooltip.add(Component.translatable("univom.jade.wireless_optical_hatch.linked_source")
                            .withStyle(ChatFormatting.GRAY));
                    case 3 -> iTooltip.add(Component.translatable("univom.jade.wireless_optical_hatch.conflict_target")
                            .withStyle(ChatFormatting.GRAY));
                    case 4 -> iTooltip.add(Component.translatable("univom.jade.wireless_optical_hatch.waiting_target")
                            .withStyle(ChatFormatting.GRAY));
                    case 5 -> iTooltip.add(Component.translatable("univom.jade.wireless_optical_hatch.linked_target")
                            .withStyle(ChatFormatting.GRAY));
                    case 6 -> iTooltip.add(Component.translatable("univom.jade.wireless_optical_hatch.conflict_source")
                            .withStyle(ChatFormatting.GRAY));
                    default -> iTooltip.add(Component.translatable("univom.jade.wireless_optical_hatch.unconfigured")
                            .withStyle(ChatFormatting.GRAY));
                }
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            if (blockEntity.getMetaMachine() instanceof WirelessOpticalComputationHatchMachine hatch) {
                compoundTag.putString("channel", hatch.getChannel());
                compoundTag.putInt("connection_status", hatch.getConnectionStatus());
                compoundTag.putBoolean("is_transmitter", hatch.isTransmitter());
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UNIVOM.id("wireless_computation_hatch");
    }
}
