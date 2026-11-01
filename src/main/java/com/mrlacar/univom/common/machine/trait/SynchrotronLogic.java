package com.mrlacar.univom.common.machine.trait;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.mrlacar.univom.api.capability.IBeamProvider;
import com.mrlacar.univom.api.capability.recipe.BeamRecipeCapability;
import com.mrlacar.univom.api.data.particle.BeamPacket;
import com.mrlacar.univom.api.data.particle.ParticleDefinition;
import com.mrlacar.univom.api.machine.trait.NotifiableBeamContainer;
import com.mrlacar.univom.common.data.ModParticles;
import com.mrlacar.univom.common.machine.multiblock.electric.Synchrotron;
import com.mrlacar.univom.common.machine.multiblock.part.BeamHatchPartMachine;

import java.util.HashSet;
import java.util.Set;

public class SynchrotronLogic extends RecipeLogic {

    private final Set<IBeamProvider> scratchSeen = new HashSet<>();

    public SynchrotronLogic(Synchrotron machine) {
        super(machine);
    }

    @Override
    public void findAndHandleRecipe() {
        lastFailedMatches = null;

        recipeDirty = false;

        if (!(this.machine instanceof Synchrotron syncMachine)) return;

        if (isSuspend() || !isWorkingEnabled()) return;

        long targetEUt = syncMachine.getTargetEUt();
        int targetDuration = syncMachine.getTargetDuration();
        long targetFlux = syncMachine.getTargetFlux();

        if (targetEUt <= 0 || targetDuration <= 0 || targetFlux <= 0) return;

        NotifiableBeamContainer inputHatch = getBeamHatch(IO.IN);
        if (inputHatch == null) return;

        scratchSeen.clear();
        BeamPacket available = inputHatch.requestBeam(targetFlux, true, scratchSeen);

        if (available.isEmpty() || available.flux() < targetFlux) return;

        ParticleDefinition particle = ModParticles.get(available.particle());
        if (particle == null) return;

        double m0 = particle.restMass;
        double eta = 1.0;
        double cBase = 25500.0;
        double k = 1000.0;
        double alpha = 0.4;

        long totalEU = targetEUt * targetDuration;
        double deltaE = 0.0;
        double denominator = targetFlux * k;

        if (denominator > 0) {
            double innerValue = (totalEU * eta) / denominator;
            deltaE = cBase * m0 * Math.pow(Math.max(0.0, innerValue), alpha);
        }

        double finalEnergy = available.energy() + deltaE;

        GTRecipe dynamicRecipe = GTRecipeBuilder.ofRaw()
                .duration(targetDuration)
                .EUt(targetEUt)
                .inputFluids(GTMaterials.Helium.getFluid(FluidStorageKeys.LIQUID, Math.min(640000, 1600 * targetDuration)))
                .outputFluids(GTMaterials.Helium.getFluid(Math.min(640000, 1600 * targetDuration)))
                .input(BeamRecipeCapability.CAP, new BeamPacket(available.particle(), available.energy(), targetFlux))
                .output(BeamRecipeCapability.CAP, new BeamPacket(available.particle(), finalEnergy, targetFlux))
                .buildRawRecipe();

        var match = checkRecipe(dynamicRecipe);
        if (match.isSuccess()) {
            setupRecipe(dynamicRecipe);
        }
    }

    private NotifiableBeamContainer getBeamHatch(IO io) {
        for (IMultiPart part : ((Synchrotron) machine).getParts()) {
            if (part instanceof BeamHatchPartMachine beamHatch) {
                if ((io == IO.OUT && beamHatch.isTransmitter()) ||
                        (io == IO.IN && !beamHatch.isTransmitter())) {
                    return beamHatch.getBeamContainer();
                }
            }
        }
        return null;
    }
}
