package com.mrlacar.univom.integration.jade.provider;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.integration.ae2.machine.MolecularConformationSynthesizerMEPatternBufferPartMachine;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElementHelper;

public class MolecularConformationSynthesizerMEPatternBufferPartMachineProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    @Override
    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            if (blockEntity.getMetaMachine() instanceof MolecularConformationSynthesizerMEPatternBufferPartMachine) {
                CompoundTag serverData = blockAccessor.getServerData();
                if (!serverData.getBoolean("formed")) return;
                readBufferTag(iTooltip, serverData);
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            if (blockEntity.getMetaMachine() instanceof MolecularConformationSynthesizerMEPatternBufferPartMachine buffer) {
                if (!buffer.isFormed()) {
                    compoundTag.putBoolean("formed", false);
                    return;
                }
                compoundTag.putBoolean("formed", true);
                writeBufferTag(compoundTag, buffer);
            }
        }
    }

    public static void writeBufferTag(CompoundTag compoundTag, MolecularConformationSynthesizerMEPatternBufferPartMachine buffer) {
        ListTag itemsTag = buffer.getJadeItemsTag(21);
        if (itemsTag != null && !itemsTag.isEmpty()) {
            compoundTag.put("items", itemsTag);
        }
    }

    public static void readBufferTag(ITooltip iTooltip, CompoundTag serverData) {
        if (!serverData.contains("items")) return;

        IElementHelper helper = iTooltip.getElementHelper();
        ListTag itemsTag = serverData.getList("items", Tag.TAG_COMPOUND);
        for (Tag t : itemsTag) {
            if (!(t instanceof CompoundTag ct)) continue;
            var stack = ItemStack.of(ct);
            var count = ct.getLong("real");
            if (!stack.isEmpty() && count > 0) {
                iTooltip.add(helper.smallItem(stack));

                Component text = Component.literal(" ")
                        .append(Component.literal(FormattingUtil.formatNumbers(count)).withStyle(ChatFormatting.DARK_PURPLE))
                        .append(Component.literal("× ").withStyle(ChatFormatting.WHITE))
                        .append(stack.getHoverName().copy().withStyle(ChatFormatting.GOLD));
                iTooltip.append(text);
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UNIVOM.id("molecular_conformation_synthesizer_me_pattern_buffer");
    }
}
