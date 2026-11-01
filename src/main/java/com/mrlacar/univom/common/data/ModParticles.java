package com.mrlacar.univom.common.data;

import com.mrlacar.univom.api.data.particle.ParticleDefinition;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModParticles {
    public static final Map<String, ParticleDefinition> REGISTRY = new LinkedHashMap<>();

    public static final ParticleDefinition ELECTRON = createSourceAndCollider(
            "electron", "e⁻", 0.511, -1.0,
            "EM", 800, 5.0, 60);

    public static final ParticleDefinition MUON = createColliderOnly(
            "muon", "μ⁻", 105.66, -1.0,
            "EM", 300);

    public static final ParticleDefinition TAU = createColliderOnly(
            "tau", "τ⁻", 1776.89, -1.0,
            "EM", 100);

    public static final ParticleDefinition ELECTRON_NEUTRINO = createColliderOnly(
            "electron_neutrino", "νe", 0.0, 0.0,
            "WEAK", 250);

    public static final ParticleDefinition MUON_NEUTRINO = createColliderOnly(
            "muon_neutrino", "νμ", 0.0, 0.0,
            "WEAK", 200);

    public static final ParticleDefinition TAU_NEUTRINO = createColliderOnly(
            "tau_neutrino", "ντ", 0.0, 0.0,
            "WEAK", 150);

    public static final ParticleDefinition PROTON = createSourceAndCollider(
            "proton", "p⁺", 938.27, 1.0,
            "STRONG", 600, 1000.0, 40);

    public static final ParticleDefinition NEUTRON = createSourceAndCollider(
            "neutron", "n⁰", 939.57, 0.0,
            "STRONG", 500, 15.0, 10);

    public static final ParticleDefinition ALPHA = createSourceOnly(
            "alpha", "α", 3727.38, 2.0,
            8.0, 1);

    public static final ParticleDefinition LAMBDA = createColliderOnly(
            "lambda", "Λ", 1115.68, 0.0,
            "STRONG", 80);

    public static final ParticleDefinition OMEGA = createColliderOnly(
            "omega", "Ω⁻", 1672.45, -1.0,
            "STRONG", 60);

    public static final ParticleDefinition ETA = createColliderOnly(
            "eta", "η", 547.86, 0.0,
            "STRONG", 400);

    public static final ParticleDefinition J_PSI = createColliderOnly(
            "j_psi", "J/ψ", 3096.9, 0.0,
            "STRONG", 50);

    public static final ParticleDefinition UPSILON = createColliderOnly(
            "upsilon", "Υ", 9460.3, 0.0,
            "STRONG", 40);

    public static final ParticleDefinition PHOTON = createColliderOnly(
            "photon", "γ", 0.0, 0.0,
            "EM", 1000);

    public static final ParticleDefinition W_BOSON = createColliderOnly(
            "w_boson", "W±", 80369.2, 1.0,
            "WEAK", 110);

    public static final ParticleDefinition Z_BOSON = createColliderOnly(
            "z_boson", "Z⁰", 91187.6, 0.0,
            "WEAK", 110);

    public static final ParticleDefinition HIGGS_BOSON = createColliderOnly(
            "higgs_boson", "H⁰", 125110.0, 0.0,
            "GRAVITY", 15);

    public static final ParticleDefinition GRAVITON = createColliderOnly(
            "graviton", "G", 0.0, 0.0,
            "GRAVITY", 50);

    private static ParticleDefinition createSourceAndCollider(String name, String symbol, double restMass, double charge, String forceType, int lhcWeight, double sourceMaxE, int sourceFlux) {
        ParticleDefinition particle = new ParticleDefinition(name, symbol, restMass, charge, forceType, lhcWeight, true, sourceMaxE, sourceFlux);
        REGISTRY.put(name, particle);
        return particle;
    }

    private static ParticleDefinition createColliderOnly(String name, String symbol, double restMass, double charge, String forceType, int lhcWeight) {
        ParticleDefinition particle = new ParticleDefinition(name, symbol, restMass, charge, forceType, lhcWeight, false, 0.0, 0);
        REGISTRY.put(name, particle);
        return particle;
    }

    private static ParticleDefinition createSourceOnly(String name, String symbol, double restMass, double charge, double sourceMaxE, int sourceFlux) {
        ParticleDefinition particle = new ParticleDefinition(name, symbol, restMass, charge, "NONE", 0, true, sourceMaxE, sourceFlux);
        REGISTRY.put(name, particle);
        return particle;
    }

    public static ParticleDefinition get(String name) {
        return REGISTRY.get(name);
    }

    public static Collection<ParticleDefinition> getAllParticles() {
        return REGISTRY.values();
    }
}
