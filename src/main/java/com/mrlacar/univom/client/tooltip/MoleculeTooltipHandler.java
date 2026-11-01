package com.mrlacar.univom.client.tooltip;

import com.mrlacar.univom.api.chemistry.Molecule;
import com.mrlacar.univom.config.ConfigHolder;
import com.mrlacar.univom.mixin.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltipAccessor;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.datafixers.util.Either;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class MoleculeTooltipHandler {

    private static final String SHIFT_KEY = "univom.tooltip.molecule.shift";

    private MoleculeTooltipHandler() {}

    public static boolean isActive() {
        return ConfigHolder.INSTANCE.moleculeTooltipSetting.enabled;
    }

    @Nullable
    public static Material materialOf(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof BucketItem bucket) {
            return validate(ChemicalHelper.getMaterial(bucket.getFluid()));
        }
        MaterialEntry entry = ChemicalHelper.getMaterialEntry(item);
        return entry.isEmpty() ? null : validate(entry.material());
    }

    @Nullable
    public static Material materialOf(Fluid fluid) {
        return validate(ChemicalHelper.getMaterial(fluid));
    }

    public static void decorate(ItemStack stack, List<Either<FormattedText, TooltipComponent>> elements) {
        if (!isActive()) {
            return;
        }
        Material material = materialOf(stack);
        if (material != null) {
            decorate(material, elements);
        }
    }

    public static void decorate(Material material, List<Either<FormattedText, TooltipComponent>> elements) {
        ConfigHolder.MoleculeTooltipConfigs config = ConfigHolder.INSTANCE.moleculeTooltipSetting;
        Molecule molecule = MoleculeAtlas.molecule(material);
        boolean alloy = molecule == null && config.showAlloyPieChart && MoleculeAtlas.isAlloy(material);
        if (molecule == null && !alloy && !config.colorizeFormula) {
            return;
        }

        String formula = material.getChemicalFormula();
        int index = findFormula(elements, formula);

        if (!config.onlyShowOnShift || GTUtil.isShiftDown()) {
            if (molecule != null) {
                put(elements, index, Either.right(new MoleculeTooltip(molecule)));
                return;
            }
            if (alloy) {
                AlloyTooltip drawing = AlloyTooltip.of(material);
                if (drawing != null) {
                    put(elements, index, Either.right(drawing));
                    return;
                }
            }
        }

        if (config.colorizeFormula && index >= 0) {
            elements.set(index, Either.left(ChemColor.colorizeFormula(formula)));
        }
        if (config.onlyShowOnShift && (molecule != null || alloy)) {
            elements.add(Math.max(index, 0) + 1,
                    Either.left(Component.translatable(SHIFT_KEY).withStyle(ChatFormatting.DARK_GRAY)));
        }
    }

    public static void decorateFluid(Fluid fluid, @Nullable List<ClientTooltipComponent> list) {
        if (list == null || !isActive()) {
            return;
        }
        Material material = materialOf(fluid);
        if (material == null) {
            return;
        }

        ConfigHolder.MoleculeTooltipConfigs config = ConfigHolder.INSTANCE.moleculeTooltipSetting;
        Molecule molecule = MoleculeAtlas.molecule(material);
        boolean alloy = molecule == null && config.showAlloyPieChart && MoleculeAtlas.isAlloy(material);
        if (molecule == null && !alloy && !config.colorizeFormula) {
            return;
        }

        String formula = material.getChemicalFormula();
        int index = findFormulaComponent(list, formula);

        if (!config.onlyShowOnShift || GTUtil.isShiftDown()) {
            if (molecule != null) {
                put(list, index, ClientTooltipComponent.create(new MoleculeTooltip(molecule)));
                return;
            }
            if (alloy) {
                AlloyTooltip drawing = AlloyTooltip.of(material);
                if (drawing != null) {
                    put(list, index, ClientTooltipComponent.create(drawing));
                    return;
                }
            }
        }

        if (config.colorizeFormula && index >= 0) {
            list.set(index, ClientTooltipComponent.create(ChemColor.colorizeFormula(formula).getVisualOrderText()));
        }
        if (config.onlyShowOnShift && (molecule != null || alloy)) {
            list.add(Math.max(index, 0) + 1, ClientTooltipComponent.create(Component.translatable(SHIFT_KEY)
                    .withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText()));
        }
    }

    private static int findFormulaComponent(List<ClientTooltipComponent> list, @Nullable String formula) {
        if (formula == null || formula.isEmpty()) {
            return -1;
        }
        StringBuilder scratch = new StringBuilder(formula.length() + 8);
        for (int i = 0; i < list.size(); i++) {
            if (!(list.get(i) instanceof ClientTextTooltip text)) {
                continue;
            }
            scratch.setLength(0);
            ((ClientTextTooltipAccessor) text).univom$getText()
                    .accept((position, style, codePoint) -> {
                        scratch.appendCodePoint(codePoint);
                        return true;
                    });
            if (scratch.length() == formula.length() && scratch.indexOf(formula) == 0) {
                return i;
            }
        }
        return -1;
    }

    private static void put(List<ClientTooltipComponent> list, int index, ClientTooltipComponent value) {
        if (index >= 0) {
            list.set(index, value);
        } else {
            list.add(Math.min(1, list.size()), value);
        }
    }

    private static int findFormula(List<Either<FormattedText, TooltipComponent>> elements, @Nullable String formula) {
        if (formula == null || formula.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < elements.size(); i++) {
            FormattedText text = elements.get(i).left().orElse(null);
            if (text != null && matches(text, formula)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean matches(FormattedText text, String formula) {
        if (text instanceof Component component && component.getSiblings().isEmpty()) {
            ComponentContents contents = component.getContents();
            if (contents instanceof LiteralContents literal) {
                return literal.text().equals(formula);
            }
        }
        return text.getString().equals(formula);
    }

    private static void put(List<Either<FormattedText, TooltipComponent>> elements, int index,
                            Either<FormattedText, TooltipComponent> value) {
        if (index >= 0) {
            elements.set(index, value);
        } else {
            elements.add(Math.min(1, elements.size()), value);
        }
    }

    @Nullable
    private static Material validate(@Nullable Material material) {
        return material == null || material.isNull() ? null : material;
    }
}
