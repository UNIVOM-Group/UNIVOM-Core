package com.mrlacar.univom.api.machine.tier;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;

import net.minecraft.world.level.block.state.BlockState;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import java.util.ArrayList;
import java.util.List;

public final class PartTierShapes {

    private PartTierShapes() {}

    public static List<MultiblockShapeInfo> byTier(MultiblockMachineDefinition definition,
                                                   PartTierDimension[] dimensions) {
        BlockPattern pattern = definition.getPatternFactory().get();
        int[][] bounds = pattern.aisleRepetitions;
        int[] repetition = new int[bounds.length];
        for (int i = 0; i < bounds.length; i++) {
            repetition[i] = bounds[i][0];
        }
        BlockInfo[][][] base = pattern.getPreview(repetition);

        int tiers = 1;
        for (PartTierDimension dimension : dimensions) {
            tiers = Math.max(tiers, dimension.maxTier());
        }

        List<MultiblockShapeInfo> pages = new ArrayList<>(tiers);
        for (int tier = 1; tier <= tiers; tier++) {
            pages.add(new MultiblockShapeInfo(remap(base, dimensions, tier)));
        }
        return pages;
    }

    private static BlockInfo[][][] remap(BlockInfo[][][] base, PartTierDimension[] dimensions, int tier) {
        BlockInfo[][][] copy = new BlockInfo[base.length][][];
        for (int x = 0; x < base.length; x++) {
            copy[x] = new BlockInfo[base[x].length][];
            for (int y = 0; y < base[x].length; y++) {
                copy[x][y] = new BlockInfo[base[x][y].length];
                for (int z = 0; z < base[x][y].length; z++) {
                    copy[x][y][z] = substitute(base[x][y][z], dimensions, tier);
                }
            }
        }
        return copy;
    }

    private static BlockInfo substitute(BlockInfo info, PartTierDimension[] dimensions, int tier) {
        if (info == null || info.hasBlockEntity()) {
            return info;
        }
        BlockState state = info.getBlockState();
        for (PartTierDimension dimension : dimensions) {
            if (dimension.tierOf(state) == PartTierDimension.NO_TIER) {
                continue;
            }
            BlockState replacement = dimension.blockAt(tier).defaultBlockState();
            return replacement == state ? info : BlockInfo.fromBlockState(replacement);
        }
        return info;
    }
}
