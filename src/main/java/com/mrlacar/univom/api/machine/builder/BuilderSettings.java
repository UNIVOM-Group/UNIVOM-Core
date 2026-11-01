package com.mrlacar.univom.api.machine.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import org.jetbrains.annotations.Nullable;

public final class BuilderSettings {

    public static final int MIN_LAYERS = 1;
    public static final int MAX_LAYERS = 32;
    public static final int DEFAULT_LAYERS = 4;
    public static final int MAX_BLOCKS_PER_OPERATION = 8192;

    private static final String ROOT_TAG = "multiblock_builder";
    private static final String LAYERS_TAG = "layers";
    private static final String DISMANTLE_TAG = "dismantle";
    private static final String PLACE_PARTS_TAG = "placeParts";
    private static final String USE_NETWORK_TAG = "useNetwork";
    private static final String AE_TAG = "ae";
    private static final String AE_DIM_TAG = "dim";
    private static final String AE_X_TAG = "x";
    private static final String AE_Y_TAG = "y";
    private static final String AE_Z_TAG = "z";
    private static final String MODULES_TAG = "modules";
    private static final String MODULE_BLOCK_TAG = "block";
    private static final String MODULE_REPLACE_TAG = "replace";

    private BuilderSettings() {}

    @Nullable
    private static CompoundTag read(ItemStack stack) {
        return stack.getTagElement(ROOT_TAG);
    }

    private static CompoundTag write(ItemStack stack) {
        return stack.getOrCreateTagElement(ROOT_TAG);
    }

    public static int getLayers(ItemStack stack) {
        CompoundTag tag = read(stack);
        return tag != null && tag.contains(LAYERS_TAG, Tag.TAG_INT) ?
                Mth.clamp(tag.getInt(LAYERS_TAG), MIN_LAYERS, MAX_LAYERS) : DEFAULT_LAYERS;
    }

    public static void setLayers(ItemStack stack, int layers) {
        write(stack).putInt(LAYERS_TAG, Mth.clamp(layers, MIN_LAYERS, MAX_LAYERS));
    }

    public static boolean isDismantleMode(ItemStack stack) {
        CompoundTag tag = read(stack);
        return tag != null && tag.getBoolean(DISMANTLE_TAG);
    }

    public static void setDismantleMode(ItemStack stack, boolean dismantle) {
        write(stack).putBoolean(DISMANTLE_TAG, dismantle);
    }

    public static boolean isPlaceParts(ItemStack stack) {
        CompoundTag tag = read(stack);
        return tag == null || !tag.contains(PLACE_PARTS_TAG) || tag.getBoolean(PLACE_PARTS_TAG);
    }

    public static void setPlaceParts(ItemStack stack, boolean placeParts) {
        write(stack).putBoolean(PLACE_PARTS_TAG, placeParts);
    }

    public static boolean isUseNetwork(ItemStack stack) {
        CompoundTag tag = read(stack);
        return tag != null && tag.getBoolean(USE_NETWORK_TAG);
    }

    public static void setUseNetwork(ItemStack stack, boolean useNetwork) {
        write(stack).putBoolean(USE_NETWORK_TAG, useNetwork);
    }

    @Nullable
    public static GlobalPos getNetwork(ItemStack stack) {
        CompoundTag tag = read(stack);
        if (tag == null || !tag.contains(AE_TAG, Tag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag ae = tag.getCompound(AE_TAG);
        ResourceLocation dimension = ResourceLocation.tryParse(ae.getString(AE_DIM_TAG));
        if (dimension == null) {
            return null;
        }
        return GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimension),
                new BlockPos(ae.getInt(AE_X_TAG), ae.getInt(AE_Y_TAG), ae.getInt(AE_Z_TAG)));
    }

    public static boolean hasNetwork(ItemStack stack) {
        CompoundTag tag = read(stack);
        return tag != null && tag.contains(AE_TAG, Tag.TAG_COMPOUND);
    }

    @Nullable
    public static CompoundTag getNetworkTag(ItemStack stack) {
        CompoundTag tag = read(stack);
        return tag == null || !tag.contains(AE_TAG, Tag.TAG_COMPOUND) ? null : tag.getCompound(AE_TAG);
    }

    public static String networkDimension(CompoundTag network) {
        return network.getString(AE_DIM_TAG);
    }

    public static int networkX(CompoundTag network) {
        return network.getInt(AE_X_TAG);
    }

    public static int networkY(CompoundTag network) {
        return network.getInt(AE_Y_TAG);
    }

    public static int networkZ(CompoundTag network) {
        return network.getInt(AE_Z_TAG);
    }

    public static void setNetwork(ItemStack stack, ResourceKey<Level> dimension, BlockPos pos) {
        CompoundTag ae = new CompoundTag();
        ae.putString(AE_DIM_TAG, dimension.location().toString());
        ae.putInt(AE_X_TAG, pos.getX());
        ae.putInt(AE_Y_TAG, pos.getY());
        ae.putInt(AE_Z_TAG, pos.getZ());
        write(stack).put(AE_TAG, ae);
    }

    public static void clearNetwork(ItemStack stack) {
        CompoundTag tag = read(stack);
        if (tag != null) {
            tag.remove(AE_TAG);
        }
    }

    public static String getModuleBlockId(ItemStack stack, BuilderModuleCategory category) {
        CompoundTag tag = read(stack);
        if (tag == null) {
            return category.defaultBlockId();
        }
        String id = tag.getCompound(MODULES_TAG).getCompound(category.name()).getString(MODULE_BLOCK_TAG);
        return id.isEmpty() ? category.defaultBlockId() : id;
    }

    public static Block getModuleBlock(ItemStack stack, BuilderModuleCategory category) {
        Block block = category.byId(getModuleBlockId(stack, category));
        return block == null ? category.defaultBlock() : block;
    }

    public static void setModuleBlock(ItemStack stack, BuilderModuleCategory category, Block block) {
        module(stack, category).putString(MODULE_BLOCK_TAG, BuilderModuleCategory.idOf(block));
    }

    public static boolean isModuleReplace(ItemStack stack, BuilderModuleCategory category) {
        CompoundTag tag = read(stack);
        return tag != null &&
                tag.getCompound(MODULES_TAG).getCompound(category.name()).getBoolean(MODULE_REPLACE_TAG);
    }

    public static void setModuleReplace(ItemStack stack, BuilderModuleCategory category, boolean replace) {
        module(stack, category).putBoolean(MODULE_REPLACE_TAG, replace);
    }

    public static boolean hasAnyReplace(ItemStack stack) {
        CompoundTag tag = read(stack);
        if (tag == null || !tag.contains(MODULES_TAG, Tag.TAG_COMPOUND)) {
            return false;
        }
        CompoundTag modules = tag.getCompound(MODULES_TAG);
        for (BuilderModuleCategory category : BuilderModules.ALL) {
            if (modules.getCompound(category.name()).getBoolean(MODULE_REPLACE_TAG)) {
                return true;
            }
        }
        return false;
    }

    private static CompoundTag module(ItemStack stack, BuilderModuleCategory category) {
        CompoundTag tag = write(stack);
        CompoundTag modules = tag.getCompound(MODULES_TAG);
        CompoundTag entry = modules.getCompound(category.name());
        modules.put(category.name(), entry);
        tag.put(MODULES_TAG, modules);
        return entry;
    }
}
