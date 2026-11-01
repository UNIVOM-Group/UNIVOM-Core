package com.mrlacar.univom.common.machine.trait;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.mrlacar.univom.api.data.particle.ParticleDefinition;
import com.mrlacar.univom.common.data.ModParticles;
import com.mrlacar.univom.common.machine.multiblock.electric.BeamVacuumMagneticConfinementRing;
import com.mrlacar.univom.utils.AccumulatedBeamUtils;

import static com.gregtechceu.gtceu.common.data.GTMaterials.*;

public class BeamVacuumMagneticConfinementRingLogic extends RecipeLogic {
    public BeamVacuumMagneticConfinementRingLogic(BeamVacuumMagneticConfinementRing machine) {
        super(machine);
    }

    @Override
    public BeamVacuumMagneticConfinementRing getMachine() {
        return (BeamVacuumMagneticConfinementRing) super.getMachine();
    }

    @Override
    public void findAndHandleRecipe() {
        lastFailedMatches = null;
        if (!isWorkingEnabled()) return;

        BeamVacuumMagneticConfinementRing accMachine = getMachine();
        long baseLHeDemand = 2000;
        long extraCoolingDemand = 0;

        for (AccumulatedBeamUtils beam : accMachine.getStoredBeams().values()) {
            ParticleDefinition def = ModParticles.get(beam.getParticleName());
            if (def != null) {
                extraCoolingDemand += beam.calculateCoolingDemand(def.restMass);
            }
        }

        long totalLHeDemand = baseLHeDemand + extraCoolingDemand;

        GTRecipe dynamicRecipe = GTRecipeBuilder.ofRaw()
                .duration(20)
                .EUt(GTValues.VA[GTValues.UV])
                .inputFluids(Helium.getFluid(FluidStorageKeys.LIQUID,(int) totalLHeDemand))
                .outputFluids(Helium.getFluid((int) totalLHeDemand))
                .buildRawRecipe();

        var match = checkRecipe(dynamicRecipe);
        if (match.isSuccess()) {
            setupRecipe(dynamicRecipe);
        } else {
            recipeDirty = false;
        }
    }

    @Override
    public void onRecipeFinish() {
        super.onRecipeFinish();
        for (AccumulatedBeamUtils beam : getMachine().getStoredBeams().values()) {
            ParticleDefinition def = ModParticles.get(beam.getParticleName());
            if (def != null) {
                beam.applyCooling(def.restMass);
                getMachine().needsClientSync = true;
            }
        }
        getMachine().markDirty();
    }
}
