package com.mrlacar.univom.api.machine.multiblock;

import com.mrlacar.univom.api.machine.tier.PartTierDimension;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.util.PatternMatchContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class PartTierWorkableElectricMultiblockMachine extends WorkableElectricMultiblockMachine {

    private static final String TIER_LINE_KEY = "univom.multiblock.part_tier";

    private final PartTierDimension[] dimensions;
    private final int[] partTiers;

    public PartTierWorkableElectricMultiblockMachine(IMachineBlockEntity holder, PartTierDimension[] dimensions,
                                                     Object... args) {
        super(holder, args);
        this.dimensions = dimensions;
        this.partTiers = new int[dimensions.length];
    }

    @Nullable
    public static BlockState partAppearance(IMultiController controller, IMultiPart part, Direction side) {
        return controller.self() instanceof PartTierWorkableElectricMultiblockMachine machine ?
                machine.getAppearanceState() : null;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        PatternMatchContext context = getMultiblockState().getMatchContext();
        for (int i = 0; i < dimensions.length; i++) {
            partTiers[i] = context.getOrDefault(dimensions[i].key(), PartTierDimension.NO_TIER);
        }
        applyRenderState();
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        Arrays.fill(partTiers, PartTierDimension.NO_TIER);
        applyRenderState();
    }

    public int getPartTier(PartTierDimension dimension) {
        for (int i = 0; i < dimensions.length; i++) {
            if (dimensions[i] == dimension) {
                return partTiers[i];
            }
        }
        return PartTierDimension.NO_TIER;
    }

    public int getPartTier(String key) {
        for (int i = 0; i < dimensions.length; i++) {
            if (dimensions[i].key().equals(key) || dimensions[i].name().equals(key)) {
                return partTiers[i];
            }
        }
        return PartTierDimension.NO_TIER;
    }

    public int getEffectivePartTier(PartTierDimension dimension) {
        return dimension.effectiveTier(getPartTier(dimension));
    }

    protected void applyRenderState() {}

    @Nullable
    public BlockState getAppearanceState() {
        return null;
    }

    @NotNull
    @Override
    public BlockState getBlockAppearance(BlockState state, BlockAndTintGetter level, BlockPos pos, Direction side,
                                         BlockState sourceState, BlockPos sourcePos) {
        BlockState resolved = super.getBlockAppearance(state, level, pos, side, sourceState, sourcePos);
        if (resolved != getDefinition().getAppearance().get()) {
            return resolved;
        }
        BlockState appearance = getAppearanceState();
        return appearance == null ? resolved : appearance;
    }

    public void appendTierLines(List<Component> lines) {
        for (int i = 0; i < dimensions.length; i++) {
            if (partTiers[i] != PartTierDimension.NO_TIER) {
                lines.add(Component.translatable(TIER_LINE_KEY,
                        Component.translatable(dimensions[i].translationKey()), partTiers[i]));
            }
        }
    }
}
