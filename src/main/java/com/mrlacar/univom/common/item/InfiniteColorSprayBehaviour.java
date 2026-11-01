package com.mrlacar.univom.common.item;

import appeng.api.implementations.blockentities.IColorableBlockEntity;
import appeng.api.util.AEColor;
import appeng.blockentity.networking.CableBusBlockEntity;
import com.google.common.collect.ImmutableMap;
import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.blockentity.IPaintable;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory;
import com.gregtechceu.gtceu.api.pipenet.IPipeNode;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTSoundEntries;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.gregtechceu.gtceu.utils.BreadthFirstBlockSearch;
import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.util.TriPredicate;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class InfiniteColorSprayBehaviour implements IInteractionItem, IAddInformation, IItemUIFactory {
    private static final ImmutableMap<DyeColor, Block> GLASS_MAP;
    private static final ImmutableMap<DyeColor, Block> GLASS_PANE_MAP;
    private static final ImmutableMap<DyeColor, Block> TERRACOTTA_MAP;
    private static final ImmutableMap<DyeColor, Block> WOOL_MAP;
    private static final ImmutableMap<DyeColor, Block> CARPET_MAP;
    private static final ImmutableMap<DyeColor, Block> CONCRETE_MAP;
    private static final ImmutableMap<DyeColor, Block> CONCRETE_POWDER_MAP;
    private static final ImmutableMap<DyeColor, Block> SHULKER_BOX_MAP;
    private static final ImmutableMap<DyeColor, Block> CANDLE_MAP;

    @SuppressWarnings("deprecation")
    private static Block getBlock(DyeColor color, String postfix) {
        ResourceLocation id = new ResourceLocation("minecraft", color.getSerializedName() + "_" + postfix);
        return BuiltInRegistries.BLOCK.get(id);
    }

    static {
        ImmutableMap.Builder<DyeColor, Block> glassBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<DyeColor, Block> glassPaneBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<DyeColor, Block> terracottaBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<DyeColor, Block> woolBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<DyeColor, Block> carpetBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<DyeColor, Block> concreteBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<DyeColor, Block> concretePowderBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<DyeColor, Block> shulkerBoxBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<DyeColor, Block> candleBuilder = ImmutableMap.builder();

        for (DyeColor color : DyeColor.values()) {
            glassBuilder.put(color, getBlock(color, "stained_glass"));
            glassPaneBuilder.put(color, getBlock(color, "stained_glass_pane"));
            terracottaBuilder.put(color, getBlock(color, "terracotta"));
            woolBuilder.put(color, getBlock(color, "wool"));
            carpetBuilder.put(color, getBlock(color, "carpet"));
            concreteBuilder.put(color, getBlock(color, "concrete"));
            concretePowderBuilder.put(color, getBlock(color, "concrete_powder"));
            shulkerBoxBuilder.put(color, getBlock(color, "shulker_box"));
            candleBuilder.put(color, getBlock(color, "candle"));
        }
        GLASS_MAP = glassBuilder.build();
        GLASS_PANE_MAP = glassPaneBuilder.build();
        TERRACOTTA_MAP = terracottaBuilder.build();
        WOOL_MAP = woolBuilder.build();
        CARPET_MAP = carpetBuilder.build();
        CONCRETE_MAP = concreteBuilder.build();
        CONCRETE_POWDER_MAP = concretePowderBuilder.build();
        SHULKER_BOX_MAP = shulkerBoxBuilder.build();
        CANDLE_MAP = candleBuilder.build();
    }

    private static final String NBT_COLOR = "SprayColor";

    public static int getColorConfig(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(NBT_COLOR) ? tag.getInt(NBT_COLOR) : -1;
    }

    public static void setColorConfig(ItemStack stack, int color) {
        stack.getOrCreateTag().putInt(NBT_COLOR, color);
    }

    @Nullable
    public static DyeColor getDyeColor(ItemStack stack) {
        int colorId = getColorConfig(stack);
        return (colorId >= 0 && colorId < DyeColor.values().length) ? DyeColor.values()[colorId] : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        DyeColor color = getDyeColor(stack);
        if (color != null) {
            tooltipComponents.add(Component.translatable("behaviour.paintspray." + color.getSerializedName() + ".tooltip").withStyle(style -> style.withColor(color.getTextColor())));
        } else {
            tooltipComponents.add(Component.translatable("behaviour.paintspray.solvent.tooltip"));
        }
        tooltipComponents.add(Component.translatable("behaviour.paintspray.configure"));
    }

    @Override
    public ModularUI createUI(HeldItemUIFactory.HeldItemHolder holder, Player entityPlayer) {
        LabelWidget label = new LabelWidget(9, 8, "Spray Can Color Configuration");
        label.setDropShadow(false);
        label.setTextColor(0x404040);

        var modular = new ModularUI(176, 60, holder, entityPlayer).widget(label);

        for (int i = 0; i <= 16; i++) {
            int row = i / 9;
            int col = i % 9;
            int x = 7 + col * 18;
            int y = 20 + row * 18;

            final int colorIndex = (i == 0) ? -1 : (i - 1);
            ItemStack displayStack = (colorIndex == -1) ? GTItems.SPRAY_SOLVENT.asStack() : GTItems.SPRAY_CAN_DYES[colorIndex].asStack();

            ButtonWidget button = new ButtonWidget(x, y, 18, 18,
                    new GuiTextureGroup(GuiTextures.SLOT, new ItemStackTexture(displayStack)),
                    data -> {
                        setColorConfig(holder.getHeld(), colorIndex);
                    });

            button.setHoverTooltips(displayStack.getHoverName());
            modular.widget(button);
        }

        modular.mainGroup.setBackground(GuiTextures.BACKGROUND);
        return modular;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack itemStack, UseOnContext context) {
        var player = context.getPlayer();
        var level = context.getLevel();
        var pos = context.getClickedPos();

        if (player == null) {
            return InteractionResult.PASS;
        }

        int maxBlocksToRecolor = player.isShiftKeyDown() ? ConfigHolder.INSTANCE.tools.sprayCanChainLength : 1;
        DyeColor currentColor = getDyeColor(itemStack);

        var first = level.getBlockEntity(pos);
        if (first == null || !handleSpecialBlockEntities(first, maxBlocksToRecolor, context, currentColor)) {
            handleBlocks(pos, maxBlocksToRecolor, context, currentColor);
        }
        GTSoundEntries.SPRAY_CAN_TOOL.play(level, null, player.position(), 1.0f, 1.0f);
        return InteractionResult.SUCCESS;
    }

    private static boolean paintPaintable(IPaintable paintable, DyeColor color) {
        if (color == null) {
            if (!paintable.isPainted()) {
                return false;
            }
            paintable.setPaintingColor(IPaintable.UNPAINTED_COLOR);
        } else if (paintable.getPaintingColor() != color.getMapColor().col) {
            paintable.setPaintingColor(color.getMapColor().col);
        } else {
            return false;
        }
        return true;
    }

    @SuppressWarnings({ "rawtypes", "DataFlowIssue" })
    private boolean handleSpecialBlockEntities(BlockEntity first, int limit, UseOnContext context, DyeColor color) {
        var player = context.getPlayer();
        if (player == null) {
            return false;
        }
        if (GTCEu.Mods.isAE2Loaded() && AE2CallWrapper.isColorable(first)) {
            Set<? extends IColorableBlockEntity> collected;
            if (first instanceof CableBusBlockEntity) {
                collected = AE2CallWrapper.collect(first, limit);
            } else {
                collected = Set.of((IColorableBlockEntity) first);
            }
            var ae2Color = color == null ? AEColor.TRANSPARENT : AEColor.values()[color.ordinal()];
            for (var c : collected) {
                if (c.getColor() == ae2Color) {
                    continue;
                }
                c.recolourBlock(context.getClickedFace(), ae2Color, player);
            }
        } else if (first instanceof IPipeNode pipe) {
            var collected = BreadthFirstBlockSearch.conditionalSearch(IPipeNode.class, pipe,
                    first.getLevel(), IPipeNode::getPipePos,
                    gtPipePredicate, limit, limit * 6);
            paintPaintables(collected, context, color);
        } else if (first instanceof IPaintable paintable) {
            var collected = BreadthFirstBlockSearch.conditionalSearch(IPaintable.class, paintable,
                    first.getLevel(), p -> ((BlockEntity) p).getBlockPos(),
                    paintablePredicate, limit, limit * 6);
            paintPaintables(collected, context, color);
        } else if (first instanceof ShulkerBoxBlockEntity shulkerBox) {
            var tag = shulkerBox.saveWithoutMetadata();
            var level = first.getLevel();
            var pos = first.getBlockPos();
            recolorBlockNoState(SHULKER_BOX_MAP, color, level, pos, Blocks.SHULKER_BOX);
            if (level.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity newShulker) {
                newShulker.load(tag);
            }
        } else {
            return false;
        }
        return true;
    }

    private void handleBlocks(BlockPos start, int limit, UseOnContext context, DyeColor color) {
        final var level = context.getLevel();
        var player = context.getPlayer();
        if (player == null) {
            return;
        }
        var collected = BreadthFirstBlockSearch
                .conditionalBlockPosSearch(start,
                        (parent, child) -> parent == null ||
                                level.getBlockState(child).is(level.getBlockState(parent).getBlock()),
                        limit, limit * 6);
        for (var pos : collected) {
            if (!tryPaintBlock(level, pos, color)) {
                break;
            }
        }
    }

    private <T extends IPaintable> void paintPaintables(Set<T> paintables, UseOnContext context, DyeColor color) {
        for (var c : paintables) {
            if (!paintPaintable(c, color)) {
                continue;
            }
        }
    }

    private boolean tryPaintBlock(Level level, BlockPos pos, DyeColor color) {
        var blockState = level.getBlockState(pos);
        var block = blockState.getBlock();
        if (color == null) {
            return tryStripBlockColor(level, pos, block);
        }
        return recolorBlockState(level, pos, color) || tryPaintSpecialBlock(level, pos, block, color);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static boolean recolorBlockState(Level level, BlockPos pos, DyeColor color) {
        BlockState state = level.getBlockState(pos);
        for (Property property : state.getProperties()) {
            if (property.getValueClass() == DyeColor.class) {
                level.setBlockAndUpdate(pos, state.setValue(property, color));
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean tryPaintSpecialBlock(Level world, BlockPos pos, Block block, DyeColor color) {
        if (block.defaultBlockState().is(Tags.Blocks.GLASS)) {
            if (recolorBlockNoState(GLASS_MAP, color, world, pos, Blocks.GLASS)) {
                return true;
            }
        }
        if (block.defaultBlockState().is(Tags.Blocks.GLASS_PANES)) {
            if (recolorBlockNoState(GLASS_PANE_MAP, color, world, pos, Blocks.GLASS_PANE)) {
                return true;
            }
        }
        if (block.defaultBlockState().is(BlockTags.TERRACOTTA)) {
            if (recolorBlockNoState(TERRACOTTA_MAP, color, world, pos, Blocks.TERRACOTTA)) {
                return true;
            }
        }
        if (block.defaultBlockState().is(BlockTags.WOOL)) {
            if (recolorBlockNoState(WOOL_MAP, color, world, pos)) {
                return true;
            }
        }
        if (block.defaultBlockState().is(BlockTags.WOOL_CARPETS)) {
            if (recolorBlockNoState(CARPET_MAP, color, world, pos)) {
                return true;
            }
        }
        if (block.defaultBlockState().is(CustomTags.CONCRETE_BLOCK)) {
            if (recolorBlockNoState(CONCRETE_MAP, color, world, pos)) {
                return true;
            }
        }
        if (block.defaultBlockState().is(CustomTags.CONCRETE_POWDER_BLOCK)) {
            if (recolorBlockNoState(CONCRETE_POWDER_MAP, color, world, pos)) {
                return true;
            }
        }
        if (block.defaultBlockState().is(BlockTags.CANDLES)) {
            if (recolorBlockNoState(CANDLE_MAP, color, world, pos)) {
                return true;
            }
        }
        return false;
    }

    private static boolean recolorBlockNoState(Map<DyeColor, Block> map, DyeColor color, Level world, BlockPos pos) {
        return recolorBlockNoState(map, color, world, pos, null);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static boolean recolorBlockNoState(Map<DyeColor, Block> map, @Nullable DyeColor color,
                                               Level level, BlockPos pos, Block defaultBlock) {
        Block newBlock = map.getOrDefault(color, defaultBlock);
        if (newBlock == Blocks.AIR) newBlock = defaultBlock;

        BlockState old = level.getBlockState(pos);
        if (newBlock != null && newBlock != old.getBlock()) {
            BlockState state = newBlock.defaultBlockState();
            for (Property property : old.getProperties()) {
                if (!state.hasProperty(property)) continue;
                state.setValue(property, old.getValue(property));
            }
            level.setBlockAndUpdate(pos, state);
            return true;
        }
        return false;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static boolean tryStripBlockColor(Level world, BlockPos pos, Block block) {
        if (block instanceof StainedGlassBlock) {
            world.setBlockAndUpdate(pos, Blocks.GLASS.defaultBlockState());
            return true;
        }
        if (block instanceof StainedGlassPaneBlock) {
            world.setBlockAndUpdate(pos, Blocks.GLASS_PANE.defaultBlockState());
            return true;
        }
        if (block.defaultBlockState().is(BlockTags.TERRACOTTA) && block != Blocks.TERRACOTTA) {
            world.setBlockAndUpdate(pos, Blocks.TERRACOTTA.defaultBlockState());
            return true;
        }
        if (block.defaultBlockState().is(BlockTags.WOOL) && block != Blocks.WHITE_WOOL) {
            world.setBlockAndUpdate(pos, Blocks.WHITE_WOOL.defaultBlockState());
            return true;
        }
        if (block.defaultBlockState().is(BlockTags.WOOL_CARPETS) && block != Blocks.WHITE_CARPET) {
            world.setBlockAndUpdate(pos, Blocks.WHITE_CARPET.defaultBlockState());
            return true;
        }
        if (block.defaultBlockState().is(CustomTags.CONCRETE_BLOCK) && block != Blocks.WHITE_CONCRETE) {
            world.setBlockAndUpdate(pos, Blocks.WHITE_CONCRETE.defaultBlockState());
            return true;
        }
        if (block.defaultBlockState().is(CustomTags.CONCRETE_POWDER_BLOCK) && block != Blocks.WHITE_CONCRETE_POWDER) {
            world.setBlockAndUpdate(pos, Blocks.WHITE_CONCRETE_POWDER.defaultBlockState());
            return true;
        }
        if (block.defaultBlockState().is(BlockTags.CANDLES) && block != Blocks.WHITE_CANDLE) {
            recolorBlockNoState(CANDLE_MAP, DyeColor.WHITE, world, pos);
            return true;
        }

        BlockState state = world.getBlockState(pos);
        for (Property prop : state.getProperties()) {
            if (prop.getValueClass() == DyeColor.class) {
                BlockState defaultState = block.defaultBlockState();
                DyeColor defaultColor = DyeColor.WHITE;
                try {
                    defaultColor = (DyeColor) defaultState.getValue(prop);
                } catch (IllegalArgumentException ignored) {
                }
                recolorBlockState(world, pos, defaultColor);
                return true;
            }
        }

        return false;
    }

    private static final TriPredicate<IPaintable, IPaintable, Direction> paintablePredicate = (parent, child, dir) -> {
        if (parent == null) return true;
        if (!parent.getClass().equals(child.getClass())) {
            return false;
        }
        return parent.getPaintingColor() == child.getPaintingColor();
    };

    @SuppressWarnings("rawtypes")
    private static final TriPredicate<IPipeNode, IPipeNode, Direction> gtPipePredicate = (parent, child, direction) -> {
        if (parent == null) return true;
        if (!paintablePredicate.test(parent, child, direction)) {
            return false;
        }
        return parent.isConnected(direction) && child.isConnected(direction.getOpposite());
    };

    private static class AE2CallWrapper {

        static Set<CableBusBlockEntity> collect(BlockEntity first, int limit) {
            return BreadthFirstBlockSearch.conditionalBlockEntitySearch(CableBusBlockEntity.class,
                    (CableBusBlockEntity) first,
                    AE2CallWrapper::ae2CablePredicate,
                    limit, limit * 6);
        }

        static boolean isColorable(BlockEntity be) {
            return be instanceof IColorableBlockEntity;
        }

        static boolean ae2CablePredicate(CableBusBlockEntity parent, CableBusBlockEntity child, Direction direction) {
            if (parent == null) return true;
            var childDirection = direction.getOpposite();
            return parent.getPart(direction) == null && parent.getCableConnectionType(direction).isValid() &&
                    child.getPart(childDirection) == null && child.getCableConnectionType(childDirection).isValid() &&
                    parent.getColor() == child.getColor();
        }
    }
}
