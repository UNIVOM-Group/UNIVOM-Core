package com.mrlacar.univom.api.data.particle;

import net.minecraft.network.chat.Component;

import java.util.List;

public class ParticleDefinition {
    public final String name;
    public final String symbol;
    public final double restMass;
    public final double charge;

    public final String forceType;
    public final int lhcWeight;

    public final boolean isSourceParticle;
    public final double sourceMaxEnergy;
    public final int sourceBaseFlux;

    public ParticleDefinition(String name, String symbol, double restMass, double charge, String forceType, int lhcWeight, boolean isSourceParticle, double sourceMaxEnergy, int sourceBaseFlux) {
        this.name = name;
        this.symbol = symbol;
        this.restMass = restMass;
        this.charge = charge;
        this.forceType = forceType;
        this.lhcWeight = lhcWeight;
        this.isSourceParticle = isSourceParticle;
        this.sourceMaxEnergy = sourceMaxEnergy;
        this.sourceBaseFlux = sourceBaseFlux;
    }

    public void addInformation(List<Component> tooltip) {
        tooltip.add(Component.translatable("univom.tooltip.particle.symbol").append(this.symbol));
        tooltip.add(Component.translatable("univom.tooltip.particle.rest_mass").append(String.format("%.3f MeV", this.restMass)));
    }
}
