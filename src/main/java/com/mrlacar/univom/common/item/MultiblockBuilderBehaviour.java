package com.mrlacar.univom.common.item;

import com.mrlacar.univom.api.machine.builder.BuilderModuleCategory;
import com.mrlacar.univom.api.machine.builder.BuilderModules;
import com.mrlacar.univom.api.machine.builder.BuilderSettings;
import com.mrlacar.univom.api.machine.builder.CombinedMaterialSource;
import com.mrlacar.univom.api.machine.builder.MaterialSource;
import com.mrlacar.univom.api.machine.builder.PlayerMaterialSource;
import com.mrlacar.univom.api.machine.builder.StructureBuilder;
import com.mrlacar.univom.api.machine.builder.StructureDismantler;
import com.mrlacar.univom.integration.ae2.MEMaterialSource;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SwitchWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class MultiblockBuilderBehaviour implements IItemUIFactory {

    public static final MultiblockBuilderBehaviour INSTANCE = new MultiblockBuilderBehaviour();

    private static final int UI_WIDTH = 176;
    private static final int UI_HEIGHT = 200;
    private static final int TEXT_COLOR = 0xFAF9F6;
    private static final int PAGE_X = 16;
    private static final int PAGE_Y = 24;
    private static final int PAGE_WIDTH = 144;
    private static final int PAGE_HEIGHT = 136;
    private static final int ICON_SIZE = 22;
    private static final int ICONS_PER_ROW = 5;
    private static final float ICON_SCALE = 0.8f;
    private static final int DISPLAY_INSET = 4;
    private static final int SWITCH_Y = 54;
    private static final int SWITCH_PITCH = 22;
    private static final int SWITCH_LABEL_OFFSET = 6;
    private static final int SWITCH_SIZE = 18;
    private static final int BUTTON_WIDTH = 162;
    private static final int BUTTON_HEIGHT = 18;
    private static final int LABEL_HEIGHT = 9;
    private static final int UNBIND_BUTTON_Y = 148;
    private static final int MODULES_BUTTON_Y = UNBIND_BUTTON_Y + BUTTON_HEIGHT + 4;
    private static final int AE_STATUS_Y = SWITCH_Y + SWITCH_PITCH * 2 + SWITCH_SIZE +
            (UNBIND_BUTTON_Y - (SWITCH_Y + SWITCH_PITCH * 2 + SWITCH_SIZE) - LABEL_HEIGHT) / 2;
    private static final int AE_LOCATION_Y = AE_STATUS_Y + LABEL_HEIGHT + 2;
    private static final int MODULE_ICON_GAP = 4;
    private static final int BLOCK_ICON_GAP = 2;
    private static final int LIST_WIDTH = PAGE_WIDTH - 16;

    private static int gridOriginX(int containerWidth, int gap) {
        return (containerWidth - (ICONS_PER_ROW * ICON_SIZE + (ICONS_PER_ROW - 1) * gap)) / 2;
    }

    @Override
    public ModularUI createUI(HeldItemUIFactory.HeldItemHolder holder, Player entityPlayer) {
        List<WidgetGroup> overlays = new ArrayList<>();
        WidgetGroup layersPage = createLayersPage(holder, overlays);
        WidgetGroup modulePage = createOverlay(PAGE_HEIGHT);
        List<WidgetGroup> categoryPages = new ArrayList<>();
        for (BuilderModuleCategory category : BuilderModules.ALL) {
            categoryPages.add(createCategoryPage(holder, category, overlays, modulePage));
        }
        fillModulePage(modulePage, categoryPages, overlays);

        overlays.add(layersPage);
        overlays.add(modulePage);
        overlays.addAll(categoryPages);

        WidgetGroup root = new WidgetGroup(0, 0, UI_WIDTH, UI_HEIGHT);
        root.addWidget(new ImageWidget(DISPLAY_INSET, DISPLAY_INSET, UI_WIDTH - DISPLAY_INSET * 2,
                UI_HEIGHT - DISPLAY_INSET * 2, GuiTextures.DISPLAY));
        root.addWidget(createMainPage(holder, layersPage, modulePage, overlays));
        root.addWidget(layersPage);
        root.addWidget(modulePage);
        categoryPages.forEach(root::addWidget);

        return new ModularUI(UI_WIDTH, UI_HEIGHT, holder, entityPlayer)
                .background(GuiTextures.BACKGROUND_INVERSE)
                .widget(root);
    }

    private WidgetGroup createOverlay(int height) {
        WidgetGroup overlay = new WidgetGroup(PAGE_X, PAGE_Y, PAGE_WIDTH, height);
        overlay.setBackground(GuiTextures.BACKGROUND_INVERSE);
        overlay.addWidget(new ImageWidget(DISPLAY_INSET, DISPLAY_INSET, PAGE_WIDTH - DISPLAY_INSET * 2,
                height - DISPLAY_INSET * 2, GuiTextures.DISPLAY));
        overlay.setVisible(false);
        return overlay;
    }

    private WidgetGroup createMainPage(HeldItemUIFactory.HeldItemHolder holder, WidgetGroup layersPage,
                                       WidgetGroup modulePage, List<WidgetGroup> overlays) {
        WidgetGroup main = new WidgetGroup(0, 0, UI_WIDTH, UI_HEIGHT);
        main.addWidget(new LabelWidget(7, 7, "univom.builder.title").setTextColor(TEXT_COLOR));

        main.addWidget(new ButtonWidget(7, 20, BUTTON_WIDTH, BUTTON_HEIGHT,
                new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("univom.builder.layers")),
                clickData -> {
                    if (clickData.isRemote) {
                        showOverlay(overlays, layersPage);
                    }
                }));
        main.addWidget(new LabelWidget(7, 42,
                () -> LocalizationUtils.format("univom.builder.layers.current",
                        BuilderSettings.getLayers(stackOf(holder)))).setTextColor(TEXT_COLOR));

        main.addWidget(modeSwitch(holder, 7, SWITCH_Y,
                () -> BuilderSettings.isDismantleMode(stackOf(holder)), BuilderSettings::setDismantleMode));
        main.addWidget(new LabelWidget(31, SWITCH_Y + SWITCH_LABEL_OFFSET, "univom.builder.dismantle")
                .setTextColor(TEXT_COLOR));

        main.addWidget(modeSwitch(holder, 7, SWITCH_Y + SWITCH_PITCH,
                () -> BuilderSettings.isPlaceParts(stackOf(holder)), BuilderSettings::setPlaceParts));
        main.addWidget(new LabelWidget(31, SWITCH_Y + SWITCH_PITCH + SWITCH_LABEL_OFFSET,
                "univom.builder.place_parts").setTextColor(TEXT_COLOR));

        main.addWidget(modeSwitch(holder, 7, SWITCH_Y + SWITCH_PITCH * 2,
                () -> BuilderSettings.isUseNetwork(stackOf(holder)), BuilderSettings::setUseNetwork));
        main.addWidget(new LabelWidget(31, SWITCH_Y + SWITCH_PITCH * 2 + SWITCH_LABEL_OFFSET,
                "univom.builder.ae.inventory").setTextColor(TEXT_COLOR));

        main.addWidget(new LabelWidget(7, AE_STATUS_Y, () -> LocalizationUtils.format("univom.builder.ae",
                LocalizationUtils.format(BuilderSettings.hasNetwork(stackOf(holder)) ?
                        "univom.builder.ae.on" : "univom.builder.ae.off"))).setTextColor(TEXT_COLOR));
        main.addWidget(new LabelWidget(7, AE_LOCATION_Y, () -> {
            CompoundTag network = BuilderSettings.getNetworkTag(stackOf(holder));
            return network == null ? "" : LocalizationUtils.format("univom.builder.ae.location",
                    BuilderSettings.networkDimension(network), BuilderSettings.networkX(network),
                    BuilderSettings.networkY(network), BuilderSettings.networkZ(network));
        }).setTextColor(TEXT_COLOR));

        main.addWidget(new ButtonWidget(7, UNBIND_BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT,
                new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("univom.builder.ae.unbind")),
                clickData -> BuilderSettings.clearNetwork(stackOf(holder))));

        main.addWidget(new ButtonWidget(7, MODULES_BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT,
                new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("univom.builder.modules")),
                clickData -> {
                    if (clickData.isRemote) {
                        showOverlay(overlays, modulePage);
                    }
                }));
        return main;
    }

    private WidgetGroup createLayersPage(HeldItemUIFactory.HeldItemHolder holder, List<WidgetGroup> overlays) {
        WidgetGroup page = createOverlay(56);
        page.addWidget(new LabelWidget(8, 8, "univom.builder.layers").setTextColor(TEXT_COLOR));
        page.addWidget(new TextFieldWidget(8, 24, PAGE_WIDTH - 16, 16,
                () -> String.valueOf(BuilderSettings.getLayers(stackOf(holder))),
                value -> setLayers(holder, value))
                .setNumbersOnly(BuilderSettings.MIN_LAYERS, BuilderSettings.MAX_LAYERS));
        page.addWidget(closeButton(overlays, null));
        return page;
    }

    private void fillModulePage(WidgetGroup modulePage, List<WidgetGroup> categoryPages,
                                List<WidgetGroup> overlays) {
        modulePage.addWidget(new LabelWidget(8, 8, "univom.builder.modules").setTextColor(TEXT_COLOR));
        List<BuilderModuleCategory> categories = BuilderModules.ALL;
        for (int i = 0; i < categories.size(); i++) {
            BuilderModuleCategory category = categories.get(i);
            WidgetGroup categoryPage = categoryPages.get(i);
            int x = gridOriginX(PAGE_WIDTH, MODULE_ICON_GAP) + (i % ICONS_PER_ROW) * (ICON_SIZE + MODULE_ICON_GAP);
            int y = 24 + (i / ICONS_PER_ROW) * (ICON_SIZE + MODULE_ICON_GAP);
            modulePage.addWidget(new ButtonWidget(x, y, ICON_SIZE, ICON_SIZE,
                    new GuiTextureGroup(GuiTextures.BUTTON,
                            new ItemStackTexture(new ItemStack(category.defaultBlock())).scale(ICON_SCALE)),
                    clickData -> {
                        if (clickData.isRemote) {
                            showOverlay(overlays, categoryPage);
                        }
                    }).setHoverTooltips(Component.translatable(category.translationKey())));
        }
        modulePage.addWidget(closeButton(overlays, null));
    }

    private WidgetGroup createCategoryPage(HeldItemUIFactory.HeldItemHolder holder, BuilderModuleCategory category,
                                           List<WidgetGroup> overlays, WidgetGroup modulePage) {
        WidgetGroup page = createOverlay(PAGE_HEIGHT);
        page.addWidget(new LabelWidget(8, 8, category.translationKey()).setTextColor(TEXT_COLOR));

        DraggableScrollableWidgetGroup list = new DraggableScrollableWidgetGroup(8, 22, LIST_WIDTH, 70);
        list.setYBarStyle(GuiTextures.SLIDER_BACKGROUND, GuiTextures.BUTTON);
        List<Block> blocks = category.blocks();
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            String blockId = BuilderModuleCategory.idOf(block);
            int x = gridOriginX(LIST_WIDTH, BLOCK_ICON_GAP) + (i % ICONS_PER_ROW) * (ICON_SIZE + BLOCK_ICON_GAP);
            int y = (i / ICONS_PER_ROW) * (ICON_SIZE + BLOCK_ICON_GAP);
            list.addWidget(new ButtonWidget(x, y, ICON_SIZE, ICON_SIZE,
                    new GuiTextureGroup(GuiTextures.BUTTON,
                            new ItemStackTexture(new ItemStack(block)).scale(ICON_SCALE)),
                    clickData -> BuilderSettings.setModuleBlock(stackOf(holder), category, block))
                    .setHoverTooltips(block.getName()));
            list.addWidget(new ImageWidget(x, y, ICON_SIZE, ICON_SIZE,
                    () -> blockId.equals(BuilderSettings.getModuleBlockId(stackOf(holder), category)) ?
                            GuiTextures.SELECT_BOX : IGuiTexture.EMPTY));
        }
        page.addWidget(list);

        page.addWidget(modeSwitch(holder, 8, 100,
                () -> BuilderSettings.isModuleReplace(stackOf(holder), category),
                (stack, pressed) -> BuilderSettings.setModuleReplace(stack, category, pressed)));
        page.addWidget(new LabelWidget(32, 106, "univom.builder.replace").setTextColor(TEXT_COLOR));
        page.addWidget(closeButton(overlays, modulePage));
        return page;
    }

    private SwitchWidget modeSwitch(HeldItemUIFactory.HeldItemHolder holder, int x, int y,
                                    Supplier<Boolean> supplier, BiConsumer<ItemStack, Boolean> setter) {
        return new SwitchWidget(x, y, SWITCH_SIZE, SWITCH_SIZE,
                (clickData, pressed) -> setter.accept(stackOf(holder), pressed)).setSupplier(supplier)
                .setTexture(
                        new GuiTextureGroup(GuiTextures.BUTTON, GuiTextures.PROGRESS_BAR_SOLAR_STEAM.get(true)
                                .copy().getSubTexture(0, 0, 1, 0.5).scale(0.8f)),
                        new GuiTextureGroup(GuiTextures.BUTTON,
                                GuiTextures.PROGRESS_BAR_SOLAR_STEAM.get(true).copy()
                                        .getSubTexture(0, 0.5, 1, 0.5).scale(0.8f)));
    }

    private ButtonWidget closeButton(List<WidgetGroup> overlays) {
        return closeButton(overlays, null);
    }

    private ButtonWidget closeButton(List<WidgetGroup> overlays, @Nullable WidgetGroup back) {
        return new ButtonWidget(PAGE_WIDTH - 20, 4, 16, 16,
                new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("univom.builder.close")),
                clickData -> {
                    if (clickData.isRemote) {
                        showOverlay(overlays, back);
                    }
                });
    }

    private static void setLayers(HeldItemUIFactory.HeldItemHolder holder, String value) {
        try {
            BuilderSettings.setLayers(stackOf(holder), Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
        }
    }

    private static void showOverlay(List<WidgetGroup> overlays, WidgetGroup target) {
        for (WidgetGroup overlay : overlays) {
            overlay.setVisible(overlay == target);
        }
    }

    private static ItemStack stackOf(HeldItemUIFactory.HeldItemHolder holder) {
        return holder.getPlayer().getItemInHand(holder.getHand());
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Item item, Level level, Player player,
                                                           InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            HeldItemUIFactory.INSTANCE.openUI(serverPlayer, usedHand);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack terminal = player.getItemInHand(context.getHand());

        if (MetaMachine.getMachine(level, pos) instanceof IMultiController controller) {
            boolean dismantle = BuilderSettings.isDismantleMode(terminal);
            if (dismantle != controller.isFormed()) {
                return InteractionResult.PASS;
            }
            if (player instanceof ServerPlayer serverPlayer) {
                if (dismantle) {
                    runDismantle(serverPlayer, terminal, controller);
                } else {
                    runBuild(serverPlayer, terminal, controller);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private static MaterialSource sourceFor(ServerPlayer player, ItemStack terminal) {
        MaterialSource inventory = new PlayerMaterialSource(player);
        if (!BuilderSettings.isUseNetwork(terminal)) {
            return inventory;
        }
        GlobalPos network = BuilderSettings.getNetwork(terminal);
        if (network == null) {
            return inventory;
        }
        MEMaterialSource meSource = MEMaterialSource.resolve(player, network);
        if (meSource == null) {
            player.displayClientMessage(Component.translatable("univom.builder.ae.unreachable")
                    .withStyle(ChatFormatting.RED), true);
            return inventory;
        }
        return new CombinedMaterialSource(meSource, inventory);
    }

    private static void runBuild(ServerPlayer player, ItemStack terminal, IMultiController controller) {
        if (StructureBuilder.build(player, terminal, controller,
                sourceFor(player, terminal)) == StructureBuilder.BuildResult.TOO_LARGE) {
            player.displayClientMessage(Component.translatable("univom.builder.too_large",
                    BuilderSettings.MAX_BLOCKS_PER_OPERATION).withStyle(ChatFormatting.RED), true);
        }
    }

    private static void runDismantle(ServerPlayer player, ItemStack terminal, IMultiController controller) {
        switch (StructureDismantler.dismantle(player, controller, sourceFor(player, terminal))) {
            case TOO_LARGE -> player.displayClientMessage(Component.translatable("univom.builder.too_large",
                    BuilderSettings.MAX_BLOCKS_PER_OPERATION).withStyle(ChatFormatting.RED), true);
            case NO_PERMISSION -> player.displayClientMessage(
                    Component.translatable("univom.builder.no_permission").withStyle(ChatFormatting.RED), true);
            case PART_NOT_EMPTY -> player.displayClientMessage(
                    Component.translatable("univom.builder.not_empty").withStyle(ChatFormatting.RED), true);
            default -> {}
        }
    }
}
