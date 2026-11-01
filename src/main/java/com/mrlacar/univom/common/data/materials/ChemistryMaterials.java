package com.mrlacar.univom.common.data.materials;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.HazardProperty;
import com.gregtechceu.gtceu.api.fluids.FluidBuilder;
import com.gregtechceu.gtceu.api.fluids.attribute.FluidAttributes;
import com.gregtechceu.gtceu.common.data.GTMedicalConditions;

import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.DISABLE_DECOMPOSITION;
import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet.METALLIC;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.mrlacar.univom.common.data.ModMaterials.*;

public class ChemistryMaterials {
    public static void init() {
        SiliconTetrachloride = new Material.Builder(GTCEu.id("silicon_tetrachloride"))
                .liquid(new FluidBuilder().customStill())
                .color(0xD40D5C)
                .flags(DISABLE_DECOMPOSITION)
                .hazard(HazardProperty.HazardTrigger.SKIN_CONTACT, GTMedicalConditions.CHEMICAL_BURNS)
                .components(Silicon, 1, Chlorine, 4)
                .buildAndRegister();

        ZincChloride = new Material.Builder(GTCEu.id("zinc_chloride"))
                .dust()
                .color(0xB6CEFF).secondaryColor(0x6598FF)
                .hazard(HazardProperty.HazardTrigger.SKIN_CONTACT, GTMedicalConditions.CHEMICAL_BURNS)
                .iconSet(METALLIC)
                .components(Zinc, 1, Chlorine, 2)
                .buildAndRegister();

        SodiumBicarbonateSolution = new Material.Builder(GTCEu.id("sodium_bicarbonate_solution"))
                .liquid(new FluidBuilder().customStill())
                .flags(DISABLE_DECOMPOSITION)
                .components(SodiumBicarbonate, 1, Water, 1)
                .buildAndRegister();

        SodiumAcetate = new Material.Builder(GTCEu.id("sodium_acetate"))
                .dust()
                .color(0xDFE3E5).secondaryColor(0xF5F5F5)
                .flags(DISABLE_DECOMPOSITION)
                .components(Carbon, 2, Hydrogen, 3, Sodium, 1, Oxygen, 2)
                .buildAndRegister();

        SodiumAcetateSolution = new Material.Builder(GTCEu.id("sodium_acetate_solution"))
                .liquid(new FluidBuilder().customStill())
                .flags(DISABLE_DECOMPOSITION)
                .components(SodiumAcetate, 1, Water, 1)
                .buildAndRegister();

        AmmoniaSolution = new Material.Builder(GTCEu.id("ammonia_solution"))
                .liquid(new FluidBuilder().customStill())
                .flags(DISABLE_DECOMPOSITION)
                .hazard(HazardProperty.HazardTrigger.SKIN_CONTACT, GTMedicalConditions.CHEMICAL_BURNS)
                .components(Ammonia, 1, Water, 1)
                .buildAndRegister();

        AmmoniumAcetate = new Material.Builder(GTCEu.id("ammonium_acetate"))
                .dust()
                .color(0xD4D6D8).secondaryColor(0xECECEC)
                .flags(DISABLE_DECOMPOSITION)
                .components(Carbon, 2, Hydrogen, 7, Nitrogen, 1, Oxygen, 2)
                .buildAndRegister();

        AmmoniumAcetateSolution = new Material.Builder(GTCEu.id("ammonium_acetate_solution"))
                .liquid(new FluidBuilder().customStill())
                .flags(DISABLE_DECOMPOSITION)
                .components(AmmoniumAcetate, 1, Water, 1)
                .buildAndRegister();

        AmmoniumPhosphate = new Material.Builder(GTCEu.id("ammonium_phosphate"))
                .dust()
                .color(0xCAD1A3).secondaryColor(0xE3E8C8)
                .flags(DISABLE_DECOMPOSITION)
                .components(Nitrogen, 3, Hydrogen, 12, Phosphorus, 1, Oxygen, 4)
                .buildAndRegister()
                .setFormula("(NH4)3PO4");

        AmmoniumPhosphateSolution = new Material.Builder(GTCEu.id("ammonium_phosphate_solution"))
                .liquid(new FluidBuilder().customStill())
                .flags(DISABLE_DECOMPOSITION)
                .components(AmmoniumPhosphate, 1, Water, 1)
                .buildAndRegister();

        AmmoniumSulfate = new Material.Builder(GTCEu.id("ammonium_sulfate"))
                .dust()
                .color(0xDEE2E8).secondaryColor(0xF5F6F8)
                .flags(DISABLE_DECOMPOSITION)
                .components(Nitrogen, 2, Hydrogen, 8, Sulfur, 1, Oxygen, 4)
                .buildAndRegister()
                .setFormula("(NH4)2SO4");

        AmmoniumSulfateSolution = new Material.Builder(GTCEu.id("ammonium_sulfate_solution"))
                .liquid(new FluidBuilder().customStill())
                .flags(DISABLE_DECOMPOSITION)
                .components(AmmoniumSulfate, 1, Water, 1)
                .buildAndRegister();

        SodiumHydroxideSolution = new Material.Builder(GTCEu.id("sodium_hydroxide_solution"))
                .liquid(new FluidBuilder().customStill())
                .flags(DISABLE_DECOMPOSITION)
                .hazard(HazardProperty.HazardTrigger.SKIN_CONTACT, GTMedicalConditions.CHEMICAL_BURNS)
                .components(SodiumHydroxide, 1, Water, 1)
                .buildAndRegister();

        TrisodiumPhosphate = new Material.Builder(GTCEu.id("trisodium_phosphate"))
                .dust()
                .color(0xE2E2E2).secondaryColor(0xFAFAFA)
                .flags(DISABLE_DECOMPOSITION)
                .hazard(HazardProperty.HazardTrigger.SKIN_CONTACT, GTMedicalConditions.CHEMICAL_BURNS)
                .components(Sodium, 3, Phosphorus, 1, Oxygen, 4)
                .buildAndRegister();

        TrisodiumPhosphateSolution = new Material.Builder(GTCEu.id("trisodium_phosphate_solution"))
                .liquid(new FluidBuilder().customStill())
                .flags(DISABLE_DECOMPOSITION)
                .hazard(HazardProperty.HazardTrigger.SKIN_CONTACT, GTMedicalConditions.CHEMICAL_BURNS)
                .components(TrisodiumPhosphate, 1, Water, 1)
                .buildAndRegister();

        SodiumSulfate = new Material.Builder(GTCEu.id("sodium_sulfate"))
                .dust()
                .color(0xD6DFEA).secondaryColor(0xF0F4F8)
                .flags(DISABLE_DECOMPOSITION)
                .components(Sodium, 2, Sulfur, 1, Oxygen, 4)
                .buildAndRegister();

        SodiumSulfateSolution = new Material.Builder(GTCEu.id("sodium_sulfate_solution"))
                .liquid(new FluidBuilder().customStill())
                .flags(DISABLE_DECOMPOSITION)
                .components(SodiumSulfate, 1, Water, 1)
                .buildAndRegister();

        PotassiumHydroxideSolution = new Material.Builder(GTCEu.id("potassium_hydroxide_solution"))
                .liquid(new FluidBuilder().customStill())
                .flags(DISABLE_DECOMPOSITION)
                .hazard(HazardProperty.HazardTrigger.SKIN_CONTACT, GTMedicalConditions.CHEMICAL_BURNS)
                .components(PotassiumHydroxide, 1, Water, 1)
                .buildAndRegister();

        PotassiumSulfateSolution = new Material.Builder(GTCEu.id("potassium_sulfate_solution"))
                .liquid(new FluidBuilder().customStill())
                .flags(DISABLE_DECOMPOSITION)
                .components(PotassiumSulfate, 1, Water, 1)
                .buildAndRegister();

        TrifluoroaceticAcid = new Material.Builder(GTCEu.id("trifluoroacetic_acid"))
                .liquid(new FluidBuilder().attribute(FluidAttributes.ACID))
                .color(0x99E6CC)
                .flags(DISABLE_DECOMPOSITION)
                .hazard(HazardProperty.HazardTrigger.SKIN_CONTACT, GTMedicalConditions.CHEMICAL_BURNS)
                .components(Carbon, 2, Hydrogen, 1, Fluorine, 3, Oxygen, 2)
                .buildAndRegister();

        SodiumAmide = new Material.Builder(GTCEu.id("sodium_amide"))
                .dust()
                .color(0x8A9A7C).secondaryColor(0xA6B599)
                .flags(DISABLE_DECOMPOSITION)
                .components(Sodium, 1, Nitrogen, 1, Hydrogen, 2)
                .buildAndRegister();

        SodiumAmideAmmoniaSolution = new Material.Builder(GTCEu.id("sodium_amide_ammonia_solution"))
                .liquid()
                .color(0x99A88F)
                .flags(DISABLE_DECOMPOSITION)
                .hazard(HazardProperty.HazardTrigger.SKIN_CONTACT, GTMedicalConditions.CHEMICAL_BURNS)
                .components(SodiumAmide, 1, Ammonia, 1)
                .buildAndRegister();

        SodiumTrifluoroacetateSolution = new Material.Builder(GTCEu.id("sodium_trifluoroacetate_solution"))
                .liquid()
                .color(0x7DA685)
                .flags(DISABLE_DECOMPOSITION)
                .components(Carbon, 2, Fluorine, 3, Sodium, 1, Oxygen, 2)
                .buildAndRegister();

        SodiumFluoride = new Material.Builder(GTCEu.id("sodium_fluoride"))
                .dust()
                .color(0xD2D4E3).secondaryColor(0xEEEEF5)
                .flags(DISABLE_DECOMPOSITION)
                .components(Sodium, 1, Fluorine, 1)
                .buildAndRegister();

        Oleum = new Material.Builder(GTCEu.id("oleum"))
                .liquid(new FluidBuilder().customStill().attribute(FluidAttributes.ACID))
                .flags(DISABLE_DECOMPOSITION)
                .hazard(HazardProperty.HazardTrigger.SKIN_CONTACT, GTMedicalConditions.CHEMICAL_BURNS)
                .components(Hydrogen, 2, Sulfur, 2, Oxygen, 7)
                .buildAndRegister();

        HydrogenChloride = new Material.Builder(GTCEu.id("hydrogen_chloride"))
                .gas(new FluidBuilder().attribute(FluidAttributes.ACID))
                .color(0xB9B8B7)
                .hazard(HazardProperty.HazardTrigger.SKIN_CONTACT, GTMedicalConditions.CHEMICAL_BURNS)
                .components(Hydrogen, 1, Chlorine, 1)
                .buildAndRegister();

        ChlorosulfuricAcid = new Material.Builder(GTCEu.id("chlorosulfuric_acid"))
                .liquid(new FluidBuilder().attribute(FluidAttributes.ACID))
                .color(0xC6E823)
                .flags(DISABLE_DECOMPOSITION)
                .hazard(HazardProperty.HazardTrigger.SKIN_CONTACT, GTMedicalConditions.CHEMICAL_BURNS)
                .components(Hydrogen, 1, Sulfur, 1, Oxygen, 3, Chlorine, 1)
                .buildAndRegister();

        IronIISulfate = new Material.Builder(GTCEu.id("iron_ii_sulfate"))
                .dust()
                .color(0x568268).secondaryColor(0x3B5947)
                .components(Iron, 1, Sulfur, 1, Oxygen, 4)
                .buildAndRegister();

        CrudeDilutedAceticAcid = new Material.Builder(GTCEu.id("crude_diluted_acetic_acid"))
                .liquid(new FluidBuilder().attribute(FluidAttributes.ACID))
                .color(0x9B8A74)
                .flags(DISABLE_DECOMPOSITION)
                .buildAndRegister();
    }
}
