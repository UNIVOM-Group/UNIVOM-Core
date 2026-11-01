package com.mrlacar.univom.common.item.armor;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpaceFederationOxygenTank extends FluidHandlerItemStack {

    public static final ResourceLocation OXYGEN_FLUID = new ResourceLocation("ad_astra", "oxygen");
    public static final TagKey<Fluid> OXYGEN = TagKey.create(Registries.FLUID, OXYGEN_FLUID);

    @Nullable
    private final ICapabilityProvider parent;

    public SpaceFederationOxygenTank(@NotNull ItemStack container, int capacity,
                                     @Nullable ICapabilityProvider parent) {
        super(container, capacity);
        this.parent = parent;
    }

    @Override
    public boolean canFillFluidType(FluidStack fluid) {
        return fluid.getFluid().is(OXYGEN);
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return stack.getFluid().is(OXYGEN);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction facing) {
        LazyOptional<T> result = super.getCapability(capability, facing);
        if (result.isPresent() || parent == null) {
            return result;
        }
        return parent.getCapability(capability, facing);
    }

    public static int readAmount(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(FLUID_NBT_KEY)) {
            return 0;
        }
        return tag.getCompound(FLUID_NBT_KEY).getInt("Amount");
    }

    public static void fill(ItemStack stack, int amount) {
        if (amount <= 0) {
            return;
        }
        Fluid oxygen = BuiltInRegistries.FLUID.get(OXYGEN_FLUID);
        if (oxygen == Fluids.EMPTY) {
            return;
        }
        IFluidHandlerItem handler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        if (handler != null) {
            handler.fill(new FluidStack(oxygen, amount), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    public static boolean consume(ItemStack stack, int amount) {
        if (amount <= 0) {
            return true;
        }
        IFluidHandlerItem handler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        if (handler == null) {
            return false;
        }
        if (handler.drain(amount, IFluidHandler.FluidAction.SIMULATE).getAmount() < amount) {
            return false;
        }
        handler.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        return true;
    }
}
