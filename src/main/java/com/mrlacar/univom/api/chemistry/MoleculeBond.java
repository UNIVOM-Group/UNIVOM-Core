package com.mrlacar.univom.api.chemistry;

public record MoleculeBond(int a, int b, boolean centered, BondLine[] lines, int ring, boolean aromaticDouble) {

    public static final int NO_RING = -1;
}
