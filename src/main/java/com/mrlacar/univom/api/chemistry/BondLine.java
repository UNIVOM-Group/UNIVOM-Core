package com.mrlacar.univom.api.chemistry;

import org.jetbrains.annotations.Nullable;

public enum BondLine {

    SOLID("solid", 1),
    DOTTED("dotted", 1),
    THICK("thick", 3),
    INWARD("inward", 3),
    OUTWARD("outward", 3);

    private static final BondLine[] VALUES = values();

    public final String jsonName;
    public final int thickness;

    BondLine(String jsonName, int thickness) {
        this.jsonName = jsonName;
        this.thickness = thickness;
    }

    @Nullable
    public static BondLine byName(String name) {
        for (BondLine line : VALUES) {
            if (line.jsonName.equals(name)) {
                return line;
            }
        }
        return null;
    }
}
