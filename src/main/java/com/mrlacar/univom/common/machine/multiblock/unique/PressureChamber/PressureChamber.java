package com.mrlacar.univom.common.machine.multiblock.unique.PressureChamber;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;
import com.gregtechceu.gtceu.api.gui.fancy.TooltipsPanel;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.IVoidable;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDisplayUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pipenet.Node;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.mrlacar.univom.api.machine.multiblock.ModPartAbility;
import com.mrlacar.univom.common.data.ModBlocks;
import com.mrlacar.univom.common.machine.multiblock.part.PressurePortPartMachine;
import com.mrlacar.univom.common.pipelike.pressure.LevelPressurePipeNet;
import com.mrlacar.univom.common.pipelike.pressure.PressurePipeNet;
import com.mrlacar.univom.common.pipelike.pressure.PressurePipeProperties;
import com.mrlacar.univom.common.pipelike.pressure.PressureProcessing;
import com.mrlacar.univom.common.recipe.condition.PressureCondition;
import com.mrlacar.univom.common.recipe.condition.PressureGasCondition;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReferenceArray;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressureChamber extends WorkableMultiblockMachine implements IFancyUIMachine, IDisplayUIMachine,
                             IMachineLife {

    public static final int VOLUME_PER_BLOCK = 500;
    public static final float BURST_FACTOR = 1.6F;
    private static final int LINK_INTERVAL = 20;
    private static final AtomicReferenceArray<BlockPattern> PATTERNS = new AtomicReferenceArray<>(
            PressureChamberLayout.COUNT);

    private int interior;
    @Nullable
    private PressurePortPartMachine port;
    @Nullable
    private BlockPos endpoint;
    @Nullable
    private BlockPos node;
    private WeakReference<PressurePipeNet> linked = new WeakReference<>(null);
    @Nullable
    private TickableSubscription linkSubs;

    public PressureChamber(IMachineBlockEntity holder) {
        super(holder);
    }

    public static ModifierFunction recipeModifier(MetaMachine machine, GTRecipe recipe) {
        if (!(machine instanceof PressureChamber chamber)) {
            return RecipeModifier.nullWrongType(PressureChamber.class, machine);
        }
        if (!chamber.pressureConditionsMet(recipe)) {
            return ModifierFunction.IDENTITY;
        }
        int allowed = chamber.allowedParallel(chamber.getLinkedNet(), recipe);
        if (allowed <= 0) {
            return ModifierFunction.NULL;
        }
        int parallel = ParallelLogic.getParallelAmount(machine, recipe, allowed);
        if (parallel <= 1) {
            return ModifierFunction.IDENTITY;
        }
        return ModifierFunction.builder()
                .inputModifier(ContentModifier.multiplier(parallel))
                .outputModifier(ContentModifier.multiplier(parallel))
                .parallels(parallel)
                .build();
    }

    public static BlockPattern defaultPattern(MultiblockMachineDefinition definition) {
        return pattern(definition, PressureChamberLayout.DEFAULT);
    }

    public static BlockPattern pattern(MultiblockMachineDefinition definition, int layout) {
        BlockPattern pattern = PATTERNS.get(layout);
        if (pattern == null) {
            pattern = createPattern(definition, layout);
            if (!PATTERNS.compareAndSet(layout, null, pattern)) {
                pattern = PATTERNS.get(layout);
            }
        }
        return pattern;
    }

    public static List<MultiblockShapeInfo> shapeInfos(MultiblockMachineDefinition definition) {
        List<MultiblockShapeInfo> shapes = new ArrayList<>();
        for (int size = 1; size <= PressureChamberLayout.MAX_INTERIOR; size += 2) {
            int center = (size + 1) / 2;
            int[] repetition = new int[size + 2];
            Arrays.fill(repetition, 1);
            shapes.add(new MultiblockShapeInfo(pattern(definition, PressureChamberLayout.encode(size, center, center))
                    .getPreview(repetition)));
        }
        return shapes;
    }

    public static Component gasName(@Nullable ResourceLocation gas) {
        Fluid fluid = gas == null ? null : ForgeRegistries.FLUIDS.getValue(gas);
        return fluid == null || fluid == Fluids.EMPTY ?
                Component.translatable("univom.machine.pressure_chamber.no_gas") :
                fluid.getFluidType().getDescription();
    }

    @Override
    public BlockPattern getPattern() {
        return pattern(getDefinition(), currentLayout());
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        interior = PressureChamberLayout.size(currentLayout());
        port = null;
        for (IMultiPart part : getParts()) {
            if (part instanceof PressurePortPartMachine found) {
                port = found;
                break;
            }
        }
        relink();
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        unlink();
    }

    @Override
    public void onMachineRemoved() {
        unlink();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        linked.clear();
        linkSubs = null;
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        if (recipe == null) {
            return super.beforeWorking(null);
        }
        PressurePipeNet net = getLinkedNet();
        int allowed = allowedParallel(net, recipe);
        if (allowed <= 0 || recipe.parallels > allowed) {
            recipeLogic.markLastRecipeDirty();
            return false;
        }
        if (!super.beforeWorking(recipe)) {
            return false;
        }
        if (net != null) {
            net.extract(PressureProcessing.batchLoss(net.getAmount(), net.getVolume(), recipe.parallels,
                    gasConsumption(recipe)));
        }
        return true;
    }

    @Nullable
    public PressurePipeNet getLinkedNet() {
        BlockPos endpoint = this.endpoint;
        BlockPos node = this.node;
        if (endpoint == null || node == null) {
            return null;
        }
        PressurePipeNet net = linked.get();
        if (net != null && net.isValid() && net.isAttachedVia(endpoint, node)) {
            return net;
        }
        if (!(getLevel() instanceof ServerLevel level)) {
            return null;
        }
        net = LevelPressurePipeNet.getOrCreate(level).getNetFromPos(node);
        if (net != null && net.isAttachedVia(endpoint, node)) {
            linked = new WeakReference<>(net);
            return net;
        }
        linked.clear();
        updateLinkSubscription();
        return null;
    }

    public double getPressure() {
        PressurePipeNet net = getLinkedNet();
        return net == null ? 0.0D : net.getPressure();
    }

    @Nullable
    public ResourceLocation getGas() {
        PressurePipeNet net = getLinkedNet();
        return net == null ? null : net.getGas();
    }

    public int getChamberVolume() {
        return interior * interior * interior * VOLUME_PER_BLOCK;
    }

    public int getRating() {
        return interior <= 0 ? 0 : PressureChamberLayout.rating(interior);
    }

    public int getActiveParallel() {
        GTRecipe recipe = recipeLogic.getLastRecipe();
        return recipeLogic.isActive() && recipe != null ? recipe.parallels : 0;
    }

    public PressureStatus getPressureStatus() {
        PressurePipeNet net = getLinkedNet();
        if (net == null) {
            return PressureStatus.NO_NETWORK;
        }
        GTRecipe recipe = pendingRecipe();
        if (recipe == null) {
            return PressureStatus.READY;
        }
        for (RecipeCondition<?> condition : recipe.conditions) {
            if (condition instanceof PressureGasCondition && !condition.check(recipe, recipeLogic)) {
                return PressureStatus.WRONG_GAS;
            }
        }
        for (RecipeCondition<?> condition : recipe.conditions) {
            if (condition instanceof PressureCondition && !condition.check(recipe, recipeLogic)) {
                return PressureStatus.LOW_PRESSURE;
            }
        }
        if (recipeLogic.isIdle() && allowedParallel(net, recipe) <= 0) {
            return PressureStatus.LOW_GAS;
        }
        return PressureStatus.READY;
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        int parallels = getActiveParallel();
        MultiblockDisplayText.builder(textList, isFormed())
                .setWorkingStatus(recipeLogic.isWorkingEnabled(), recipeLogic.isActive())
                .addCustom(this::addPressureText)
                .addParallelsLine(parallels, parallels > 0)
                .addWorkingStatusLine()
                .addProgressLine(recipeLogic)
                .addRecipeFailReasonLine(recipeLogic)
                .addOutputLines(recipeLogic.getLastRecipe());
        getDefinition().getAdditionalDisplay().accept(this, textList);
        IDisplayUIMachine.super.addDisplayText(textList);
    }

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 182 + 8, 117 + 8);
        group.addWidget(new DraggableScrollableWidgetGroup(4, 4, 182, 117).setBackground(getScreenTexture())
                .addWidget(new LabelWidget(4, 5, self().getBlockState().getBlock().getDescriptionId()))
                .addWidget(new ComponentPanelWidget(4, 17, this::addDisplayText)
                        .textSupplier(this.getLevel().isClientSide ? null : this::addDisplayText)
                        .setMaxWidthLimit(200)
                        .clickHandler(this::handleDisplayClick)));
        group.setBackground(GuiTextures.BACKGROUND_INVERSE);
        return group;
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        return new ModularUI(198, 208, this, entityPlayer).widget(new FancyMachineUIWidget(this, 198, 208));
    }

    @Override
    public List<IFancyUIProvider> getSubTabs() {
        List<IFancyUIProvider> tabs = new ArrayList<>();
        for (IMultiPart part : getParts()) {
            if (part != null && !(part instanceof PressurePortPartMachine)) {
                tabs.add(part);
            }
        }
        return tabs;
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        IVoidable.attachConfigurators(configuratorPanel, this);
        IFancyUIMachine.super.attachConfigurators(configuratorPanel);
    }

    @Override
    public void attachTooltips(TooltipsPanel tooltipsPanel) {
        for (IMultiPart part : getParts()) {
            part.attachFancyTooltipsToController(this, tooltipsPanel);
        }
    }

    private int currentLayout() {
        Level level = getLevel();
        Direction front = getFrontFacing();
        int layout = level == null ? PressureChamberLayout.INVALID :
                PressureChamberLayout.scan(getPos(), front, pos -> level.isLoaded(pos) && level.isEmptyBlock(pos));
        return layout == PressureChamberLayout.INVALID ? PressureChamberLayout.DEFAULT : layout;
    }

    private void addPressureText(List<Component> textList) {
        if (!isFormed()) {
            return;
        }
        textList.add(Component.translatable("univom.machine.pressure_chamber.size", interior, interior, interior,
                FormattingUtil.formatNumbers(getChamberVolume())).withStyle(ChatFormatting.GRAY));
        textList.add(Component.translatable("univom.machine.pressure_chamber.rating", getRating()));
        PressurePipeNet net = getLinkedNet();
        if (net != null) {
            textList.add(Component.translatable("univom.machine.pressure_chamber.pressure",
                    String.format(Locale.ROOT, "%.1f", net.getPressure()), net.getMinRating()));
            textList.add(Component.translatable("univom.machine.pressure_chamber.gas", gasName(net.getGas())));
        }
        PressureStatus status = getPressureStatus();
        if (status != PressureStatus.READY) {
            textList.add(Component.translatable(status.getTranslationKey()).withStyle(status.getColor()));
        }
    }

    @Nullable
    private GTRecipe pendingRecipe() {
        if (!recipeLogic.isIdle()) {
            return recipeLogic.getLastRecipe();
        }
        List<GTRecipe> failed = recipeLogic.lastFailedMatches;
        return failed == null || failed.isEmpty() ? null : failed.get(0);
    }

    private boolean pressureConditionsMet(GTRecipe recipe) {
        for (RecipeCondition<?> condition : recipe.conditions) {
            if ((condition instanceof PressureCondition || condition instanceof PressureGasCondition) &&
                    !condition.check(recipe, recipeLogic)) {
                return false;
            }
        }
        return true;
    }

    private int allowedParallel(@Nullable PressurePipeNet net, GTRecipe recipe) {
        return PressureProcessing.maxParallel(interior * interior * interior, getRating(),
                net == null ? 0 : net.getAmount(), net == null ? 0 : net.getVolume(), requiredPressure(recipe),
                gasConsumption(recipe));
    }

    private void relink() {
        if (!(getLevel() instanceof ServerLevel level)) {
            return;
        }
        LevelPressurePipeNet world = LevelPressurePipeNet.getOrCreate(level);
        PressurePortPartMachine current = isFormed() ? port : null;
        BlockPos newEndpoint = current == null ? null : current.getPos();
        BlockPos newNode = current == null ? null : current.getNodePos();
        if (endpoint != null && node != null && !(endpoint.equals(newEndpoint) && node.equals(newNode))) {
            PressurePipeNet old = world.getNetFromPos(node);
            if (old != null && old.isAttachedVia(endpoint, node)) {
                old.detach(endpoint);
            }
        }
        endpoint = newEndpoint;
        node = newNode;
        linked.clear();
        if (current != null) {
            PressurePipeNet net = world.getNetFromPos(newNode);
            Node<PressurePipeProperties> pipe = net == null ? null : net.getNodeAt(newNode);
            if (pipe != null && !pipe.isBlocked(current.getFrontFacing().getOpposite())) {
                net.attach(newEndpoint, newNode, getRating(), getChamberVolume(), BURST_FACTOR);
                linked = new WeakReference<>(net);
            } else if (net != null && net.isAttachedVia(newEndpoint, newNode)) {
                net.detach(newEndpoint);
            }
        }
        updateLinkSubscription();
    }

    private void unlink() {
        if (getLevel() instanceof ServerLevel level && endpoint != null && node != null) {
            PressurePipeNet net = LevelPressurePipeNet.getOrCreate(level).getNetFromPos(node);
            if (net != null && net.isAttachedVia(endpoint, node)) {
                net.detach(endpoint);
            }
        }
        endpoint = null;
        node = null;
        port = null;
        interior = 0;
        linked.clear();
        unsubscribe(linkSubs);
        linkSubs = null;
    }

    private void updateLinkSubscription() {
        if (isFormed() && port != null && linked.get() == null) {
            linkSubs = subscribeServerTick(linkSubs, this::pollLink);
        } else if (linkSubs != null) {
            unsubscribe(linkSubs);
            linkSubs = null;
        }
    }

    private void pollLink() {
        if (!isFormed()) {
            updateLinkSubscription();
        } else if (getOffsetTimer() % LINK_INTERVAL == 0) {
            relink();
        }
    }

    private static int requiredPressure(GTRecipe recipe) {
        int required = 0;
        for (RecipeCondition<?> condition : recipe.conditions) {
            if (condition instanceof PressureCondition pressure && !pressure.isReverse()) {
                required = Math.max(required, pressure.getPressure());
            }
        }
        return required;
    }

    private static long gasConsumption(GTRecipe recipe) {
        long consumption = 0;
        for (RecipeCondition<?> condition : recipe.conditions) {
            if (condition instanceof PressureGasCondition gas && !gas.isReverse()) {
                consumption += gas.getConsumption();
            }
        }
        return consumption;
    }

    private static BlockPattern createPattern(MultiblockMachineDefinition definition, int layout) {
        FactoryBlockPattern builder = FactoryBlockPattern.start();
        for (String[] aisle : PressureChamberLayout.aisles(layout)) {
            builder.aisle(aisle);
        }
        return builder
                .where(PressureChamberLayout.CONTROLLER, Predicates.controller(Predicates.blocks(definition.getBlock())))
                .where(PressureChamberLayout.SHELL, Predicates.blocks(ModBlocks.CASING_PRESSURE_CHAMBER.get())
                        .setMinGlobalLimited(PressureChamberLayout.casings(layout))
                        .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                        .or(Predicates.abilities(ModPartAbility.PRESSURE_PORT).setExactLimit(1)))
                .where(PressureChamberLayout.INTERIOR, Predicates.air())
                .build();
    }

    public enum PressureStatus {

        READY(ChatFormatting.GREEN),
        NO_NETWORK(ChatFormatting.RED),
        LOW_PRESSURE(ChatFormatting.RED),
        WRONG_GAS(ChatFormatting.RED),
        LOW_GAS(ChatFormatting.RED);

        private static final PressureStatus[] VALUES = values();

        private final ChatFormatting color;
        private final String translationKey;

        PressureStatus(ChatFormatting color) {
            this.color = color;
            this.translationKey = "univom.machine.pressure_chamber.status." + name().toLowerCase(Locale.ROOT);
        }

        public ChatFormatting getColor() {
            return color;
        }

        public String getTranslationKey() {
            return translationKey;
        }

        public static PressureStatus byOrdinal(int ordinal) {
            return VALUES[Math.max(0, Math.min(ordinal, VALUES.length - 1))];
        }
    }
}
