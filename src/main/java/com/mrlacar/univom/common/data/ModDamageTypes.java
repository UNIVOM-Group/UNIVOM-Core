package com.mrlacar.univom.common.data;

import com.gregtechceu.gtceu.api.data.damagesource.DamageTypeData;
import net.minecraft.tags.DamageTypeTags;

public class ModDamageTypes {
    public static final DamageTypeData IRRADIATION = new DamageTypeData.Builder()
            .simpleId("irradiation")
            .tag(DamageTypeTags.BYPASSES_ARMOR)
            .build();
}
