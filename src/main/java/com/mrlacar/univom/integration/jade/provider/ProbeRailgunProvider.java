package com.mrlacar.univom.integration.jade.provider;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.client.util.TooltipHelper;
import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.common.machine.multiblock.electric.ProbeRailgun;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class ProbeRailgunProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    @Override
    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            if (blockEntity.getMetaMachine() instanceof ProbeRailgun) {
                CompoundTag serverData = blockAccessor.getServerData();

                if (!serverData.getBoolean("formed")) return;

                if (!serverData.getBoolean("bound")) {
                    iTooltip.add(Component.translatable("univom.jade.probe_railgun_not_bound").withStyle(ChatFormatting.RED));
                    return;
                }

                int[] pos = serverData.getIntArray("pos");
                iTooltip.add(Component.translatable("univom.jade.probe_railgun_bound_pos", pos[0], pos[1], pos[2])
                        .withStyle(TooltipHelper.RAINBOW_HSL_SLOW));

                boolean isWorking = serverData.getBoolean("working");
                boolean isDeepSpace = serverData.getBoolean("deep_space");

                if (isDeepSpace) {
                    iTooltip.add(Component.translatable("univom.multiblock.probe_railgun.deep_space_mode"));
                } else {
                    iTooltip.add(Component.translatable("univom.multiblock.probe_railgun.targeted_mode"));
                }

                if (isWorking) {
                    int progress = serverData.getInt("progress");
                    int maxProgress = serverData.getInt("maxProgress");
                    double progressPercent = maxProgress > 0 ? (double) progress / maxProgress : 0.0;

                    String phaseKey;
                    if (progressPercent < 0.15) phaseKey = "univom.multiblock.probe_railgun.mission.phase.launch";
                    else if (progressPercent < 0.35) phaseKey = "univom.multiblock.probe_railgun.mission.phase.transit";
                    else if (progressPercent < 0.60) phaseKey = isDeepSpace ? "univom.multiblock.probe_railgun.mission.phase.scanning_deep" : "univom.multiblock.probe_railgun.mission.phase.scanning_target";
                    else if (progressPercent < 0.85) phaseKey = "univom.multiblock.probe_railgun.mission.phase.compiling";
                    else phaseKey = "univom.multiblock.probe_railgun.mission.phase.transmitting";

                    iTooltip.add(Component.translatable("univom.multiblock.probe_railgun.mission.phase.prefix", Component.translatable(phaseKey)));
                    iTooltip.add(Component.translatable("univom.multiblock.probe_railgun.mission.phase.progress", String.format("%.1f", progressPercent * 100)));
                } else {
                    iTooltip.add(Component.translatable("univom.multiblock.probe_railgun.mission.phase.prefix", Component.translatable("univom.multiblock.probe_railgun.mission.phase.standby")));
                }
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            if (blockEntity.getMetaMachine() instanceof ProbeRailgun silo) {
                if (!silo.isFormed()) {
                    compoundTag.putBoolean("formed", false);
                    return;
                }
                compoundTag.putBoolean("formed", true);

                var boundPos = silo.getBoundStationPos();
                if (boundPos == null) {
                    compoundTag.putBoolean("bound", false);
                } else {
                    compoundTag.putBoolean("bound", true);
                    compoundTag.putIntArray("pos", new int[] { boundPos.getX(), boundPos.getY(), boundPos.getZ() });
                }

                var recipeLogic = silo.getRecipeLogic();
                boolean isWorking = recipeLogic.isWorking();
                compoundTag.putBoolean("working", isWorking);
                compoundTag.putBoolean("deep_space", silo.isDeepSpaceMode());

                if (isWorking) {
                    compoundTag.putInt("progress", recipeLogic.getProgress());
                    compoundTag.putInt("maxProgress", recipeLogic.getMaxProgress());
                }
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UNIVOM.id("probe_railgun");
    }
}