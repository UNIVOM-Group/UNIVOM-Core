package com.mrlacar.univom.mixin.ae2.client.gui.style;

import appeng.client.gui.style.FluidBlitter;
import com.gregtechceu.gtceu.GTCEu;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
@Mixin(value = FluidBlitter.class, remap = false)
public class FluidBlitterMixin {
    private static final Set<String> univom$reported = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @ModifyExpressionValue(
            method = "create(Lnet/minecraftforge/fluids/FluidStack;)Lappeng/client/gui/style/Blitter;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraftforge/client/extensions/common/IClientFluidTypeExtensions;" +
                            "getStillTexture(Lnet/minecraftforge/fluids/FluidStack;)" +
                            "Lnet/minecraft/resources/ResourceLocation;"))
    private static ResourceLocation univom$guardMissingStillTexture(ResourceLocation original, FluidStack stack) {
        if (original != null) return original;

        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
        String name = id == null ? String.valueOf(stack.getFluid()) : id.toString();
        if (univom$reported.add(name)) {
            GTCEu.LOGGER.warn("Fluid {} returned a null still texture; falling back to the missing-texture sprite", name);
        }
        return MissingTextureAtlasSprite.getLocation();
    }
}
