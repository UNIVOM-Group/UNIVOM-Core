package com.mrlacar.univom.common.data;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.mrlacar.univom.common.data.materials.*;

public class ModMaterials {
    //Element Materials//
    public static Material SiliconSolarGrade;

    //Alloy Materials//
    public static Material FluixTitaniumAlloy;
    public static Material NCCTSteel;
    public static Material Cyanel914;

    //Common Materials//
    public static Material LanthanumHexaboride;

    //Chemistry Materials//
    public static Material SiliconTetrachloride;
    public static Material ZincChloride;
    public static Material SodiumBicarbonateSolution;
    public static Material SodiumAcetate;
    public static Material SodiumAcetateSolution;
    public static Material AmmoniaSolution;
    public static Material AmmoniumAcetate;
    public static Material AmmoniumAcetateSolution;
    public static Material AmmoniumPhosphate;
    public static Material AmmoniumPhosphateSolution;
    public static Material AmmoniumSulfate;
    public static Material AmmoniumSulfateSolution;
    public static Material SodiumHydroxideSolution;
    public static Material TrisodiumPhosphate;
    public static Material TrisodiumPhosphateSolution;
    public static Material SodiumSulfate;
    public static Material SodiumSulfateSolution;
    public static Material PotassiumHydroxideSolution;
    public static Material PotassiumSulfateSolution;
    public static Material TrifluoroaceticAcid;
    public static Material SodiumAmide;
    public static Material SodiumAmideAmmoniaSolution;
    public static Material SodiumTrifluoroacetateSolution;
    public static Material SodiumFluoride;
    public static Material Oleum;
    public static Material HydrogenChloride;
    public static Material ChlorosulfuricAcid;
    public static Material IronIISulfate;

    //Unknown Composition Materials//
    public static Material InfernalChelatingSap;
    public static Material ExtremophileChemolithotrophicSludge;
    public static Material BioChelatedNetheriteComplex;
    public static Material CrimsonSaccharifiedLiquid;
    public static Material ConcentratedCrimsonSlurry;
    public static Material ConcentratedCrimsonLysate;
    public static Material ActiveCytochromeCarrier;
    public static Material WarpedEPSFluid;
    public static Material ImpureNetheriteScrap;
    public static Material DeslaggedNetherite;
    public static Material PurifiedNetheriteConcentrate;
    public static Material BacterialMetabolicWastewater;
    public static Material OrganicImpurityWasteAcid;
    public static Material DenaturedProteinWastewater;
    public static Material ChitinousWasteAcid;
    public static Material FluorosilicateWastewater;
    public static Material CellularWasteSlurry;
    public static Material SulfurRichOrganicWastewater;
    public static Material BioleachingToxicWastewater;
    public static Material BiomineralizedOrganicWastewater;
    public static Material CrudeDilutedAceticAcid;
    public static Material DenaturedAqueousWaste;
    public static Material IndustrialWastewater;

    public static void init() {
        ModifyMaterials.init();
        ElementMaterials.init();
        AlloyMaterials.init();
        ChemistryMaterials.init();
        CommonMaterials.init();
        UnknownCompositionMaterials.init();
    }
}
