package com.mrlacar.univom.integration.jade.provider;

import com.gregtechceu.gtceu.api.pipenet.Node;
import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.common.blockentity.PressurePipeBlockEntity;
import com.mrlacar.univom.common.pipelike.pressure.PressurePipeNet;
import com.mrlacar.univom.common.pipelike.pressure.PressurePipeProperties;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

public class PressureNetworkProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    private static final String DATA_TAG = "UnivomPressure";
    private static final double WARNING_RATIO = 0.9D;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData().getCompound(DATA_TAG);
        int rating = data.getInt("Rating");
        if (rating <= 0) {
            return;
        }
        long volume = data.getLong("Volume");
        double pressure = volume > 0 ? (double) data.getLong("Amount") / volume : 0.0D;
        tooltip.add(Component.translatable("univom.jade.pressure.pressure",
                String.format(Locale.ROOT, "%.1f", pressure), rating));
        tooltip.add(Component.translatable("univom.jade.pressure.gas", gasName(data.getString("Gas"))));
        tooltip.add(Component.translatable("univom.jade.pressure.volume", volume));
        if (data.contains("Safety", Tag.TAG_INT)) {
            tooltip.add(Component.translatable("univom.jade.pressure.safety", data.getInt("Safety")));
        }
        if (pressure > rating * WARNING_RATIO) {
            tooltip.add(Component.translatable("univom.jade.pressure.warning").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof PressurePipeBlockEntity pipe)) {
            return;
        }
        PressurePipeNet net = pipe.getPressureNet();
        if (net == null) {
            return;
        }
        CompoundTag data = new CompoundTag();
        data.putInt("Rating", net.getMinRating());
        data.putLong("Volume", net.getVolume());
        data.putLong("Amount", net.getAmount());
        if (net.getGas() != null) {
            data.putString("Gas", net.getGas().toString());
        }
        Node<PressurePipeProperties> node = net.getNodeAt(pipe.getPipePos());
        if (node != null && node.data.isSafetyValve()) {
            data.putInt("Safety", node.data.safetyPercent());
        }
        tag.put(DATA_TAG, data);
    }

    @Override
    public ResourceLocation getUid() {
        return UNIVOM.id("pressure_network");
    }

    private static Component gasName(String id) {
        ResourceLocation location = id.isEmpty() ? null : ResourceLocation.tryParse(id);
        Fluid fluid = location == null ? null : ForgeRegistries.FLUIDS.getValue(location);
        return fluid == null || fluid == Fluids.EMPTY ? Component.translatable("univom.jade.pressure.no_gas") :
                fluid.getFluidType().getDescription();
    }
}
