package com.mrlacar.univom.integration.jade.provider;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.integration.ae2.machine.feature.IGridConnectedMachine;
import com.mrlacar.univom.UNIVOM;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class GridConnectedMachineProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    public static final GridConnectedMachineProvider INSTANCE = new GridConnectedMachineProvider();

    private static final String NBT_KEY_ONLINE = "me_online";
    private static final Component TEXT_ONLINE = Component.translatable("univom.top.online").withStyle(ChatFormatting.GRAY);
    private static final Component TEXT_OFFLINE = Component.translatable("univom.top.offline").withStyle(ChatFormatting.GRAY);

    @Override
    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity && blockEntity.getMetaMachine() instanceof IGridConnectedMachine) {
            CompoundTag serverData = blockAccessor.getServerData();
            if (serverData.contains(NBT_KEY_ONLINE)) iTooltip.add(serverData.getBoolean(NBT_KEY_ONLINE) ? TEXT_ONLINE : TEXT_OFFLINE);
        }
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity && blockEntity.getMetaMachine() instanceof IGridConnectedMachine gridMachine) {
            compoundTag.putBoolean(NBT_KEY_ONLINE, gridMachine.isOnline());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UNIVOM.id("me_machine");
    }
}
