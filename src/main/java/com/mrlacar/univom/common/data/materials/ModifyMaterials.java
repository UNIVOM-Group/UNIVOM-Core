package com.mrlacar.univom.common.data.materials;

import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.IngotProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.common.data.GTMaterials;

public class ModifyMaterials {
    public static void init() {
        GTMaterials.Beryllium.addFlags(MaterialFlags.GENERATE_LONG_ROD);

        GTMaterials.Californium.setProperty(PropertyKey.INGOT, new IngotProperty());
    }
}
