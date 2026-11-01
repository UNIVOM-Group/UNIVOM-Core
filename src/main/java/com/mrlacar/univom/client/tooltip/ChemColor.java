package com.mrlacar.univom.client.tooltip;

import com.mrlacar.univom.api.chemistry.ChemSymbols;
import com.mrlacar.univom.api.chemistry.MoleculeSymbol;
import com.mrlacar.univom.config.ConfigHolder;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class ChemColor {

    private static final int FALLBACK = 0xFFFFFF55;

    private static String cachedDefaultSource = "";
    private static int cachedDefault = FALLBACK;

    private ChemColor() {}

    public static void invalidate() {
        cachedDefaultSource = "";
    }

    public static int defaultColor() {
        String source = ConfigHolder.INSTANCE.moleculeTooltipSetting.defaultColor;
        if (source == null || source.isEmpty()) {
            return FALLBACK;
        }
        if (!source.equals(cachedDefaultSource)) {
            cachedDefaultSource = source;
            cachedDefault = parseHex(source);
        }
        return cachedDefault;
    }

    public static int forSymbol(MoleculeSymbol symbol) {
        if (symbol.hasExplicitColor()) {
            return symbol.explicitColor();
        }
        return forMaterial(symbol.material());
    }

    public static int forMaterial(@Nullable Material material) {
        return material == null ? defaultColor() : material.getMaterialARGB() | 0xFF000000;
    }

    public static Component colorizeFormula(String formula) {
        MutableComponent result = Component.empty();
        int fallback = defaultColor();
        int index = 0;
        int length = formula.length();

        while (index < length) {
            char current = formula.charAt(index);
            if (current >= 'A' && current <= 'Z') {
                int end = index + 1;
                if (end < length && formula.charAt(end) >= 'a' && formula.charAt(end) <= 'z') {
                    end++;
                }
                int color = symbolColor(formula.substring(index, end), fallback);
                if (color == fallback && end - index == 2) {
                    int single = symbolColor(formula.substring(index, index + 1), fallback);
                    if (single != fallback) {
                        end = index + 1;
                        color = single;
                    }
                }
                while (end < length && isTrailing(formula.charAt(end))) {
                    end++;
                }
                result.append(Component.literal(formula.substring(index, end))
                        .withStyle(Style.EMPTY.withColor(color)));
                index = end;
            } else {
                int end = index + 1;
                while (end < length && !(formula.charAt(end) >= 'A' && formula.charAt(end) <= 'Z')) {
                    end++;
                }
                result.append(Component.literal(formula.substring(index, end))
                        .withStyle(Style.EMPTY.withColor(fallback)));
                index = end;
            }
        }
        return result;
    }

    private static boolean isTrailing(char value) {
        if (value >= 'A' && value <= 'Z') {
            return false;
        }
        return value != '(' && value != ')' && value != '[' && value != ']' && value != '{' && value != '}';
    }

    private static int symbolColor(String symbol, int fallback) {
        Material material = ChemSymbols.materialFor(symbol);
        return material == null ? fallback : forMaterial(material);
    }

    private static int parseHex(String source) {
        String text = source.startsWith("#") ? source.substring(1) : source;
        if (text.isEmpty()) {
            return FALLBACK;
        }
        try {
            return 0xFF000000 | (int) Long.parseLong(text, 16);
        } catch (NumberFormatException e) {
            return FALLBACK;
        }
    }
}
