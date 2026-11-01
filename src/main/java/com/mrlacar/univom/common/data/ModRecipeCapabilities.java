package com.mrlacar.univom.common.data;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.mrlacar.univom.api.capability.recipe.BeamRecipeCapability;
import com.mrlacar.univom.api.data.particle.BeamPacket;

public class ModRecipeCapabilities {
    public final static RecipeCapability<BeamPacket> BEAM = BeamRecipeCapability.CAP;


    public static void init() {
        GTRegistries.RECIPE_CAPABILITIES.register(BEAM.name, BEAM);
    }
}
