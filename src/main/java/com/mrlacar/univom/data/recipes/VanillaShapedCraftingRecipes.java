package com.mrlacar.univom.data.recipes;

import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;
import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.common.data.ModBlocks;
import com.mrlacar.univom.common.data.machines.ModMultiMachines;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.LV;

public class VanillaShapedCraftingRecipes {
    public static void init(Consumer<FinishedRecipe> provider) {
        VanillaRecipeHelper.addShapedRecipe(provider, UNIVOM.id("remelting_furnace"),
                ModMultiMachines.REMELTING_FURNACE.asStack(),
                "ABA",
                "CDC",
                "ECE",
                'A', GTMachines.ELECTRIC_FURNACE[LV].asStack(),
                'B', GTMachines.EXTRACTOR[LV].asStack(),
                'C', CustomTags.MV_CIRCUITS,
                'D', GTBlocks.CASING_INVAR_HEATPROOF.asStack(),
                'E', new MaterialEntry(TagPrefix.cableGtSingle, GTMaterials.Copper));

        VanillaRecipeHelper.addShapedRecipe(provider, UNIVOM.id("mob_crushing_wheels"),
                ModBlocks.MOB_CRUSHING_WHEELS.asStack(2),
                "AAA",
                "BCB",
                "BDB",
                'A', new MaterialEntry(TagPrefix.gearSmall, GTMaterials.Steel),
                'B', new MaterialEntry(TagPrefix.gear, GTMaterials.Steel),
                'C', GTBlocks.CASING_STEEL_SOLID.asStack(),
                'D', GTItems.ELECTRIC_MOTOR_LV.asStack());
    }
}
