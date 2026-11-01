package com.mrlacar.univom.client.tooltip;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record AlloyTooltip(AlloySlices slices) implements TooltipComponent {

    @Nullable
    public static AlloyTooltip of(Material material) {
        AlloySlices slices = AlloySlices.of(material);
        return slices == null ? null : new AlloyTooltip(slices);
    }
}
