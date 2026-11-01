package com.mrlacar.univom.data.recipes.misc;

import com.mrlacar.univom.UNIVOM;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.LV;
import static com.gregtechceu.gtceu.api.GTValues.VA;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys.LIQUID;
import static com.gregtechceu.gtceu.common.data.GTItems.ELECTRIC_PUMP_LV;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.mrlacar.univom.common.data.ModRecipeTypes.SUPERCONDUCTOR_ASSEMBLER_RECIPES;

public class SuperconductorAssemblerRecipes {
    public static void init(Consumer<FinishedRecipe> provider) {
        SUPERCONDUCTOR_ASSEMBLER_RECIPES.recipeBuilder(UNIVOM.id("manganese_phosphide_single_wire_from_manganese_phosphide_ingot"))
                .inputItems(ingot, ManganesePhosphide, 4)
                .inputItems(pipeTinyFluid, StainlessSteel, 2)
                .inputItems(ELECTRIC_PUMP_LV, 2)
                .inputFluids(Helium.getFluid(LIQUID, 1000))
                .outputItems(wireGtSingle, ManganesePhosphide, 4)
                .duration(400).EUt(VA[LV]).save(provider);
    }
}
