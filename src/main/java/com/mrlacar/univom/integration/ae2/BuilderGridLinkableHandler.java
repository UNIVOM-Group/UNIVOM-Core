package com.mrlacar.univom.integration.ae2;

import com.mrlacar.univom.api.machine.builder.BuilderSettings;
import com.mrlacar.univom.common.data.ModItems;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.ItemStack;

import appeng.api.features.IGridLinkableHandler;

public class BuilderGridLinkableHandler implements IGridLinkableHandler {

    public static final BuilderGridLinkableHandler INSTANCE = new BuilderGridLinkableHandler();

    private BuilderGridLinkableHandler() {}

    @Override
    public boolean canLink(ItemStack stack) {
        return stack.is(ModItems.MULTIBLOCK_MACHINE_BUILDER.get());
    }

    @Override
    public void link(ItemStack stack, GlobalPos pos) {
        BuilderSettings.setNetwork(stack, pos.dimension(), pos.pos());
    }

    @Override
    public void unlink(ItemStack stack) {
        BuilderSettings.clearNetwork(stack);
    }
}
