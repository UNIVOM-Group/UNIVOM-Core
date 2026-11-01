package com.mrlacar.univom.data.tooltips;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class CommonMachineTooltips {
    //Primitive
    public static final Component[] LARGE_PRIMITIVE_MACHINE = {
            Component.translatable("gtceu.multiblock.parallel", 8),
            Component.translatable("univom.machine.large_primitive_machine.tooltip")
    };
    //High-Pressure Steam//
    public static final Component[] HIGH_PRESSURE_STEAM_MULTIBLOCK_MACHINE = {
            Component.translatable("gtceu.multiblock.parallel", 16),
            Component.translatable("univom.machine.high_pressure_steam_multiblock_machine.tooltip")
    };
    //Parallel Machine
    public static final Component[] PARALLELIZABLE = {
            Component.translatable("gtceu.multiblock.parallelizable.tooltip")
    };
    //Wireless AE Machine
    public static final Component[] WIRELESS_AE_MACHINE = {
            Component.translatable("univom.machine.wireless_ae_machine.tooltip").withStyle(ChatFormatting.LIGHT_PURPLE)
    };
    //Wireless AE Machine
    public static final Component[] WIRELESS_ME_PATTERN_BUFFER = {
            Component.translatable("univom.machine.wireless_me_pattern_buffer.tooltip")
    };
}
