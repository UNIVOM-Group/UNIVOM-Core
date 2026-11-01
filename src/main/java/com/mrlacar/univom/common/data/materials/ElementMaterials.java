package com.mrlacar.univom.common.data.materials;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.mrlacar.univom.common.data.ModElements;

import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet.METALLIC;
import static com.mrlacar.univom.common.data.ModMaterials.SiliconSolarGrade;

public class ElementMaterials {
    public static void init() {
        SiliconSolarGrade = new Material.Builder(GTCEu.id("silicon_solar_grade"))
                .ingot()
                .fluid()
                .color(0x707078)
                .secondaryColor(0x10293B)
                .iconSet(METALLIC)
                .element(ModElements.SiliconSolarGrade)
                .blast(2273)
                .buildAndRegister();
    }
}
