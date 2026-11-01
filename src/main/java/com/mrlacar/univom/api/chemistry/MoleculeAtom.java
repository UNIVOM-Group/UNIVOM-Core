package com.mrlacar.univom.api.chemistry;

import org.jetbrains.annotations.Nullable;

public record MoleculeAtom(int index, MoleculeSymbol center, @Nullable MoleculeSymbol above,
                           @Nullable MoleculeSymbol right, @Nullable MoleculeSymbol below,
                           @Nullable MoleculeSymbol left, float x, float y) {

    public boolean isInvisible() {
        return center.invisible();
    }
}
