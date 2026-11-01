package com.mrlacar.univom.common.data.materials;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.DISABLE_DECOMPOSITION;
import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet.METALLIC;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.mrlacar.univom.common.data.ModMaterials.LanthanumHexaboride;

public class CommonMaterials {
    public static void init() {
        LanthanumHexaboride = new Material.Builder(GTCEu.id("lanthanum_hexaboride"))
                .ingot()
                .color(0x622675).secondaryColor(0x341142)
                .appendFlags(EXT2_METAL)
                .flags(DISABLE_DECOMPOSITION)
                .iconSet(METALLIC)
                .components(Lanthanum, 1, Boron, 6)
                .buildAndRegister();
    }
}
