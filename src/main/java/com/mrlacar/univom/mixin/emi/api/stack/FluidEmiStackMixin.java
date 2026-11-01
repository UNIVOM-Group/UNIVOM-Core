package com.mrlacar.univom.mixin.emi.api.stack;

import com.mrlacar.univom.client.tooltip.MoleculeTooltipHandler;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.level.material.Fluid;

import dev.emi.emi.api.stack.FluidEmiStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = FluidEmiStack.class, remap = false)
public class FluidEmiStackMixin {

    @Shadow
    @Final
    private Fluid fluid;

    @Inject(method = "getTooltip", at = @At("RETURN"), require = 0)
    private void univom$appendMoleculeDrawing(CallbackInfoReturnable<List<ClientTooltipComponent>> cir) {
        MoleculeTooltipHandler.decorateFluid(fluid, cir.getReturnValue());
    }
}
