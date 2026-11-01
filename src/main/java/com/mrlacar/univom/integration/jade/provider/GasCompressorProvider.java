package com.mrlacar.univom.integration.jade.provider;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.common.machine.electric.GasCompressor;
import com.mrlacar.univom.common.pipelike.pressure.GasCompression;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class GasCompressorProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    private static final String DATA_TAG = "UnivomGasCompressor";

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(DATA_TAG, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag tag = data.getCompound(DATA_TAG);
        GasCompression.Status status = GasCompression.Status.byOrdinal(tag.getInt("Status"));
        tooltip.add(Component.translatable("univom.jade.gas_compressor.target", tag.getInt("Target")));
        if (status == GasCompression.Status.WORKING) {
            tooltip.add(Component.translatable("univom.jade.gas_compressor.flow", tag.getLong("Flow")));
        } else {
            tooltip.add(Component.translatable(status.getTranslationKey()).withStyle(status.getColor()));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof IMachineBlockEntity holder &&
                holder.getMetaMachine() instanceof GasCompressor machine) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Target", machine.getTargetPressure());
            tag.putLong("Flow", machine.getLastFlow());
            tag.putInt("Status", machine.getStatus().ordinal());
            data.put(DATA_TAG, tag);
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UNIVOM.id("gas_compressor");
    }
}
