package com.mrlacar.univom.mixin.gtceu.api.pattern;

import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockPattern.class)
public interface BlockPatternAccessor {

    @Accessor("blockMatches")
    TraceabilityPredicate[][][] univom$getBlockMatches();

    @Accessor("fingerLength")
    int univom$getFingerLength();

    @Accessor("thumbLength")
    int univom$getThumbLength();

    @Accessor("palmLength")
    int univom$getPalmLength();

    @Accessor("centerOffset")
    int[] univom$getCenterOffset();
}
