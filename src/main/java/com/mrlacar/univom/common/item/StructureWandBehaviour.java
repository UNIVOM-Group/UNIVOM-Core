package com.mrlacar.univom.common.item;

import com.mrlacar.univom.api.pattern.DebugBlockPattern;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory;
import com.gregtechceu.gtceu.api.item.component.ISubItemHandler;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.mrlacar.univom.config.ConfigHolder;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import com.google.common.base.Joiner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class StructureWandBehaviour implements IItemUIFactory, ISubItemHandler {
    public static final StructureWandBehaviour INSTANCE = new StructureWandBehaviour();

    private static final String structure_wand_TAG = "structure_wand";
    private static final String DIR_TAG = "dir";
    private static final String MIN_X_TAG = "minX";
    private static final String MAX_X_TAG = "maxX";
    private static final String MIN_Y_TAG = "minY";
    private static final String MAX_Y_TAG = "maxY";
    private static final String MIN_Z_TAG = "minZ";
    private static final String MAX_Z_TAG = "maxZ";
    private static final int TEXT_COLOR = 0xFAF9F6;
    private static final int CONTAINER_WIDTH = 160;
    private static final int CONTAINER_HEIGHT = 54;
    private static final int UI_WIDTH = 176;
    private static final int UI_HEIGHT = 120;

    @Override
    public ModularUI createUI(HeldItemUIFactory.HeldItemHolder playerInventoryHolder, Player entityPlayer) {
        WidgetGroup container = createInfoContainer(playerInventoryHolder);
        return new ModularUI(UI_WIDTH, UI_HEIGHT, playerInventoryHolder, entityPlayer)
                .background(GuiTextures.BACKGROUND)
                .widget(container)
                .widget(createExportButton(playerInventoryHolder))
                .widget(createChangeDirXButton(playerInventoryHolder))
                .widget(createChangeDirYButton(playerInventoryHolder));
    }

    private WidgetGroup createInfoContainer(HeldItemUIFactory.HeldItemHolder holder) {
        WidgetGroup container = new WidgetGroup(8, 8, CONTAINER_WIDTH, CONTAINER_HEIGHT);
        container.setBackground(GuiTextures.BACKGROUND_INVERSE);

        container.addWidget(new ImageWidget(4, 4, 152, 46, GuiTextures.DISPLAY));
        container.addWidget(createDimensionsLabel(holder));
        container.addWidget(createDirectionsLabel(holder));

        return container;
    }

    private LabelWidget createDimensionsLabel(HeldItemUIFactory.HeldItemHolder holder) {
        return new LabelWidget(7, 7, () -> {
            BlockPos[] blockPos = getPos(holder.getHeld());
            if (blockPos == null) {
                return LocalizationUtils.format("univom.tooltip.structure_wand.5", 0, 0, 0);
            }
            int x = 1 + blockPos[1].getX() - blockPos[0].getX();
            int y = 1 + blockPos[1].getY() - blockPos[0].getY();
            int z = 1 + blockPos[1].getZ() - blockPos[0].getZ();
            return LocalizationUtils.format("univom.tooltip.structure_wand.5", x, y, z);
        }).setTextColor(TEXT_COLOR);
    }

    private LabelWidget createDirectionsLabel(HeldItemUIFactory.HeldItemHolder holder) {
        return new LabelWidget(7, 20, () -> {
            Direction direction = getDir(holder.getHeld());
            RelativeDirection[] dirs = DebugBlockPattern.getDir(direction);
            return LocalizationUtils.format(
                    "univom.tooltip.structure_wand.0",
                    dirs[0].name(),
                    dirs[1].name(),
                    dirs[2].name());
        }).setTextColor(TEXT_COLOR);
    }

    private ButtonWidget createExportButton(HeldItemUIFactory.HeldItemHolder holder) {
        return new ButtonWidget(
                9, 68, 158, 20,
                new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("univom.tooltip.structure_wand.1")),
                clickData -> exportLog(holder));
    }

    private ButtonWidget createChangeDirXButton(HeldItemUIFactory.HeldItemHolder holder) {
        return new ButtonWidget(
                9, 91, 77, 20,
                new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("univom.tooltip.structure_wand.3")),
                clickData -> changeDir(holder, Direction.Axis.X));
    }

    private ButtonWidget createChangeDirYButton(HeldItemUIFactory.HeldItemHolder holder) {
        return new ButtonWidget(
                90, 91, 77, 20,
                new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("univom.tooltip.structure_wand.4")),
                clickData -> changeDir(holder, Direction.Axis.Y));
    }

    private void exportLog(HeldItemUIFactory.HeldItemHolder playerInventoryHolder) {
        ItemStack heldItem = playerInventoryHolder.getHeld();
        BlockPos[] blockPos = getPos(heldItem);
        Player player = playerInventoryHolder.getPlayer();

        if (blockPos == null || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Direction direction = getDir(heldItem);
        DebugBlockPattern blockPattern = new DebugBlockPattern(
                player.level(),
                blockPos[0].getX(), blockPos[0].getY(), blockPos[0].getZ(),
                blockPos[1].getX(), blockPos[1].getY(), blockPos[1].getZ());

        RelativeDirection[] dirs = DebugBlockPattern.getDir(direction);
        blockPattern.changeDir(dirs[0], dirs[1], dirs[2]);

        serverPlayer.displayClientMessage(Component.translatable("univom.tooltip.structure_wand.2"), false);

        StringBuilder builder = new StringBuilder();
        for (String[] strings : blockPattern.getPattern()) {
            builder.append(String.format(".aisle(\"%s\")%n", Joiner.on("\", \"").join(strings)));
        }

        GTCEu.LOGGER.info("\n{}", builder);
    }

    private void changeDir(HeldItemUIFactory.HeldItemHolder playerInventoryHolder, Direction.Axis axis) {
        ItemStack heldItem = playerInventoryHolder.getHeld();
        if (getPos(heldItem) != null && playerInventoryHolder.getPlayer() instanceof ServerPlayer) {
            Direction direction = getDir(heldItem);
            setDir(heldItem, direction.getClockWise(axis));
        }
    }

    public static boolean isEnabled() {
        return ConfigHolder.INSTANCE.devModeSetting.devMode;
    }

    public static boolean isItemStructureWriter(ItemStack stack) {
        return !stack.isEmpty() &&
                stack.getItem() instanceof ComponentItem item &&
                item.getComponents().contains(INSTANCE);
    }

    public static Direction getDir(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTagElement(structure_wand_TAG);
        return tag.contains(DIR_TAG) ?
                Objects.requireNonNull(Direction.byName(tag.getString(DIR_TAG))) :
                Direction.WEST;
    }

    public static void setDir(ItemStack stack, Direction dir) {
        CompoundTag tag = stack.getOrCreateTagElement(structure_wand_TAG);
        tag.putString(DIR_TAG, dir.getName());
    }

    @Nullable
    public static BlockPos[] getPos(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTagElement(structure_wand_TAG);
        if (!tag.contains(MIN_X_TAG)) return null;

        return new BlockPos[] {
                new BlockPos(tag.getInt(MIN_X_TAG), tag.getInt(MIN_Y_TAG), tag.getInt(MIN_Z_TAG)),
                new BlockPos(tag.getInt(MAX_X_TAG), tag.getInt(MAX_Y_TAG), tag.getInt(MAX_Z_TAG))
        };
    }

    public static void addPos(ItemStack stack, BlockPos pos) {
        CompoundTag tag = stack.getOrCreateTagElement(structure_wand_TAG);
        updateMinMax(tag, MIN_X_TAG, MAX_X_TAG, pos.getX());
        updateMinMax(tag, MIN_Y_TAG, MAX_Y_TAG, pos.getY());
        updateMinMax(tag, MIN_Z_TAG, MAX_Z_TAG, pos.getZ());
    }

    private static void updateMinMax(CompoundTag tag, String minKey, String maxKey, int value) {
        if (!tag.contains(minKey) || tag.getInt(minKey) > value) {
            tag.putInt(minKey, value);
        }
        if (!tag.contains(maxKey) || tag.getInt(maxKey) < value) {
            tag.putInt(maxKey, value);
        }
    }

    public static void removePos(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTagElement(structure_wand_TAG);
        tag.remove(MIN_X_TAG);
        tag.remove(MAX_X_TAG);
        tag.remove(MIN_Y_TAG);
        tag.remove(MAX_Y_TAG);
        tag.remove(MIN_Z_TAG);
        tag.remove(MAX_Z_TAG);
    }

    @Override
    public void fillItemCategory(Item item, CreativeModeTab category, NonNullList<ItemStack> items) {
        if (isEnabled()) {
            items.add(new ItemStack(item));
        }
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack itemStack, UseOnContext context) {
        if (!isEnabled()) return InteractionResult.PASS;

        Player player = context.getPlayer();
        if (player == null) return InteractionResult.SUCCESS;

        ItemStack stack = player.getItemInHand(context.getHand());
        if (player.isShiftKeyDown()) {
            removePos(stack);
        } else {
            addPos(stack, context.getClickedPos());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Item item, Level level, Player player,
                                                           InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!isEnabled()) {
            return InteractionResultHolder.pass(stack);
        }
        if (player.isShiftKeyDown()) {
            removePos(stack);
        } else if (player instanceof ServerPlayer serverPlayer) {
            HeldItemUIFactory.INSTANCE.openUI(serverPlayer, usedHand);
        }
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
    }
}
