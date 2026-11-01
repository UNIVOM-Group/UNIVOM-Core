package com.mrlacar.univom.client.tooltip;

import com.mrlacar.univom.api.chemistry.Molecule;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record MoleculeTooltip(Molecule molecule) implements TooltipComponent {}
