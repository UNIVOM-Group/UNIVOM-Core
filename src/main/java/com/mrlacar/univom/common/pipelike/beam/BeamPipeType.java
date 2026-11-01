package com.mrlacar.univom.common.pipelike.beam;

import com.gregtechceu.gtceu.api.pipenet.IPipeType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum BeamPipeType implements IPipeType<BeamPipeProperties>, StringRepresentable {
    NORMAL;

    public static final ResourceLocation TYPE = new ResourceLocation("univom", "beam");

    @Override
    public float getThickness() {
        return 0.375F;
    }

    @Override
    public BeamPipeProperties modifyProperties(BeamPipeProperties baseProperties) {
        return baseProperties;
    }

    @Override
    public boolean isPaintable() {
        return true;
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
