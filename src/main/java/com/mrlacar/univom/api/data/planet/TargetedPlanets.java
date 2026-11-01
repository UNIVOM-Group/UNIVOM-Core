package com.mrlacar.univom.api.data.planet;

import org.jetbrains.annotations.NotNull;

public enum TargetedPlanets {
    MERCURY("mercury", "solar_system", "0.3871"),
    VENUS("venus", "solar_system", "0.7233"),
    MOON("moon", "solar_system", "1.0000"),
    MARS("mars", "solar_system", "1.5237"),

    GLACIO("glacio", "proxima_centauri", "0.1235");

    public final String planetId;
    public final String systemId;
    public final String auCoordinate;

    TargetedPlanets(String planetId, String systemId, String auCoordinate) {
        this.planetId = planetId;
        this.systemId = systemId;
        this.auCoordinate = auCoordinate;
    }

    @NotNull
    public static String getPlanetBySystemAndAU(String currentSystem, String inputAU) {
        if (currentSystem == null || currentSystem.isEmpty() || inputAU == null) return "";

        for (TargetedPlanets planet : values()) {
            if (planet.systemId.equals(currentSystem) && planet.auCoordinate.equals(inputAU)) {
                return planet.planetId;
            }
        }
        return "";
    }
}
