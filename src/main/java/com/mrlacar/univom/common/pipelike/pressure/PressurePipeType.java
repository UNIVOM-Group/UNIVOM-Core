package com.mrlacar.univom.common.pipelike.pressure;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.pipenet.IPipeType;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.mrlacar.univom.UNIVOM;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum PressurePipeType implements IPipeType<PressurePipeProperties>, StringRepresentable {

    HV_PIPE("hv_pressure_pipe", GTValues.HV, 50, 2, Kind.PIPE),
    EV_PIPE("ev_pressure_pipe", GTValues.EV, 150, 4, Kind.PIPE),
    IV_PIPE("iv_pressure_pipe", GTValues.IV, 300, 8, Kind.PIPE),
    HV_TANK("hv_pressure_tank", GTValues.HV, 50, 500, Kind.TANK),
    EV_TANK("ev_pressure_tank", GTValues.EV, 150, 500, Kind.TANK),
    IV_TANK("iv_pressure_tank", GTValues.IV, 300, 500, Kind.TANK),
    VENT_VALVE("vent_valve", GTValues.IV, 300, 1, Kind.VENT_VALVE),
    SAFETY_VALVE("safety_valve", GTValues.IV, 300, 1, Kind.SAFETY_VALVE);

    public static final ResourceLocation TYPE = UNIVOM.id("pressure");
    public static final int DEFAULT_SAFETY_PERCENT = 90;
    private static final float PIPE_THICKNESS = 0.375F;

    private final String name;
    private final int tier;
    private final Kind kind;
    private final PressurePipeProperties properties;

    PressurePipeType(String name, int tier, int ratingBar, int volume, Kind kind) {
        this.name = name;
        this.tier = tier;
        this.kind = kind;
        this.properties = new PressurePipeProperties(ratingBar, volume,
                kind == Kind.SAFETY_VALVE ? DEFAULT_SAFETY_PERCENT : 0);
    }

    public PressurePipeProperties getProperties() {
        return properties;
    }

    public int getTier() {
        return tier;
    }

    public boolean isPipe() {
        return kind == Kind.PIPE;
    }

    public boolean isTank() {
        return kind == Kind.TANK;
    }

    public boolean isVentValve() {
        return kind == Kind.VENT_VALVE;
    }

    public boolean isSafetyValve() {
        return kind == Kind.SAFETY_VALVE;
    }

    public Material getMaterial() {
        return switch (tier) {
            case GTValues.EV -> GTMaterials.StainlessSteel;
            case GTValues.IV -> GTMaterials.Titanium;
            default -> GTMaterials.Steel;
        };
    }

    public float getBurstFactor() {
        return kind == Kind.TANK ? 1.3F : 1.0F;
    }

    @Override
    public float getThickness() {
        return kind == Kind.PIPE ? PIPE_THICKNESS : 1.0F;
    }

    @Override
    public PressurePipeProperties modifyProperties(PressurePipeProperties baseProperties) {
        return baseProperties;
    }

    @Override
    public boolean isPaintable() {
        return false;
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }

    private enum Kind {
        PIPE,
        TANK,
        VENT_VALVE,
        SAFETY_VALVE
    }
}
