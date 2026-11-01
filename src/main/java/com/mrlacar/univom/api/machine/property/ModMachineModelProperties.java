package com.mrlacar.univom.api.machine.property;

import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class ModMachineModelProperties {

    public static final int MAX_CASING_TIER = 5;

    public static final IntegerProperty CASING_TIER = IntegerProperty.create("casing_tier", 1, MAX_CASING_TIER);

    private ModMachineModelProperties() {}
}
