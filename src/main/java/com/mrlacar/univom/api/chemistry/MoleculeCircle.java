package com.mrlacar.univom.api.chemistry;

public record MoleculeCircle(float a00, float a01, float a10, float a11, int[] atoms) {

    public static MoleculeCircle scaled(float x, float y, int[] atoms) {
        return new MoleculeCircle(x, 0.0F, 0.0F, y, atoms);
    }
}
