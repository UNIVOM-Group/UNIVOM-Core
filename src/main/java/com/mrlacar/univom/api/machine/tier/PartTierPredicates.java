package com.mrlacar.univom.api.machine.tier;

import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.error.PatternStringError;

import net.minecraft.network.chat.Component;

public final class PartTierPredicates {

    private PartTierPredicates() {}

    public static TraceabilityPredicate uniformTier(PartTierDimension dimension) {
        return new TraceabilityPredicate(blockWorldState -> {
            int tier = dimension.tierOf(blockWorldState.getBlockState());
            if (tier == PartTierDimension.NO_TIER) {
                return false;
            }
            Integer current = blockWorldState.getMatchContext().getOrPut(dimension.key(), tier);
            if (current.intValue() != tier) {
                blockWorldState.setError(new PatternStringError(dimension.errorKey()));
                return false;
            }
            return true;
        }, dimension::candidates).addTooltips(Component.translatable(dimension.errorKey()));
    }
}
