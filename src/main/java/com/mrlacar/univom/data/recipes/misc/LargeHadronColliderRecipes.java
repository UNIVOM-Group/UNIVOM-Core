package com.mrlacar.univom.data.recipes.misc;

import com.mrlacar.univom.common.data.ModItems;
import com.mrlacar.univom.common.data.ModParticles;
import com.mrlacar.univom.common.data.machines.ModMultiMachines;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.mrlacar.univom.common.data.ModRecipeTypes.LARGE_HADRON_COLLIDER_RECIPES;

public class LargeHadronColliderRecipes {
    public static void init(Consumer<FinishedRecipe> provider) {
        LARGE_HADRON_COLLIDER_RECIPES.recipeBuilder("cms")
                .inputItems(ModMultiMachines.CMS_MODULE)
                .outputItems(ModItems.PARTICLE_MODELS.get(ModParticles.PHOTON.name))
                .outputItems(ModItems.PARTICLE_MODELS.get(ModParticles.ELECTRON.name))
                .outputItems(ModItems.PARTICLE_MODELS.get(ModParticles.MUON.name))
                .outputItems(ModItems.PARTICLE_MODELS.get(ModParticles.TAU.name))
                .duration(1200).EUt(VA[UV]).save(provider);

        LARGE_HADRON_COLLIDER_RECIPES.recipeBuilder("atlas")
                .inputItems(ModMultiMachines.ATLAS_MODULE)
                .outputItems(ModItems.PARTICLE_MODELS.get(ModParticles.ELECTRON_NEUTRINO.name))
                .outputItems(ModItems.PARTICLE_MODELS.get(ModParticles.MUON_NEUTRINO.name))
                .outputItems(ModItems.PARTICLE_MODELS.get(ModParticles.TAU_NEUTRINO.name))
                .outputItems(ModItems.PARTICLE_MODELS.get(ModParticles.W_BOSON.name))
                .outputItems(ModItems.PARTICLE_MODELS.get(ModParticles.Z_BOSON.name))
                .duration(1200).EUt(VA[UV]).save(provider);

        LARGE_HADRON_COLLIDER_RECIPES.recipeBuilder("alice")
                .inputItems(ModMultiMachines.ALICE_MODULE)
                .outputItems(ModItems.PARTICLE_MODELS.get(ModParticles.PROTON.name))
                .outputItems(ModItems.PARTICLE_MODELS.get(ModParticles.NEUTRON.name))
                .outputItems(ModItems.PARTICLE_MODELS.get(ModParticles.ETA.name))
                .outputItems(ModItems.PARTICLE_MODELS.get(ModParticles.LAMBDA.name))
                .outputItems(ModItems.PARTICLE_MODELS.get(ModParticles.OMEGA.name))
                .outputItems(ModItems.PARTICLE_MODELS.get(ModParticles.J_PSI.name))
                .outputItems(ModItems.PARTICLE_MODELS.get(ModParticles.UPSILON.name))
                .duration(1200).EUt(VA[UV]).save(provider);

        LARGE_HADRON_COLLIDER_RECIPES.recipeBuilder("lhcb")
                .inputItems(ModMultiMachines.LHCB_MODULE)
                .outputItems(ModItems.PARTICLE_MODELS.get(ModParticles.HIGGS_BOSON.name))
                .outputItems(ModItems.PARTICLE_MODELS.get(ModParticles.GRAVITON.name))
                .duration(1200).EUt(VA[UV]).save(provider);
    }
}
