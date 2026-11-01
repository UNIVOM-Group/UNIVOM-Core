package com.mrlacar.univom.integration.jade.provider;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.common.machine.multiblock.electric.FissionReactor;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class FissionReactorProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    @Override
    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            if (blockEntity.getMetaMachine() instanceof FissionReactor) {
                CompoundTag serverData = blockAccessor.getServerData();

                if (!serverData.getBoolean("formed")) return;

                double temp = serverData.getDouble("coreTemp");
                ChatFormatting tempColor = ChatFormatting.GREEN;
                if (temp >= 2000.0) {
                    tempColor = ChatFormatting.RED;
                } else if (temp >= 1000.0) {
                    tempColor = ChatFormatting.YELLOW;
                }

                iTooltip.add(Component.translatable("univom.multiblock.fission_reactor.temperature")
                        .append(Component.literal(String.format("%.1f K", temp)).withStyle(tempColor))
                        .append(Component.literal(" / 2500.0 K").withStyle(ChatFormatting.GRAY)));

                boolean isWorking = serverData.getBoolean("working");
                int insertion = serverData.getInt("insertion");
                boolean isMissingRod = serverData.getBoolean("missingRod");

                if (isMissingRod) {
                    iTooltip.add(Component.translatable("univom.multiblock.fission_reactor.missing_control_rod").withStyle(ChatFormatting.RED));
                } else {
                    iTooltip.add(Component.translatable("univom.multiblock.fission_reactor.insertion_rate", insertion).withStyle(ChatFormatting.AQUA));
                }

                int activeFuel = serverData.getInt("activeFuel");
                iTooltip.add(Component.translatable("univom.multiblock.fission_reactor.active_fuel", activeFuel).withStyle(ChatFormatting.LIGHT_PURPLE));
                iTooltip.add(Component.translatable("univom.multiblock.fission_reactor.coolant_flow_setting", serverData.getInt("maxFlow")).withStyle(ChatFormatting.BLUE));

                if (!isWorking || insertion >= 100) {
                    if (!isMissingRod) {
                        int decayHeat = serverData.getInt("decayHeat");
                        if (decayHeat > 0) {
                            iTooltip.add(Component.translatable("univom.multiblock.fission_reactor.scramming", decayHeat).withStyle(ChatFormatting.RED));
                        } else {
                            iTooltip.add(Component.translatable("univom.multiblock.fission_reactor.cold_shutdown").withStyle(ChatFormatting.GREEN));
                        }
                    } else {
                        iTooltip.add(Component.translatable("univom.multiblock.fission_reactor.scram_failed").withStyle(ChatFormatting.DARK_RED));
                    }
                } else if (activeFuel == 0) {
                    if (temp > 305.0) {
                        iTooltip.add(Component.translatable("univom.multiblock.fission_reactor.status.no_fuel_cooling").withStyle(ChatFormatting.GOLD));
                    } else {
                        iTooltip.add(Component.translatable("univom.multiblock.fission_reactor.status.no_fuel_standby").withStyle(ChatFormatting.GOLD));
                    }
                } else {
                    iTooltip.add(Component.translatable("univom.multiblock.fission_reactor.status.running").withStyle(ChatFormatting.GREEN));
                }
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            if (blockEntity.getMetaMachine() instanceof FissionReactor reactor) {
                if (!reactor.isFormed()) {
                    compoundTag.putBoolean("formed", false);
                    return;
                }
                compoundTag.putBoolean("formed", true);

                compoundTag.putDouble("coreTemp", reactor.getCoreTemperature());
                compoundTag.putInt("maxFlow", reactor.getMaxCoolantFlowRate());
                compoundTag.putInt("activeFuel", reactor.getActiveFuelCount());

                boolean isWorking = reactor.isWorkingEnabled();
                compoundTag.putBoolean("working", isWorking);

                int insertion = isWorking ? reactor.getControlRodPercent() : 100;
                compoundTag.putInt("insertion", insertion);

                boolean isMissingRod = reactor.isMissingControlRod();
                compoundTag.putBoolean("missingRod", isMissingRod);

                if ((!isWorking || insertion >= 100) && !isMissingRod) {
                    compoundTag.putInt("decayHeat", reactor.getDecayHeatPercent());
                }
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UNIVOM.id("fission_reactor");
    }
}