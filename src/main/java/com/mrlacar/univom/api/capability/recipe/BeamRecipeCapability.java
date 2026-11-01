package com.mrlacar.univom.api.capability.recipe;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.mrlacar.univom.api.data.particle.BeamPacket;
import com.mrlacar.univom.api.recipe.content.SerializerBeamPacket;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BeamRecipeCapability extends RecipeCapability<BeamPacket> {
    public final static BeamRecipeCapability CAP = new BeamRecipeCapability();

    protected BeamRecipeCapability() {
        super("beam", 0xFFEEEE00, false, 3, SerializerBeamPacket.INSTANCE);
    }

    @Override
    public BeamPacket copyInner(BeamPacket content) {
        return content;
    }

    @Override
    public BeamPacket copyWithModifier(BeamPacket content, ContentModifier modifier) {
        long modifiedFlux = ((Number) modifier.apply(content.flux())).longValue();
        return new BeamPacket(content.particle(), content.energy(), modifiedFlux);
    }

    @Override
    public int getMaxParallelByInput(IRecipeCapabilityHolder holder, GTRecipe recipe, int limit, boolean tick) {
        List<Content> inputs = (tick ? recipe.tickInputs : recipe.inputs).get(this);
        if (inputs == null || inputs.isEmpty()) {
            return limit;
        }
        if (!holder.hasCapabilityProxies()) {
            return 0;
        }
        List<IRecipeHandler<?>> handlers = holder.getCapabilitiesFlat(IO.IN, this);
        if (handlers.isEmpty()) {
            return 0;
        }

        int parallel = limit;
        for (int i = 0; i < inputs.size(); i++) {
            String particle = particleOf(inputs.get(i));
            if (particle == null || seenBefore(inputs, particle, i)) {
                continue;
            }
            long required = 0L;
            for (int j = i; j < inputs.size(); j++) {
                if (particle.equals(particleOf(inputs.get(j)))) {
                    required += fluxOf(inputs.get(j));
                }
            }
            if (required <= 0L) {
                continue;
            }
            long available = 0L;
            for (IRecipeHandler<?> handler : handlers) {
                for (Object content : handler.getContents()) {
                    if (content instanceof BeamPacket packet && particle.equals(packet.particle())) {
                        available += packet.flux();
                    }
                }
            }
            if (available < required) {
                return 0;
            }
            parallel = (int) Math.min(parallel, available / required);
            if (parallel <= 1) {
                return parallel;
            }
        }
        return parallel;
    }

    private static boolean seenBefore(List<Content> inputs, String particle, int index) {
        for (int i = 0; i < index; i++) {
            if (particle.equals(particleOf(inputs.get(i)))) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static String particleOf(Content content) {
        return content.content instanceof BeamPacket packet ? packet.particle() : null;
    }

    private static long fluxOf(Content content) {
        return content.content instanceof BeamPacket packet ? packet.flux() : 0L;
    }
}
