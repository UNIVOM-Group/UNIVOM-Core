package com.mrlacar.univom.integration.jade.provider;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.client.util.TooltipHelper;
import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.api.machine.feature.IAEWirelessMachine;
import com.mrlacar.univom.integration.ae2.machine.WirelessMENetworkTransmitterMachine;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class AEWirelessMachineProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    @Override
    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            var metaMachine = blockEntity.getMetaMachine();

            if (metaMachine instanceof IAEWirelessMachine || metaMachine instanceof WirelessMENetworkTransmitterMachine) {
                CompoundTag serverData = blockAccessor.getServerData();
                String channel = serverData.getString("ae_target_channel");

                if (channel != null && !channel.isEmpty()) {
                    iTooltip.add(Component.translatable("univom.jade.wireless_ae_channel.channel", channel)
                            .withStyle(TooltipHelper.RAINBOW_HSL_SLOW));
                }
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            var metaMachine = blockEntity.getMetaMachine();

            if (metaMachine instanceof IAEWirelessMachine wirelessMachine) {
                compoundTag.putString("ae_target_channel", wirelessMachine.getTargetChannel());
            } else if (metaMachine instanceof WirelessMENetworkTransmitterMachine transmitter) {
                compoundTag.putString("ae_target_channel", transmitter.getChannel());
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UNIVOM.id("ae_wireless_machine");
    }
}
