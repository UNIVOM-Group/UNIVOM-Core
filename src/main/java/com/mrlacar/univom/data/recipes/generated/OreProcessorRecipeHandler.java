package com.mrlacar.univom.data.recipes.generated;

import com.mrlacar.univom.common.data.ModRecipeTypes;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.OreProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;

import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.IV;
import static com.gregtechceu.gtceu.api.GTValues.VA;

public final class OreProcessorRecipeHandler {

    public enum Route {

        MACERATE_WASH_THERMAL_MACERATE(0, 600, 2),
        MACERATE_WASH_MACERATE_CENTRIFUGE(1, 300, 2),
        MACERATE_MACERATE_CENTRIFUGE(2, 200, 2),
        MACERATE_WASH_SIFT(3, 400, 2),
        MACERATE_BATH_MACERATE_CENTRIFUGE(4, 340, 2),
        MACERATE_BATH_THERMAL_MACERATE(5, 640, 2),
        HAMMER_HAMMER_SIMPLE_WASH(6, 20, 1);

        private static final Route[] VALUES = values();

        public final int circuit;
        public final int duration;
        public final int yieldMultiplier;

        Route(int circuit, int duration, int yieldMultiplier) {
            this.circuit = circuit;
            this.duration = duration;
            this.yieldMultiplier = yieldMultiplier;
        }
    }

    private static final int CHANCE_MACERATE = 1400;
    private static final int CHANCE_WASH = 3333;
    private static final int CHANCE_THERMAL = 3333;
    private static final int CHANCE_CENTRIFUGE = 1111;
    private static final int CHANCE_BATH = 7000;
    private static final int DISTILLED_WATER_PER_UNIT = 100;
    private static final int WATER_PER_UNIT = 100;

    private static final Set<Material> BLACKLIST = new ReferenceOpenHashSet<>();
    private static final Map<Material, Set<Route>> ALLOWED_ROUTES = new Reference2ObjectOpenHashMap<>();

    private OreProcessorRecipeHandler() {}

    public static void blacklist(Material @NotNull... materials) {
        for (Material material : materials) {
            BLACKLIST.add(material);
        }
    }

    public static void restrict(@NotNull Material material, Route @NotNull... routes) {
        Set<Route> allowed = EnumSet.noneOf(Route.class);
        for (Route route : routes) {
            allowed.add(route);
        }
        ALLOWED_ROUTES.put(material, allowed);
    }

    public static void exclude(@NotNull Material material, Route @NotNull... routes) {
        Set<Route> allowed = ALLOWED_ROUTES.computeIfAbsent(material, key -> EnumSet.allOf(Route.class));
        for (Route route : routes) {
            allowed.remove(route);
        }
    }

    public static void run(@NotNull Consumer<FinishedRecipe> provider, @NotNull Material material) {
        OreProperty property = material.getProperty(PropertyKey.ORE);
        if (property == null || BLACKLIST.contains(material)) {
            return;
        }
        Set<Route> allowed = ALLOWED_ROUTES.get(material);
        for (Route route : Route.VALUES) {
            if (allowed != null && !allowed.contains(route)) {
                continue;
            }
            emit(provider, material, property, route, TagPrefix.ore, "ore");
            emit(provider, material, property, route, TagPrefix.rawOre, "raw_ore");
        }
    }

    private static void emit(@NotNull Consumer<FinishedRecipe> provider, @NotNull Material material,
                             @NotNull OreProperty property, @NotNull Route route, @NotNull TagPrefix input,
                             @NotNull String inputName) {
        if (!material.shouldGenerateRecipesFor(input)) {
            return;
        }
        int multiplier = property.getOreMultiplier();
        int amount = multiplier * route.yieldMultiplier;
        int byAmount = Math.max(1, property.getByProductMultiplier() * multiplier);
        boolean bath = !property.getWashedIn().first().isNull();
        boolean thermal = material.shouldGenerateRecipesFor(TagPrefix.crushedRefined);
        boolean sift = material.hasProperty(PropertyKey.GEM);

        GTRecipeBuilder builder = ModRecipeTypes.ORE_PROCESSOR_RECIPES
                .recipeBuilder(material.getName() + "_" + inputName + "_mode_" + route.circuit)
                .inputItems(input, material)
                .circuitMeta(route.circuit)
                .duration(route.duration)
                .EUt(VA[IV]);

        switch (route) {
            case MACERATE_WASH_THERMAL_MACERATE -> {
                if (!primary(builder, thermal ? TagPrefix.dust : TagPrefix.dustPure, material, amount)) return;
                distilled(builder, amount);
                byProduct(builder, property, material, 0, byAmount, CHANCE_MACERATE);
                byProduct(builder, property, material, 0, byAmount, CHANCE_WASH);
                if (thermal) {
                    byProduct(builder, property, material, 1, byAmount, CHANCE_THERMAL);
                    byProduct(builder, property, material, 2, byAmount, CHANCE_MACERATE);
                } else {
                    byProduct(builder, property, material, 1, byAmount, CHANCE_MACERATE);
                }
            }
            case MACERATE_WASH_MACERATE_CENTRIFUGE -> {
                if (!primary(builder, TagPrefix.dust, material, amount)) return;
                distilled(builder, amount);
                byProduct(builder, property, material, 0, byAmount, CHANCE_MACERATE);
                byProduct(builder, property, material, 0, byAmount, CHANCE_WASH);
                byProduct(builder, property, material, 1, byAmount, CHANCE_MACERATE);
                byProduct(builder, property, material, 1, byAmount, CHANCE_CENTRIFUGE);
            }
            case MACERATE_MACERATE_CENTRIFUGE -> {
                if (!primary(builder, TagPrefix.dust, material, amount)) return;
                byProduct(builder, property, material, 0, byAmount, CHANCE_MACERATE);
                byProduct(builder, property, material, 0, byAmount, CHANCE_MACERATE);
                byProduct(builder, property, material, 0, byAmount, CHANCE_CENTRIFUGE);
            }
            case MACERATE_WASH_SIFT -> {
                if (sift) {
                    if (!gems(builder, material, amount)) return;
                } else if (!primary(builder, TagPrefix.crushedPurified, material, amount)) {
                    return;
                }
                distilled(builder, amount);
                byProduct(builder, property, material, 0, byAmount, CHANCE_MACERATE);
                byProduct(builder, property, material, 0, byAmount, CHANCE_WASH);
            }
            case MACERATE_BATH_MACERATE_CENTRIFUGE -> {
                if (!primary(builder, TagPrefix.dust, material, amount)) return;
                byProduct(builder, property, material, 0, byAmount, CHANCE_MACERATE);
                if (bath) {
                    bath(builder, property, amount);
                    byProduct(builder, property, material, 3, byAmount, CHANCE_BATH);
                    byProduct(builder, property, material, 1, byAmount, CHANCE_MACERATE);
                    byProduct(builder, property, material, 1, byAmount, CHANCE_CENTRIFUGE);
                } else {
                    byProduct(builder, property, material, 0, byAmount, CHANCE_MACERATE);
                    byProduct(builder, property, material, 0, byAmount, CHANCE_CENTRIFUGE);
                }
            }
            case MACERATE_BATH_THERMAL_MACERATE -> {
                if (!primary(builder, thermal ? TagPrefix.dust : TagPrefix.dustPure, material, amount)) return;
                byProduct(builder, property, material, 0, byAmount, CHANCE_MACERATE);
                if (bath) {
                    bath(builder, property, amount);
                    byProduct(builder, property, material, 3, byAmount, CHANCE_BATH);
                } else {
                    distilled(builder, amount);
                    byProduct(builder, property, material, 0, byAmount, CHANCE_WASH);
                }
                if (thermal) {
                    byProduct(builder, property, material, 1, byAmount, CHANCE_THERMAL);
                    byProduct(builder, property, material, 2, byAmount, CHANCE_MACERATE);
                } else {
                    byProduct(builder, property, material, 1, byAmount, CHANCE_MACERATE);
                }
            }
            case HAMMER_HAMMER_SIMPLE_WASH -> {
                if (!primary(builder, TagPrefix.dust, material, amount)) return;
                builder.inputFluids(GTMaterials.Water.getFluid(WATER_PER_UNIT * amount));
            }
        }

        builder.save(provider);
    }

    private static void distilled(@NotNull GTRecipeBuilder builder, int amount) {
        builder.inputFluids(GTMaterials.DistilledWater.getFluid(DISTILLED_WATER_PER_UNIT * amount));
    }

    private static void bath(@NotNull GTRecipeBuilder builder, @NotNull OreProperty property, int amount) {
        ObjectIntPair<Material> washedIn = property.getWashedIn();
        builder.inputFluids(washedIn.first().getFluid(washedIn.secondInt() * amount));
    }

    private static boolean primary(@NotNull GTRecipeBuilder builder, @NotNull TagPrefix prefix,
                                   @NotNull Material material, int amount) {
        ItemStack stack = ChemicalHelper.get(prefix, material, amount);
        if (stack.isEmpty()) {
            return false;
        }
        builder.outputItems(stack);
        return true;
    }

    private static void byProduct(@NotNull GTRecipeBuilder builder, @NotNull OreProperty property,
                                  @NotNull Material material, int index, int amount, int chance) {
        ItemStack stack = ChemicalHelper.get(TagPrefix.dust, property.getOreByProduct(index, material), amount);
        if (!stack.isEmpty()) {
            builder.chancedOutput(stack, chance, 0);
        }
    }

    private static boolean gems(@NotNull GTRecipeBuilder builder, @NotNull Material material, int amount) {
        boolean high = material.hasFlag(MaterialFlags.HIGH_SIFTER_OUTPUT);
        boolean any = gem(builder, TagPrefix.gemExquisite, material, amount, high ? 500 : 300);
        any |= gem(builder, TagPrefix.gemFlawless, material, amount, high ? 1500 : 1000);
        any |= gem(builder, TagPrefix.gem, material, amount, high ? 5000 : 3500);
        any |= gem(builder, TagPrefix.gemFlawed, material, amount, high ? 2000 : 2500);
        any |= gem(builder, TagPrefix.gemChipped, material, amount, high ? 3000 : 3500);
        any |= gem(builder, TagPrefix.dust, material, amount, high ? 2500 : 5000);
        return any;
    }

    private static boolean gem(@NotNull GTRecipeBuilder builder, @NotNull TagPrefix prefix, @NotNull Material material,
                               int amount, int chance) {
        ItemStack stack = ChemicalHelper.get(prefix, material, amount);
        if (stack.isEmpty()) {
            return false;
        }
        builder.chancedOutput(stack, chance, 0);
        return true;
    }
}
