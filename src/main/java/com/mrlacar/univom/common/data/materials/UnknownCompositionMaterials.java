package com.mrlacar.univom.common.data.materials;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.fluids.FluidBuilder;
import com.gregtechceu.gtceu.api.fluids.attribute.FluidAttributes;

import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.STICKY;
import static com.mrlacar.univom.common.data.ModMaterials.*;

public class UnknownCompositionMaterials {
    public static void init() {
        InfernalChelatingSap = new Material.Builder(GTCEu.id("infernal_chelating_sap"))
                .liquid(new FluidBuilder().temperature(800))
                .color(0x9E2A2B)
                .flags(STICKY)
                .buildAndRegister();

        ExtremophileChemolithotrophicSludge = new Material.Builder(GTCEu.id("extremophile_chemolithotrophic_sludge"))
                .liquid(new FluidBuilder().temperature(723))
                .color(0x8C9E13)
                .buildAndRegister();

        BioChelatedNetheriteComplex = new Material.Builder(GTCEu.id("bio_chelated_netherite_complex"))
                .liquid(new FluidBuilder().temperature(723).attribute(FluidAttributes.ACID))
                .color(0x3B282E)
                .buildAndRegister();

        CrimsonSaccharifiedLiquid = new Material.Builder(GTCEu.id("crimson_saccharified_liquid"))
                .liquid(new FluidBuilder().attribute(FluidAttributes.ACID))
                .color(0x8B1E22)
                .buildAndRegister();

        ConcentratedCrimsonSlurry = new Material.Builder(GTCEu.id("concentrated_crimson_slurry"))
                .liquid()
                .color(0xB8323D)
                .buildAndRegister();

        ConcentratedCrimsonLysate = new Material.Builder(GTCEu.id("concentrated_crimson_lysate"))
                .liquid(new FluidBuilder().temperature(723))
                .color(0x611417)
                .buildAndRegister();

        ActiveCytochromeCarrier = new Material.Builder(GTCEu.id("active_cytochrome_carrier"))
                .liquid()
                .color(0xCC4A2E)
                .buildAndRegister();

        WarpedEPSFluid = new Material.Builder(GTCEu.id("warped_eps_fluid"))
                .liquid()
                .color(0x167E86)
                .buildAndRegister();

        ImpureNetheriteScrap = new Material.Builder(GTCEu.id("impure_netherite_scrap"))
                .dust()
                .color(0x654740).secondaryColor(0x5D3E38)
                .buildAndRegister();

        DeslaggedNetherite = new Material.Builder(GTCEu.id("deslagged_netherite"))
                .dust()
                .color(0x7E6059).secondaryColor(0x654740)
                .buildAndRegister();

        PurifiedNetheriteConcentrate = new Material.Builder(GTCEu.id("purified_netherite_concentrate"))
                .dust()
                .color(0x654740).secondaryColor(0x2A2222)
                .buildAndRegister();

        BacterialMetabolicWastewater = new Material.Builder(GTCEu.id("bacterial_metabolic_wastewater"))
                .liquid()
                .color(0x9E9051)
                .buildAndRegister();

        OrganicImpurityWasteAcid = new Material.Builder(GTCEu.id("organic_impurity_waste_acid"))
                .liquid(new FluidBuilder().attribute(FluidAttributes.ACID))
                .color(0x8B7355)
                .buildAndRegister();

        DenaturedProteinWastewater = new Material.Builder(GTCEu.id("denatured_protein_wastewater"))
                .liquid()
                .color(0xD9CDBF)
                .buildAndRegister();

        ChitinousWasteAcid = new Material.Builder(GTCEu.id("chitinous_waste_acid"))
                .liquid(new FluidBuilder().attribute(FluidAttributes.ACID))
                .color(0x4C6A48)
                .buildAndRegister();

        FluorosilicateWastewater = new Material.Builder(GTCEu.id("fluorosilicate_wastewater"))
                .liquid(new FluidBuilder().attribute(FluidAttributes.ACID))
                .color(0x6A7B76)
                .buildAndRegister();

        CellularWasteSlurry = new Material.Builder(GTCEu.id("cellular_waste_slurry"))
                .liquid()
                .color(0x50312B)
                .buildAndRegister();

        SulfurRichOrganicWastewater = new Material.Builder(GTCEu.id("sulfur_rich_organic_wastewater"))
                .liquid()
                .color(0x9E9931)
                .buildAndRegister();

        BioleachingToxicWastewater = new Material.Builder(GTCEu.id("bioleaching_toxic_wastewater"))
                .liquid(new FluidBuilder().attribute(FluidAttributes.ACID))
                .color(0x42261E)
                .buildAndRegister();

        BiomineralizedOrganicWastewater = new Material.Builder(GTCEu.id("biomineralized_organic_wastewater"))
                .liquid()
                .color(0x2E494E)
                .buildAndRegister();

        DenaturedAqueousWaste = new Material.Builder(GTCEu.id("denatured_aqueous_waste"))
                .liquid()
                .color(0x3B4540)
                .buildAndRegister();

        IndustrialWastewater = new Material.Builder(GTCEu.id("industrial_wastewater"))
                .liquid()
                .color(0x5A635C)
                .buildAndRegister();
    }
}
