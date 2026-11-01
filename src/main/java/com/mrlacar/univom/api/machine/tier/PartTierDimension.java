package com.mrlacar.univom.api.machine.tier;

import com.mrlacar.univom.UNIVOM;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.common.block.CoilBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.lowdragmc.lowdraglib.utils.BlockInfo;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public final class PartTierDimension {

    public static final int NO_TIER = 0;

    private final String name;
    private final String key;
    private final String translationKey;
    private final String errorKey;
    private final Supplier<List<Block>> order;
    private final @Nullable Supplier<Block> capBlock;

    private volatile Resolved resolved;

    private PartTierDimension(String name, Supplier<List<Block>> order, @Nullable Supplier<Block> capBlock) {
        this.name = name;
        this.key = UNIVOM.MOD_ID + ":" + name;
        this.translationKey = "univom.part_tier." + name;
        this.errorKey = "univom.multiblock.pattern.error." + name;
        this.order = order;
        this.capBlock = capBlock;
    }

    public static PartTierDimension of(String name, Supplier<List<Block>> order) {
        return new PartTierDimension(name, order, null);
    }

    public static PartTierDimension capped(String name, Supplier<List<Block>> order, Supplier<Block> capBlock) {
        return new PartTierDimension(name, order, capBlock);
    }

    public static PartTierDimension coils(String name) {
        return capped(name,
                () -> GTCEuAPI.HEATING_COILS.entrySet().stream()
                        .sorted(Comparator.comparingInt(entry -> entry.getKey().getCoilTemperature()))
                        .<Block>map(entry -> entry.getValue().get())
                        .toList(),
                () -> GTCEuAPI.HEATING_COILS.get(CoilBlock.CoilType.TRITANIUM).get());
    }

    public String name() {
        return name;
    }

    public String key() {
        return key;
    }

    public String translationKey() {
        return translationKey;
    }

    public String errorKey() {
        return errorKey;
    }

    public int tierOf(BlockState state) {
        return resolve().tiers.getInt(state.getBlock());
    }

    public int effectiveTier(int tier) {
        int cap = resolve().cap;
        return cap > 0 && tier > cap ? cap : tier;
    }

    public BlockInfo[] candidates() {
        return resolve().candidates;
    }

    public int maxTier() {
        return resolve().blocks.size();
    }

    public Block blockAt(int tier) {
        List<Block> blocks = resolve().blocks;
        int index = Math.min(Math.max(tier, 1), blocks.size()) - 1;
        return blocks.get(index);
    }

    private Resolved resolve() {
        Resolved current = resolved;
        if (current != null) {
            return current;
        }
        return build();
    }

    private synchronized Resolved build() {
        if (resolved != null) {
            return resolved;
        }
        List<Block> blocks = order.get();
        Reference2IntMap<Block> tiers = new Reference2IntOpenHashMap<>(blocks.size());
        tiers.defaultReturnValue(NO_TIER);
        BlockInfo[] candidates = new BlockInfo[blocks.size()];
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            tiers.put(block, i + 1);
            candidates[i] = BlockInfo.fromBlockState(block.defaultBlockState());
        }
        int cap = capBlock == null ? 0 : tiers.getInt(capBlock.get());
        Resolved built = new Resolved(blocks, tiers, candidates, cap);
        resolved = built;
        return built;
    }

    private record Resolved(List<Block> blocks, Reference2IntMap<Block> tiers, BlockInfo[] candidates, int cap) {}
}
