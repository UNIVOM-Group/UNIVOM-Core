package com.mrlacar.univom.data.recipes.misc;

import com.mrlacar.univom.api.capability.recipe.BeamRecipeCapability;
import com.mrlacar.univom.api.data.particle.BeamPacket;
import com.mrlacar.univom.common.data.ModParticles;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.mrlacar.univom.api.ModValues.*;
import static com.mrlacar.univom.common.data.ModMaterials.LanthanumHexaboride;
import static com.mrlacar.univom.common.data.ModRecipeTypes.RADIO_FREQUENCY_SOURCE_CHAMBER_RECIPES;

public class RadioFrequencySourceRecipes {
    public static void init(Consumer<FinishedRecipe> provider) {
        RADIO_FREQUENCY_SOURCE_CHAMBER_RECIPES.recipeBuilder("electron_from_tungsten_rod")
                .inputItems(rod, Tungsten)
                .output(BeamRecipeCapability.CAP, new BeamPacket(ModParticles.ELECTRON.name, ModParticles.ELECTRON.sourceMaxEnergy, 20))
                .addData(PARTICLE, ModParticles.ELECTRON.name)
                .addData(FLUX, 20)
                .addData(MAX_ENERGY, (int) ModParticles.ELECTRON.sourceMaxEnergy)
                .duration(20).EUt(VA[IV]).save(provider);

        RADIO_FREQUENCY_SOURCE_CHAMBER_RECIPES.recipeBuilder("electron_from_long_lanthanum_hexaboride_rod")
                .inputItems(rodLong, LanthanumHexaboride)
                .output(BeamRecipeCapability.CAP, new BeamPacket(ModParticles.ELECTRON.name, ModParticles.ELECTRON.sourceMaxEnergy, 60))
                .addData(PARTICLE, ModParticles.ELECTRON.name)
                .addData(FLUX, 60)
                .addData(MAX_ENERGY, (int) ModParticles.ELECTRON.sourceMaxEnergy)
                .duration(20).EUt(VA[IV]).save(provider);

        RADIO_FREQUENCY_SOURCE_CHAMBER_RECIPES.recipeBuilder("neutron_from_californium_dust")
                .inputItems(dust, Californium)
                .output(BeamRecipeCapability.CAP, new BeamPacket(ModParticles.NEUTRON.name, ModParticles.NEUTRON.sourceMaxEnergy, 10))
                .addData(PARTICLE, ModParticles.NEUTRON.name)
                .addData(FLUX, 10)
                .addData(MAX_ENERGY, (int) ModParticles.NEUTRON.sourceMaxEnergy)
                .duration(20).EUt(VA[EV]).save(provider);

        RADIO_FREQUENCY_SOURCE_CHAMBER_RECIPES.recipeBuilder("neutron_from_long_beryllium_rod")
                .inputItems(rodLong, Beryllium)
                .output(BeamRecipeCapability.CAP, new BeamPacket(ModParticles.NEUTRON.name, ModParticles.NEUTRON.sourceMaxEnergy, 10))
                .addData(PARTICLE, ModParticles.NEUTRON.name)
                .addData(FLUX, 10)
                .addData(MAX_ENERGY, (int) ModParticles.NEUTRON.sourceMaxEnergy)
                .duration(20).EUt(VA[LuV]).save(provider);

        RADIO_FREQUENCY_SOURCE_CHAMBER_RECIPES.recipeBuilder("proton_from_hydrogen")
                .inputFluids(Hydrogen.getFluid(1000))
                .output(BeamRecipeCapability.CAP, new BeamPacket(ModParticles.PROTON.name, ModParticles.PROTON.sourceMaxEnergy, 40))
                .addData(PARTICLE, ModParticles.PROTON.name)
                .addData(FLUX, 40)
                .addData(MAX_ENERGY, (int) ModParticles.PROTON.sourceMaxEnergy)
                .duration(20).EUt(VA[UV]).save(provider);
    }
}
