package com.mrlacar.univom.api.chemistry;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import org.jetbrains.annotations.Nullable;

public record MoleculeSymbol(String text, boolean invisible, int explicitColor, @Nullable Material material) {

    public static final MoleculeSymbol INVISIBLE = new MoleculeSymbol("", true, 0, null);

    public boolean hasExplicitColor() {
        return (explicitColor & 0xFF000000) != 0;
    }
}
