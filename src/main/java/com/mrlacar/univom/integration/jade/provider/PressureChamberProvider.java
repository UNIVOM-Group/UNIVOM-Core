package com.mrlacar.univom.integration.jade.provider;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.common.machine.multiblock.unique.PressureChamber.PressureChamber;
import com.mrlacar.univom.common.pipelike.pressure.PressurePipeNet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

public class PressureChamberProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    private static final String DATA_TAG = "UnivomPressureChamber";

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(DATA_TAG, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag tag = data.getCompound(DATA_TAG);
        int chamberRating = tag.getInt("ChamberRating");
        if (chamberRating > 0) {
            tooltip.add(Component.translatable("univom.machine.pressure_chamber.rating", chamberRating));
        }
        long volume = tag.getLong("Volume");
        if (volume > 0) {
            tooltip.add(Component.translatable("univom.jade.pressure.pressure",
                    String.format(Locale.ROOT, "%.1f", (double) tag.getLong("Amount") / volume),
                    tag.getInt("Rating")));
            String gas = tag.getString("Gas");
            tooltip.add(Component.translatable("univom.jade.pressure.gas",
                    PressureChamber.gasName(gas.isEmpty() ? null : ResourceLocation.tryParse(gas))));
        }
        int parallel = tag.getInt("Parallel");
        if (parallel > 1) {
            tooltip.add(Component.translatable("univom.jade.pressure_chamber.parallel", parallel));
        }
        PressureChamber.PressureStatus status = PressureChamber.PressureStatus.byOrdinal(tag.getInt("Status"));
        if (status != PressureChamber.PressureStatus.READY) {
            tooltip.add(Component.translatable(status.getTranslationKey()).withStyle(status.getColor()));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof IMachineBlockEntity holder &&
                holder.getMetaMachine() instanceof PressureChamber chamber && chamber.isFormed()) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("ChamberRating", chamber.getRating());
            PressurePipeNet net = chamber.getLinkedNet();
            if (net != null) {
                tag.putLong("Volume", net.getVolume());
                tag.putLong("Amount", net.getAmount());
                tag.putInt("Rating", net.getMinRating());
                if (net.getGas() != null) {
                    tag.putString("Gas", net.getGas().toString());
                }
            }
            tag.putInt("Parallel", chamber.getActiveParallel());
            tag.putInt("Status", chamber.getPressureStatus().ordinal());
            data.put(DATA_TAG, tag);
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UNIVOM.id("pressure_chamber");
    }
}
