package com.mrlacar.univom.common.machine.trait;

import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.mrlacar.univom.api.capability.IBeamProvider;
import com.mrlacar.univom.api.capability.recipe.BeamRecipeCapability;
import com.mrlacar.univom.api.data.particle.BeamPacket;
import com.mrlacar.univom.api.data.particle.ParticleDefinition;
import com.mrlacar.univom.api.machine.multiblock.LargeHadronColliderModuleType;
import com.mrlacar.univom.api.machine.trait.NotifiableBeamContainer;
import com.mrlacar.univom.common.data.ModParticles;
import com.mrlacar.univom.common.machine.multiblock.electric.LargeHadronCollider;

import java.util.*;

import static com.gregtechceu.gtceu.api.GTValues.UV;
import static com.gregtechceu.gtceu.api.GTValues.VA;

public class LargeHadronColliderLogic extends RecipeLogic {
    private static final int LHC_DURATION = 1200;
    private static final Map<ParticleDefinition, LargeHadronColliderModuleType> TARGET_ROUTING = new HashMap<>();

    static {
        TARGET_ROUTING.put(ModParticles.PHOTON, LargeHadronColliderModuleType.CMS);
        TARGET_ROUTING.put(ModParticles.ELECTRON, LargeHadronColliderModuleType.CMS);
        TARGET_ROUTING.put(ModParticles.MUON, LargeHadronColliderModuleType.CMS);
        TARGET_ROUTING.put(ModParticles.TAU, LargeHadronColliderModuleType.CMS);

        TARGET_ROUTING.put(ModParticles.ELECTRON_NEUTRINO, LargeHadronColliderModuleType.ATLAS);
        TARGET_ROUTING.put(ModParticles.MUON_NEUTRINO, LargeHadronColliderModuleType.ATLAS);
        TARGET_ROUTING.put(ModParticles.TAU_NEUTRINO, LargeHadronColliderModuleType.ATLAS);
        TARGET_ROUTING.put(ModParticles.W_BOSON, LargeHadronColliderModuleType.ATLAS);
        TARGET_ROUTING.put(ModParticles.Z_BOSON, LargeHadronColliderModuleType.ATLAS);

        TARGET_ROUTING.put(ModParticles.PROTON, LargeHadronColliderModuleType.ALICE);
        TARGET_ROUTING.put(ModParticles.NEUTRON, LargeHadronColliderModuleType.ALICE);
        TARGET_ROUTING.put(ModParticles.ETA, LargeHadronColliderModuleType.ALICE);
        TARGET_ROUTING.put(ModParticles.LAMBDA, LargeHadronColliderModuleType.ALICE);
        TARGET_ROUTING.put(ModParticles.OMEGA, LargeHadronColliderModuleType.ALICE);
        TARGET_ROUTING.put(ModParticles.J_PSI, LargeHadronColliderModuleType.ALICE);
        TARGET_ROUTING.put(ModParticles.UPSILON, LargeHadronColliderModuleType.ALICE);

        TARGET_ROUTING.put(ModParticles.HIGGS_BOSON, LargeHadronColliderModuleType.LHCB);
        TARGET_ROUTING.put(ModParticles.GRAVITON, LargeHadronColliderModuleType.LHCB);
    }

    public LargeHadronColliderLogic(LargeHadronCollider machine) {
        super(machine);
    }

    @Override
    public LargeHadronCollider getMachine() {
        return (LargeHadronCollider) super.getMachine();
    }

    private final Set<IBeamProvider> scratchSeenA = new HashSet<>();
    private final Set<IBeamProvider> scratchSeenB = new HashSet<>();

    private double[] getEffectiveEnergies(double e1, double e2) {
        double t1 = getMachine().getTargetEnergy1();
        double t2 = getMachine().getTargetEnergy2();

        if (t1 > 0 && t2 <= 0) t2 = t1;
        if (t2 > 0 && t1 <= 0) t1 = t2;

        double eff1 = e1;
        double eff2 = e2;

        if (t1 > 0 && t2 > 0) {
            if (e1 >= t1 && e2 >= t2) {
                eff1 = t1; eff2 = t2;
            } else if (e1 >= t2 && e2 >= t1) {
                eff1 = t2; eff2 = t1;
            } else {
                return null;
            }
        }
        return new double[]{eff1, eff2};
    }

    @Override
    public void findAndHandleRecipe() {
        lastFailedMatches = null;

        recipeDirty = false;

        if (!getMachine().isRecipeLogicAvailable() || !isWorkingEnabled()) return;

        List<NotifiableBeamContainer> inputHatches = getMachine().getBeamInputHatches();
        if (inputHatches.size() < 2) return;

        NotifiableBeamContainer hatch1 = inputHatches.get(0);
        NotifiableBeamContainer hatch2 = inputHatches.get(1);

        if (hatch1.getTotalContentAmount() <= 0 || hatch2.getTotalContentAmount() <= 0) return;

        scratchSeenA.clear();
        scratchSeenB.clear();
        BeamPacket available1 = hatch1.requestBeam(Long.MAX_VALUE, true, scratchSeenA);
        BeamPacket available2 = hatch2.requestBeam(Long.MAX_VALUE, true, scratchSeenB);

        if (available1.isEmpty() || available1.flux() <= 0 || available2.isEmpty() || available2.flux() <= 0) return;

        double[] effEnergies = getEffectiveEnergies(available1.energy(), available2.energy());
        if (effEnergies == null) return;

        GTRecipe dynamicRecipe = GTRecipeBuilder.ofRaw()
                .duration(LHC_DURATION)
                .EUt(VA[UV])
                .input(BeamRecipeCapability.CAP, new BeamPacket(available1.particle(), available1.energy(), available1.flux()))
                .input(BeamRecipeCapability.CAP, new BeamPacket(available2.particle(), available2.energy(), available2.flux()))
                .buildRawRecipe();

        var match = checkRecipe(dynamicRecipe);
        if (match.isSuccess()) {
            setupRecipe(dynamicRecipe);
        }
    }

    private BeamPacket[] parseInputs(GTRecipe recipe) {
        List<Content> beamInputs = recipe.getInputContents(BeamRecipeCapability.CAP);
        if (beamInputs == null || beamInputs.isEmpty()) return null;

        if (beamInputs.size() == 1) {
            BeamPacket merged = (BeamPacket) beamInputs.get(0).getContent();
            long halfFlux = merged.flux() / 2;
            return new BeamPacket[]{
                    new BeamPacket(merged.particle(), merged.energy(), halfFlux),
                    new BeamPacket(merged.particle(), merged.energy(), merged.flux() - halfFlux)
            };
        } else if (beamInputs.size() >= 2) {
            return new BeamPacket[]{
                    (BeamPacket) beamInputs.get(0).getContent(),
                    (BeamPacket) beamInputs.get(1).getContent()
            };
        }
        return null;
    }

    @Override
    public void onRecipeFinish() {
        GTRecipe completedRecipe = this.lastRecipe;
        super.onRecipeFinish();

        if (completedRecipe != null) {
            BeamPacket[] beams = parseInputs(completedRecipe);
            if (beams != null && beams.length == 2) {
                calculateAndDispatchGacha(beams[0], beams[1]);
            }
        }
    }

    private void calculateAndDispatchGacha(BeamPacket p1, BeamPacket p2) {
        LargeHadronCollider lhc = getMachine();
        ParticleDefinition def1 = ModParticles.get(p1.particle());
        ParticleDefinition def2 = ModParticles.get(p2.particle());
        if (def1 == null || def2 == null) return;

        double e1 = p1.energy();
        double e2 = p2.energy();
        double[] effEnergies = getEffectiveEnergies(e1, e2);
        if (effEnergies == null) effEnergies = new double[]{e1, e2};

        double eff1 = effEnergies[0];
        double eff2 = effEnergies[1];
        LargeHadronColliderModuleType photonModule = getTargetModuleForParticle(ModParticles.PHOTON);

        double deltaE1 = e1 - eff1;
        double deltaE2 = e2 - eff2;
        if (deltaE1 > 0) lhc.dispatchCollisionResult(photonModule, new BeamPacket(ModParticles.PHOTON.name, deltaE1, p1.flux()));
        if (deltaE2 > 0) lhc.dispatchCollisionResult(photonModule, new BeamPacket(ModParticles.PHOTON.name, deltaE2, p2.flux()));

        double collisionEnergy = 2.0 * Math.sqrt(eff1 * eff2);
        long f1 = p1.flux();
        long f2 = p2.flux();
        long effectiveFlux = Math.min(f1, f2) * 2L;
        long wastedFlux = (f1 + f2) - effectiveFlux;

        if (wastedFlux > 0) {
            lhc.dispatchCollisionResult(photonModule, new BeamPacket(ModParticles.PHOTON.name, collisionEnergy * 0.1, wastedFlux));
        }

        boolean isStrong1 = def1.forceType.equals("STRONG");
        boolean isStrong2 = def2.forceType.equals("STRONG");
        boolean isEM1 = def1.forceType.equals("EM");
        boolean isEM2 = def2.forceType.equals("EM");

        boolean isDirty = isStrong1 && isStrong2;
        boolean isPure = isEM1 && isEM2;
        boolean isDIS = (isStrong1 && isEM2) || (isEM1 && isStrong2);

        if (!isDirty && !isPure && !isDIS) {
            lhc.dispatchCollisionResult(photonModule, new BeamPacket(ModParticles.PHOTON.name, collisionEnergy * 0.1, effectiveFlux));
            return;
        }

        double totalWeight = 0.0;
        double maxWeight = -1.0;
        ParticleDefinition maxWeightParticle = null;
        Map<ParticleDefinition, Double> particleWeights = new HashMap<>();

        for (ParticleDefinition p : ModParticles.getAllParticles()) {
            if (p.lhcWeight <= 0) continue;
            if (isPure && p.forceType.equals("STRONG")) continue;
            if (isDIS && (p == ModParticles.HIGGS_BOSON || p == ModParticles.GRAVITON || p == ModParticles.W_BOSON || p == ModParticles.Z_BOSON)) continue;

            double effectiveMass = p.restMass;
            if (p.name.equals("photon")) effectiveMass = 0.0;
            else if (p.name.contains("neutrino")) effectiveMass = 10.0;
            else if (p.name.equals("graviton")) effectiveMass = 5000000.0;

            if (collisionEnergy >= effectiveMass) {
                double massFactor = Math.max(1.0, effectiveMass);
                double overEnergy = collisionEnergy / massFactor;
                double scaling = Math.sqrt(massFactor) * Math.log10(1.0 + overEnergy);
                double dynWeight = p.lhcWeight * (1.0 + scaling);

                particleWeights.put(p, dynWeight);
                totalWeight += dynWeight;

                if (dynWeight > maxWeight) {
                    maxWeight = dynWeight;
                    maxWeightParticle = p;
                }
            }
        }

        if (particleWeights.isEmpty() || maxWeightParticle == null || effectiveFlux <= 0) return;

        long distributedFlux = 0;
        for (Map.Entry<ParticleDefinition, Double> entry : particleWeights.entrySet()) {
            ParticleDefinition p = entry.getKey();
            if (p == maxWeightParticle) continue;

            long fluxForThis = (long) (effectiveFlux * (entry.getValue() / totalWeight));
            distributedFlux += fluxForThis;

            if (fluxForThis > 0) {
                LargeHadronColliderModuleType target = getTargetModuleForParticle(p);
                double outputEnergy = p.restMass > 0 ? p.restMass : (collisionEnergy * 0.1);
                lhc.dispatchCollisionResult(target, new BeamPacket(p.name, outputEnergy, fluxForThis));

                double leftoverEnergy = collisionEnergy - outputEnergy;
                if (leftoverEnergy > 0) {
                    lhc.dispatchCollisionResult(photonModule, new BeamPacket(ModParticles.PHOTON.name, leftoverEnergy, fluxForThis));
                }
            }
        }

        long remainder = effectiveFlux - distributedFlux;
        if (remainder > 0) {
            LargeHadronColliderModuleType target = getTargetModuleForParticle(maxWeightParticle);
            double outputEnergy = maxWeightParticle.restMass > 0 ? maxWeightParticle.restMass : (collisionEnergy * 0.1);
            lhc.dispatchCollisionResult(target, new BeamPacket(maxWeightParticle.name, outputEnergy, remainder));

            double leftoverEnergy = collisionEnergy - outputEnergy;
            if (leftoverEnergy > 0) {
                lhc.dispatchCollisionResult(photonModule, new BeamPacket(ModParticles.PHOTON.name, leftoverEnergy, remainder));
            }
        }
    }

    private LargeHadronColliderModuleType getTargetModuleForParticle(ParticleDefinition particle) {
        return TARGET_ROUTING.getOrDefault(particle, LargeHadronColliderModuleType.ALICE);
    }
}
