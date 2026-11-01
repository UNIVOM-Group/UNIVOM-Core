package com.mrlacar.univom.data.recipes.removal;

import com.mrlacar.univom.data.recipes.RecipeRemoval;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public class SuperconductorRemovals {
    private static final String[] REMOVALS = {
            //LV//
            "gtceu:extruder/extrude_manganese_phosphide_wire",
            "gtceu:wiremill/mill_manganese_phosphide_wire",
            "gtceu:wiremill/mill_manganese_phosphide_wire_2",
            "gtceu:wiremill/mill_manganese_phosphide_wire_4",
            "gtceu:wiremill/mill_manganese_phosphide_wire_8",
            "gtceu:wiremill/mill_manganese_phosphide_wire_16",
            //MV//
            "gtceu:extruder/extrude_magnesium_diboride_wire",
            "gtceu:wiremill/mill_magnesium_diboride_wire",
            "gtceu:wiremill/mill_magnesium_diboride_wire_2",
            "gtceu:wiremill/mill_magnesium_diboride_wire_4",
            "gtceu:wiremill/mill_magnesium_diboride_wire_8",
            "gtceu:wiremill/mill_magnesium_diboride_wire_16",
            //HV//
            "gtceu:extruder/extrude_mercury_barium_calcium_cuprate_wire",
            "gtceu:wiremill/mill_mercury_barium_calcium_cuprate_wire",
            "gtceu:wiremill/mill_mercury_barium_calcium_cuprate_wire_2",
            "gtceu:wiremill/mill_mercury_barium_calcium_cuprate_wire_4",
            "gtceu:wiremill/mill_mercury_barium_calcium_cuprate_wire_8",
            "gtceu:wiremill/mill_mercury_barium_calcium_cuprate_wire_16",
            //EV//
            "gtceu:extruder/extrude_uranium_triplatinum_wire",
            "gtceu:wiremill/mill_uranium_triplatinum_wire",
            "gtceu:wiremill/mill_uranium_triplatinum_wire_2",
            "gtceu:wiremill/mill_uranium_triplatinum_wire_4",
            "gtceu:wiremill/mill_uranium_triplatinum_wire_8",
            "gtceu:wiremill/mill_uranium_triplatinum_wire_16",
            //IV//
            "gtceu:extruder/extrude_samarium_iron_arsenic_oxide_wire",
            "gtceu:wiremill/mill_samarium_iron_arsenic_oxide_wire",
            "gtceu:wiremill/mill_samarium_iron_arsenic_oxide_wire_2",
            "gtceu:wiremill/mill_samarium_iron_arsenic_oxide_wire_4",
            "gtceu:wiremill/mill_samarium_iron_arsenic_oxide_wire_8",
            "gtceu:wiremill/mill_samarium_iron_arsenic_oxide_wire_16",
            //LuV//
            "gtceu:extruder/extrude_indium_tin_barium_titanium_cuprate_wire",
            "gtceu:wiremill/mill_indium_tin_barium_titanium_cuprate_wire",
            "gtceu:wiremill/mill_indium_tin_barium_titanium_cuprate_wire_2",
            "gtceu:wiremill/mill_indium_tin_barium_titanium_cuprate_wire_4",
            "gtceu:wiremill/mill_indium_tin_barium_titanium_cuprate_wire_8",
            "gtceu:wiremill/mill_indium_tin_barium_titanium_cuprate_wire_16",
            //ZPM//
            "gtceu:extruder/extrude_uranium_rhodium_dinaquadide_wire",
            "gtceu:wiremill/mill_uranium_rhodium_dinaquadide_wire",
            "gtceu:wiremill/mill_uranium_rhodium_dinaquadide_wire_2",
            "gtceu:wiremill/mill_uranium_rhodium_dinaquadide_wire_4",
            "gtceu:wiremill/mill_uranium_rhodium_dinaquadide_wire_8",
            "gtceu:wiremill/mill_uranium_rhodium_dinaquadide_wire_16",
            //UV//
            "gtceu:extruder/extrude_enriched_naquadah_trinium_europium_duranide_wire",
            "gtceu:wiremill/mill_enriched_naquadah_trinium_europium_duranide_wire",
            "gtceu:wiremill/mill_enriched_naquadah_trinium_europium_duranide_wire_2",
            "gtceu:wiremill/mill_enriched_naquadah_trinium_europium_duranide_wire_4",
            "gtceu:wiremill/mill_enriched_naquadah_trinium_europium_duranide_wire_8",
            "gtceu:wiremill/mill_enriched_naquadah_trinium_europium_duranide_wire_16",
            //UHV//
            "gtceu:extruder/extrude_ruthenium_trinium_americium_neutronate_wire",
            "gtceu:wiremill/mill_ruthenium_trinium_americium_neutronate_wire",
            "gtceu:wiremill/mill_ruthenium_trinium_americium_neutronate_wire_2",
            "gtceu:wiremill/mill_ruthenium_trinium_americium_neutronate_wire_4",
            "gtceu:wiremill/mill_ruthenium_trinium_americium_neutronate_wire_8",
            "gtceu:wiremill/mill_ruthenium_trinium_americium_neutronate_wire_16",
    };

    public static void init(Consumer<ResourceLocation> registry) {
        RecipeRemoval.remove(registry, REMOVALS);
    }
}
