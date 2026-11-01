package com.mrlacar.univom.utils;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.GTValues;

public class MachineUtils {
    public static final int[] WIRELESS_ENERGY_HATCH_TIERS = GTValues.tiersBetween(GTValues.LV, GTCEuAPI.isHighTier() ? GTValues.MAX : GTValues.UHV);
}
