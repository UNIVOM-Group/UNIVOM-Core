package com.mrlacar.univom.api.chemistry;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.Element;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class ChemSymbols {

    private static volatile Map<String, Material> symbolToMaterial;

    private ChemSymbols() {}

    @Nullable
    public static Material materialFor(String symbol) {
        Map<String, Material> index = symbolToMaterial;
        return (index == null ? buildIndex() : index).get(symbol);
    }

    public static void invalidate() {
        symbolToMaterial = null;
    }

    private static synchronized Map<String, Material> buildIndex() {
        Map<String, Material> existing = symbolToMaterial;
        if (existing != null) {
            return existing;
        }
        Map<String, Material> forward = new Object2ObjectOpenHashMap<>(256);
        for (Material material : GTCEuAPI.materialManager.getRegisteredMaterials()) {
            if (!material.isElement()) {
                continue;
            }
            Element element = material.getElement();
            if (element == null) {
                continue;
            }
            String symbol = element.symbol();
            if (symbol == null || symbol.isEmpty()) {
                continue;
            }
            forward.putIfAbsent(symbol, material);
        }
        symbolToMaterial = forward;
        return forward;
    }
}
