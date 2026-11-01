package com.mrlacar.univom.client.tooltip;

import com.mrlacar.univom.config.ConfigHolder;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public record AlloySlices(Material[] materials, double[] fractions) {

    private static final int MAX_DEPTH = 8;
    private static final Reference2ObjectMap<Material, AlloySlices> CACHE = new Reference2ObjectOpenHashMap<>(64);

    private static boolean cachedRecursive;
    private static boolean cachedByMass;
    private static boolean cacheValid;

    public static void invalidate() {
        CACHE.clear();
        cacheValid = false;
    }

    @Nullable
    public static AlloySlices of(Material material) {
        ConfigHolder.MoleculeTooltipConfigs config = ConfigHolder.INSTANCE.moleculeTooltipSetting;
        if (!cacheValid || cachedRecursive != config.alloyRecursive || cachedByMass != config.alloyByMass) {
            CACHE.clear();
            cachedRecursive = config.alloyRecursive;
            cachedByMass = config.alloyByMass;
            cacheValid = true;
        }
        if (CACHE.containsKey(material)) {
            return CACHE.get(material);
        }
        AlloySlices slices = derive(material, cachedRecursive, cachedByMass);
        CACHE.put(material, slices);
        return slices;
    }

    @Nullable
    private static AlloySlices derive(Material material, boolean recursive, boolean byMass) {
        ImmutableList<MaterialStack> components = material.getMaterialComponents();
        if (components == null || components.size() < 2) {
            return null;
        }

        Reference2DoubleMap<Material> weights = new Reference2DoubleOpenHashMap<>(16);
        long parts = 0;
        for (MaterialStack stack : components) {
            parts += stack.amount();
        }
        if (parts <= 0) {
            return null;
        }
        for (MaterialStack stack : components) {
            double share = (double) stack.amount() / parts;
            if (recursive) {
                accumulate(stack.material(), share, weights, 0);
            } else {
                weights.mergeDouble(stack.material(), share, Double::sum);
            }
        }
        if (weights.isEmpty()) {
            return null;
        }

        List<Slice> slices = new ArrayList<>(weights.size());
        double total = 0.0;
        for (Reference2DoubleMap.Entry<Material> entry : weights.reference2DoubleEntrySet()) {
            Material component = entry.getKey();
            double weight = entry.getDoubleValue();
            if (byMass) {
                weight *= Math.max(1L, component.getMass());
            }
            slices.add(new Slice(component, weight));
            total += weight;
        }
        if (total <= 0.0) {
            return null;
        }

        slices.sort(Comparator.comparingDouble((Slice slice) -> -slice.weight())
                .thenComparing(slice -> slice.material().getName()));

        Material[] materials = new Material[slices.size()];
        double[] fractions = new double[slices.size()];
        for (int i = 0; i < slices.size(); i++) {
            materials[i] = slices.get(i).material();
            fractions[i] = slices.get(i).weight() / total;
        }
        return new AlloySlices(materials, fractions);
    }

    private record Slice(Material material, double weight) {}

    private static void accumulate(Material material, double share, Reference2DoubleMap<Material> out, int depth) {
        ImmutableList<MaterialStack> components = material.getMaterialComponents();
        if (depth >= MAX_DEPTH || components == null || components.isEmpty()) {
            out.mergeDouble(material, share, Double::sum);
            return;
        }
        long parts = 0;
        for (MaterialStack stack : components) {
            parts += stack.amount();
        }
        if (parts <= 0) {
            out.mergeDouble(material, share, Double::sum);
            return;
        }
        for (MaterialStack stack : components) {
            accumulate(stack.material(), share * stack.amount() / parts, out, depth + 1);
        }
    }
}
