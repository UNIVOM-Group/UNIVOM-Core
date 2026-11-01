package com.mrlacar.univom.common.data.materials;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty;
import com.gregtechceu.gtceu.api.fluids.FluidBuilder;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.*;
import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet.METALLIC;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.mrlacar.univom.common.data.ModMaterials.*;

public class AlloyMaterials {
    public static void init() {
        NCCTSteel = new Material.Builder(GTCEu.id("ncct_steel"))
                .ingot(2)
                .liquid(new FluidBuilder().temperature(2480))
                .color(0x525266).secondaryColor(0x2d2d39)
                .appendFlags(STD_METAL, GENERATE_FRAME)
                .flags(DECOMPOSITION_BY_CENTRIFUGING)
                .iconSet(METALLIC)
                .components(Steel, 4, Nickel, 1, Cobalt, 1, Chromium, 1, Titanium, 1)
                .blast(b -> b.temp(2400, BlastProperty.GasTier.LOW)
                        .blastStats(VA[HV], 1600))
                .buildAndRegister();

        FluixTitaniumAlloy = new Material.Builder(GTCEu.id("fluix_titanium_alloy"))
                .ingot(2)
                .liquid(new FluidBuilder().temperature(3200))
                .color(0x874ACD).secondaryColor(0x32286B)
                .appendFlags(STD_METAL, GENERATE_FRAME)
                .flags(DISABLE_DECOMPOSITION)
                .iconSet(METALLIC)
                .blast(b -> b.temp(3200, BlastProperty.GasTier.MID)
                        .blastStats(VA[IV], 1200))
                .buildAndRegister();

        Cyanel914 = new Material.Builder(GTCEu.id("cyanel_914"))
                .ingot(5).fluid()
                .color(0x589F9F)
                .appendFlags(STD_METAL, GENERATE_PLATE, GENERATE_FRAME)
                .components(Niobium, 8, Tungsten, 3, Zirconium, 2, Titanium, 1)
                .iconSet(METALLIC)
                .blast(b -> b.temp(4200, BlastProperty.GasTier.HIGH)
                        .blastStats(VA[LuV], 1200))
                .buildAndRegister();
    }
}
