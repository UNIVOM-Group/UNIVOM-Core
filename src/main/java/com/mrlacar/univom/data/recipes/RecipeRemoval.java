package com.mrlacar.univom.data.recipes;

import com.mrlacar.univom.data.recipes.removal.SuperconductorRemovals;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;
import java.util.stream.Stream;

public class RecipeRemoval {
    public static void init(Consumer<ResourceLocation> registry) {
        SuperconductorRemovals.init(registry);
    }

    public static void remove(Consumer<ResourceLocation> registry, String... paths) {
        Stream.of(paths).map(ResourceLocation::parse).forEach(registry);
    }
}
