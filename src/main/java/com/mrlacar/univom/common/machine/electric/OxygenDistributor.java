package com.mrlacar.univom.common.machine.electric;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.ButtonConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.ITieredMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.mrlacar.univom.world.client.ModuleHighlightManager;
import earth.terrarium.adastra.api.systems.OxygenApi;
import earth.terrarium.adastra.common.utils.floodfill.FloodFill3D;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class OxygenDistributor extends SimpleTieredMachine {

    public static final long BASE_EUT = GTValues.VA[GTValues.MV];
    public static final int BASE_OXYGEN = 1;
    public static final Int2IntFunction TANK_CAPACITY = tier -> 64_000 << (tier - GTValues.HV);

    private static final int REFRESH_INTERVAL = 100;
    private static final int REASSERT_REFRESHES = 12;
    private static final int SCAN_BUDGET = 2048;
    private static final String CELLS_TAG = "oxygenCells";
    private static final String SEALED_TAG = "oxygenSealed";
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final LongArrayFIFOQueue UNUSED_QUEUE = new LongArrayFIFOQueue(1);
    private static final Component NOT_SEALED = Component.translatable("univom.recipe_modifier.not_sealed");
    private static final long RANGE_DISPLAY_MS = 10_000L;
    private static final int RANGE_COLOR = 0x3399FF;

    private static int budgetTick = -1;
    private static int budgetLeft;
    private static long clearGeneration;

    private final int radius;
    private final int side;
    private final int area;
    private final long[] scanned;
    private final long[] distributed;
    private final IntArrayFIFOQueue frontier = new IntArrayFIFOQueue();
    private final LongOpenHashSet sealingBlocks = new LongOpenHashSet();
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos frontCursor = new BlockPos.MutableBlockPos();
    private boolean scanning;
    private boolean sealed;
    private long scannedAt = -1;
    private long seenGeneration = -1;
    private int refreshesSinceReassert;
    @Nullable
    private TickableSubscription distributionSubs;

    public OxygenDistributor(IMachineBlockEntity holder, int tier) {
        super(holder, tier, TANK_CAPACITY);
        this.radius = rangeOf(tier);
        this.side = radius * 2 + 1;
        this.area = side * side;
        int words = (area * side + 63) >>> 6;
        this.scanned = new long[words];
        this.distributed = new long[words];
    }

    public static int rangeOf(int tier) {
        return 8 << (tier - GTValues.HV);
    }

    public static ModifierFunction recipeModifier(MetaMachine machine, GTRecipe recipe) {
        if (machine instanceof OxygenDistributor distributor && distributor.knownUnsealed()) {
            return ModifierFunction.cancel(NOT_SEALED);
        }
        if (!(machine instanceof ITieredMachine tiered) || tiered.getTier() <= GTValues.HV) {
            return ModifierFunction.IDENTITY;
        }
        int steps = tiered.getTier() - GTValues.HV;
        return ModifierFunction.builder()
                .eutMultiplier(1L << (steps * 2))
                .tickInputModifier(ContentModifier.multiplier(1L << steps))
                .build();
    }

    public static Component[] tooltips(int tier) {
        int steps = tier - GTValues.HV;
        return new Component[] {
                Component.translatable("univom.machine.oxygen_distributor.tooltip.0"),
                Component.translatable("univom.machine.oxygen_distributor.tooltip.1", rangeOf(tier)),
                Component.translatable("univom.machine.oxygen_distributor.tooltip.2",
                        FormattingUtil.formatNumbers(BASE_EUT << (steps * 2)), BASE_OXYGEN << steps),
                Component.translatable("univom.machine.oxygen_distributor.tooltip.3"),
                Component.translatable("univom.machine.oxygen_distributor.tooltip.4")
        };
    }

    @Override
    public boolean isCircuitSlotEnabled() {
        return false;
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        super.attachConfigurators(configuratorPanel);
        configuratorPanel.attachConfigurators(new ButtonConfigurator(
                new GuiTextureGroup(new ItemStackTexture(getDefinition().asStack())), this::showRange)
                .setTooltips(List.of(Component.translatable("behaviour.machine.highlight_oxygen_range"))));
    }

    private void showRange(ClickData clickData) {
        if (clickData.isRemote) {
            ModuleHighlightManager.addHighlight(getPos(), new AABB(getPos()).inflate(radius), RANGE_DISPLAY_MS,
                    RANGE_COLOR);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, this::resumeDistribution));
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        unsubscribe(distributionSubs);
        distributionSubs = null;
        scanning = false;
        frontier.clear();
    }

    @Override
    public void onMachineRemoved() {
        super.onMachineRemoved();
        clearOxygen();
    }

    @Override
    public void notifyStatusChanged(RecipeLogic.Status oldStatus, RecipeLogic.Status newStatus) {
        super.notifyStatusChanged(oldStatus, newStatus);
        if (isRemote()) {
            return;
        }
        if (newStatus == RecipeLogic.Status.WORKING) {
            if (sealed && scanFresh()) {
                apply();
            }
            distributionSubs = subscribeServerTick(distributionSubs, this::tickDistribution);
        } else if (oldStatus == RecipeLogic.Status.WORKING) {
            clearOxygen();
        }
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        if (!frontClear()) {
            return false;
        }
        if (!getRecipeLogic().isWorking() && !scanFresh()) {
            startScan();
            return false;
        }
        return sealed && super.beforeWorking(recipe);
    }

    @Override
    public boolean onWorking() {
        return sealed && frontClear() && super.onWorking();
    }

    @Override
    public void saveCustomPersistedData(@NotNull CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);
        if (forDrop) {
            return;
        }
        tag.putBoolean(SEALED_TAG, sealed);
        for (long word : distributed) {
            if (word != 0L) {
                tag.putLongArray(CELLS_TAG, distributed.clone());
                return;
            }
        }
    }

    @Override
    public void loadCustomPersistedData(@NotNull CompoundTag tag) {
        super.loadCustomPersistedData(tag);
        sealed = tag.getBoolean(SEALED_TAG);
        long[] cells = tag.getLongArray(CELLS_TAG);
        Arrays.fill(distributed, 0L);
        System.arraycopy(cells, 0, distributed, 0, Math.min(cells.length, distributed.length));
    }

    private void resumeDistribution() {
        if (isInValid()) {
            return;
        }
        if (getRecipeLogic().isWorking()) {
            distributionSubs = subscribeServerTick(distributionSubs, this::tickDistribution);
        } else {
            clearOxygen();
        }
    }

    private void tickDistribution() {
        boolean working = getRecipeLogic().isWorking();
        if (working && !scanning && (scannedAt < 0 || getOffsetTimer() - scannedAt >= REFRESH_INTERVAL)) {
            startScan();
        }
        if (scanning) {
            scanStep();
        } else if (!working) {
            unsubscribe(distributionSubs);
            distributionSubs = null;
        }
    }

    private boolean frontClear() {
        Level level = getLevel();
        if (level == null) {
            return false;
        }
        Direction front = getFrontFacing();
        if (getCoverContainer().getCoverAtSide(front) != null) {
            return false;
        }
        frontCursor.setWithOffset(getPos(), front);
        return level.isLoaded(frontCursor) && level.getBlockState(frontCursor).isAir();
    }

    private boolean knownUnsealed() {
        if (sealed || scannedAt < 0) {
            return false;
        }
        if (!isRemote() && !scanFresh()) {
            startScan();
        }
        return true;
    }

    private boolean scanFresh() {
        return !scanning && scannedAt >= 0 && getOffsetTimer() - scannedAt <= REFRESH_INTERVAL;
    }

    private void startScan() {
        if (scanning) {
            return;
        }
        Arrays.fill(scanned, 0L);
        frontier.clear();
        int center = index(0, 0, 0);
        scanned[center >>> 6] |= 1L << center;
        frontier.enqueue(center);
        scanning = true;
        distributionSubs = subscribeServerTick(distributionSubs, this::tickDistribution);
    }

    private void scanStep() {
        if (!(getLevel() instanceof ServerLevel level)) {
            finishScan(false);
            return;
        }
        int tick = level.getServer().getTickCount();
        if (tick != budgetTick) {
            budgetTick = tick;
            budgetLeft = SCAN_BUDGET;
        }
        if (budgetLeft <= 0) {
            return;
        }
        BlockPos origin = getPos();
        int reach = radius + 1;
        if (!level.hasChunksAt(origin.getX() - reach, origin.getY() - reach, origin.getZ() - reach,
                origin.getX() + reach, origin.getY() + reach, origin.getZ() + reach)) {
            finishScan(false);
            return;
        }
        while (!frontier.isEmpty()) {
            if (budgetLeft <= 0) {
                return;
            }
            budgetLeft--;
            int cell = frontier.dequeueInt();
            int x = cell / area - radius;
            int y = cell / side % side - radius;
            int z = cell % side - radius;
            for (Direction direction : DIRECTIONS) {
                int nx = x + direction.getStepX();
                int ny = y + direction.getStepY();
                int nz = z + direction.getStepZ();
                boolean inside = Math.abs(nx) <= radius && Math.abs(ny) <= radius && Math.abs(nz) <= radius;
                int next = inside ? index(nx, ny, nz) : -1;
                if (inside && (scanned[next >>> 6] & 1L << next) != 0L) {
                    continue;
                }
                cursor.set(origin.getX() + nx, origin.getY() + ny, origin.getZ() + nz);
                boolean open = FloodFill3D.TEST_FULL_SEAL.test(level, cursor, level.getBlockState(cursor),
                        sealingBlocks, UNUSED_QUEUE, direction);
                boolean sealing = !sealingBlocks.isEmpty();
                sealingBlocks.clear();
                if (!inside) {
                    if (open) {
                        finishScan(false);
                        return;
                    }
                } else if (open) {
                    scanned[next >>> 6] |= 1L << next;
                    frontier.enqueue(next);
                } else if (sealing) {
                    scanned[next >>> 6] |= 1L << next;
                }
            }
        }
        finishScan(true);
    }

    private void finishScan(boolean result) {
        scanning = false;
        frontier.clear();
        sealed = result;
        scannedAt = getOffsetTimer();
        if (result && getRecipeLogic().isWorking()) {
            apply();
        }
    }

    private void apply() {
        Level level = getLevel();
        if (level == null) {
            return;
        }
        BlockPos origin = getPos();
        boolean reassert = seenGeneration != clearGeneration || ++refreshesSinceReassert >= REASSERT_REFRESHES;
        if (reassert) {
            refreshesSinceReassert = 0;
        }
        boolean changed = false;
        List<BlockPos> supplied = new ObjectArrayList<>();
        List<BlockPos> withdrawn = new ObjectArrayList<>();
        for (int word = 0; word < scanned.length; word++) {
            long current = scanned[word];
            long previous = distributed[word];
            collect(origin, word, reassert ? current : current & ~previous, supplied);
            collect(origin, word, previous & ~current, withdrawn);
            if (current != previous) {
                distributed[word] = current;
                changed = true;
            }
        }
        if (!withdrawn.isEmpty()) {
            OxygenApi.API.removeOxygen(level, withdrawn);
            clearGeneration++;
        }
        if (!supplied.isEmpty()) {
            OxygenApi.API.setOxygen(level, supplied, true);
        }
        seenGeneration = clearGeneration;
        if (changed) {
            markDirty();
        }
    }

    private void clearOxygen() {
        Level level = getLevel();
        if (level == null) {
            return;
        }
        BlockPos origin = getPos();
        List<BlockPos> withdrawn = new ObjectArrayList<>();
        for (int word = 0; word < distributed.length; word++) {
            collect(origin, word, distributed[word], withdrawn);
            distributed[word] = 0L;
        }
        if (!withdrawn.isEmpty()) {
            OxygenApi.API.removeOxygen(level, withdrawn);
            clearGeneration++;
            markDirty();
        }
    }

    private void collect(BlockPos origin, int word, long bits, List<BlockPos> out) {
        while (bits != 0L) {
            int cell = word << 6 | Long.numberOfTrailingZeros(bits);
            bits &= bits - 1;
            out.add(origin.offset(cell / area - radius, cell / side % side - radius, cell % side - radius));
        }
    }

    private int index(int x, int y, int z) {
        return (x + radius) * area + (y + radius) * side + (z + radius);
    }
}
