package com.mrlacar.univom.api.pattern;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.machine.multiblock.IBatteryData;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.common.block.BatteryBlock;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.mrlacar.univom.common.machine.multiblock.electric.PowerMatrix;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Supplier;

import static com.gregtechceu.gtceu.common.machine.multiblock.electric.PowerSubstationMachine.PMC_BATTERY_HEADER;

public class ModPredicates {
    public static TraceabilityPredicate superPowerSubstationBatteries() {
        return new TraceabilityPredicate(blockWorldState -> {
            BlockState state = blockWorldState.getBlockState();
            for (Map.Entry<IBatteryData, Supplier<BatteryBlock>> entry : GTCEuAPI.PSS_BATTERIES.entrySet()) {
                if (state.is(entry.getValue().get())) {
                    IBatteryData battery = entry.getKey();
                    if (battery.getTier() != -1 && battery.getCapacity() > 0) {
                        String key = PMC_BATTERY_HEADER + battery.getBatteryName();
                        PowerMatrix.BatteryMatchWrapper wrapper = blockWorldState.getMatchContext().get(key);
                        if (wrapper == null) wrapper = new PowerMatrix.BatteryMatchWrapper(battery);
                        blockWorldState.getMatchContext().set(key, wrapper.increment());
                    }
                    return true;
                }
            }
            return false;
        }, () -> GTCEuAPI.PSS_BATTERIES.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> entry.getKey().getTier()))
                .map(entry -> new BlockInfo(entry.getValue().get().defaultBlockState(), null))
                .toArray(BlockInfo[]::new))
                .addTooltips(Component.translatable("gtceu.multiblock.pattern.error.batteries"));
    }
}
