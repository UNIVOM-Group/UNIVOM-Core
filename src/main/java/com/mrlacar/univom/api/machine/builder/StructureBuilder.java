package com.mrlacar.univom.api.machine.builder;

import com.mrlacar.univom.mixin.gtceu.api.pattern.BlockPatternAccessor;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ForgeHooks;

import com.lowdragmc.lowdraglib.utils.BlockInfo;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public final class StructureBuilder {

    private static final Direction[] FACINGS = { Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST,
            Direction.UP, Direction.DOWN };
    private static final Direction[] FACINGS_H = { Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST };

    private final ServerPlayer player;
    private final ItemStack terminal;
    private final MaterialSource source;
    private final Level world;
    private final BlockPattern pattern;
    private final RelativeDirection[] structureDir;
    private final TraceabilityPredicate[][][] blockMatches;
    private final int fingerLength;
    private final int thumbLength;
    private final int palmLength;
    private final int[] centerOffset;
    private final boolean placeParts;
    private final boolean anyReplace;
    private final Block[] moduleBlocks;
    private final boolean[] moduleReplace;

    private final Reference2ObjectMap<TraceabilityPredicate, Set<Block>> candidateCache =
            new Reference2ObjectOpenHashMap<>();
    private final Reference2BooleanMap<SimplePredicate> partPredicateCache = new Reference2BooleanOpenHashMap<>();
    private final List<ItemStack> scratchCandidates = new ArrayList<>();
    private final Set<Block> scratchCandidateBlocks = new ReferenceOpenHashSet<>();

    private StructureBuilder(ServerPlayer player, ItemStack terminal, MaterialSource source, BlockPattern pattern) {
        this.player = player;
        this.terminal = terminal;
        this.source = source;
        this.world = player.level();
        this.pattern = pattern;
        BlockPatternAccessor accessor = (BlockPatternAccessor) pattern;
        this.structureDir = pattern.structureDir;
        this.blockMatches = accessor.univom$getBlockMatches();
        this.fingerLength = accessor.univom$getFingerLength();
        this.thumbLength = accessor.univom$getThumbLength();
        this.palmLength = accessor.univom$getPalmLength();
        this.centerOffset = accessor.univom$getCenterOffset();
        this.placeParts = BuilderSettings.isPlaceParts(terminal);
        this.anyReplace = BuilderSettings.hasAnyReplace(terminal);
        List<BuilderModuleCategory> categories = BuilderModules.ALL;
        this.moduleBlocks = new Block[categories.size()];
        this.moduleReplace = new boolean[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            moduleBlocks[i] = BuilderSettings.getModuleBlock(terminal, categories.get(i));
            moduleReplace[i] = BuilderSettings.isModuleReplace(terminal, categories.get(i));
        }
    }

    public static BuildResult build(ServerPlayer player, ItemStack terminal, IMultiController controller,
                                    MaterialSource source) {
        BlockPattern pattern = controller.getPattern();
        if (pattern == null) {
            return BuildResult.NO_PATTERN;
        }
        StructureBuilder builder = new StructureBuilder(player, terminal, source, pattern);
        int[] repetitions = builder.resolveRepetitions(BuilderSettings.getLayers(terminal));
        if (builder.countPositions(repetitions) > BuilderSettings.MAX_BLOCKS_PER_OPERATION) {
            return BuildResult.TOO_LARGE;
        }
        Lock lock = controller.getPatternLock();
        lock.lock();
        try {
            builder.run(controller, repetitions);
        } finally {
            lock.unlock();
        }
        return BuildResult.OK;
    }

    private int[] resolveRepetitions(int layers) {
        int[][] bounds = pattern.aisleRepetitions;
        int[] repetitions = new int[fingerLength];
        for (int c = 0; c < fingerLength; c++) {
            repetitions[c] = Mth.clamp(layers, bounds[c][0], bounds[c][1]);
        }
        return repetitions;
    }

    private int countPositions(int[] repetitions) {
        int total = 0;
        for (int repetition : repetitions) {
            total += repetition * thumbLength * palmLength;
        }
        return total;
    }

    private void run(IMultiController controller, int[] repetitions) {
        MultiblockState worldState = controller.getMultiblockState();
        int minZ = -centerOffset[4];
        worldState.clean();
        BlockPos centerPos = controller.self().getPos();
        Direction facing = controller.self().getFrontFacing();
        Direction upwardsFacing = controller.self().getUpwardsFacing();
        boolean isFlipped = controller.self().isFlipped();
        Object2IntOpenHashMap<SimplePredicate> cacheGlobal = worldState.getGlobalCount();
        Object2IntOpenHashMap<SimplePredicate> cacheLayer = worldState.getLayerCount();
        Map<BlockPos, Object> blocks = new HashMap<>();
        Set<BlockPos> placeBlockPos = new HashSet<>();
        blocks.put(centerPos, controller);

        for (int c = 0, z = minZ++, r; c < this.fingerLength; c++) {
            for (r = 0; r < repetitions[c]; r++) {
                cacheLayer.clear();
                for (int b = 0, y = -centerOffset[1]; b < this.thumbLength; b++, y++) {
                    for (int a = 0, x = -centerOffset[0]; a < this.palmLength; a++, x++) {
                        TraceabilityPredicate predicate = this.blockMatches[c][b][a];
                        BlockPos pos = setActualRelativeOffset(x, y, z, facing, upwardsFacing, isFlipped)
                                .offset(centerPos.getX(), centerPos.getY(), centerPos.getZ());
                        worldState.update(pos, predicate);
                        if (!world.isEmptyBlock(pos)) {
                            for (SimplePredicate limit : predicate.limited) {
                                limit.testLimited(worldState);
                            }
                            BlockState replaced = anyReplace ? tryReplace(pos, predicate) : null;
                            blocks.put(pos, replaced == null ? world.getBlockState(pos) : replaced);
                            if (replaced != null) {
                                placeBlockPos.add(pos);
                            }
                        } else {
                            BlockInfo[] infos = selectCandidates(predicate, cacheGlobal, cacheLayer);
                            ItemStack found = placeAt(pos, infos);
                            if (found != null) {
                                placeBlockPos.add(pos);
                            }
                            if (world.getBlockEntity(pos) instanceof IMachineBlockEntity machineBlockEntity) {
                                blocks.put(pos, machineBlockEntity.getMetaMachine());
                            } else {
                                blocks.put(pos, world.getBlockState(pos));
                            }
                        }
                    }
                }
                z++;
            }
        }

        Direction frontFacing = controller.self().getFrontFacing();
        blocks.forEach((pos, block) -> {
            if (block instanceof IMultiController) {
                return;
            }
            if (block instanceof BlockState state && placeBlockPos.contains(pos)) {
                resetFacing(pos, state, frontFacing, (p, f) -> {
                    Object object = blocks.get(p.relative(f));
                    return object == null || (object instanceof BlockState other && other.getBlock() == Blocks.AIR);
                }, state1 -> world.setBlock(pos, state1, 3));
            } else if (block instanceof MetaMachine machine) {
                resetFacing(pos, machine.getBlockState(), frontFacing, (p, f) -> {
                    Object object = blocks.get(p.relative(f));
                    if (object == null || (object instanceof BlockState blockState && blockState.isAir())) {
                        return machine.isFacingValid(f);
                    }
                    return false;
                }, state1 -> world.setBlock(pos, state1, 3));
            }
        });
    }

    private BlockInfo[] selectCandidates(TraceabilityPredicate predicate,
                                         Object2IntOpenHashMap<SimplePredicate> cacheGlobal,
                                         Object2IntOpenHashMap<SimplePredicate> cacheLayer) {
        boolean find = false;
        BlockInfo[] infos = new BlockInfo[0];
        for (SimplePredicate limit : predicate.limited) {
            if (isPartPredicate(limit)) {
                continue;
            }
            if (limit.minLayerCount > 0) {
                int curr = cacheLayer.getInt(limit);
                if (curr < limit.minLayerCount && (limit.maxLayerCount == -1 || curr < limit.maxLayerCount)) {
                    cacheLayer.addTo(limit, 1);
                } else {
                    continue;
                }
            } else {
                continue;
            }
            infos = limit.candidates == null ? null : limit.candidates.get();
            find = true;
            break;
        }
        if (!find) {
            for (SimplePredicate limit : predicate.limited) {
                if (isPartPredicate(limit)) {
                    continue;
                }
                if (limit.minCount > 0) {
                    int curr = cacheGlobal.getInt(limit);
                    if (curr < limit.minCount && (limit.maxCount == -1 || curr < limit.maxCount)) {
                        cacheGlobal.addTo(limit, 1);
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
                infos = limit.candidates == null ? null : limit.candidates.get();
                find = true;
                break;
            }
        }
        if (!find) {
            for (SimplePredicate limit : predicate.limited) {
                if (isPartPredicate(limit)) {
                    continue;
                }
                if (limit.maxLayerCount != -1 &&
                        cacheLayer.getOrDefault(limit, Integer.MAX_VALUE) == limit.maxLayerCount) {
                    continue;
                }
                if (limit.maxCount != -1 && cacheGlobal.getOrDefault(limit, Integer.MAX_VALUE) == limit.maxCount) {
                    continue;
                }
                cacheLayer.addTo(limit, 1);
                cacheGlobal.addTo(limit, 1);
                infos = ArrayUtils.addAll(infos, limit.candidates == null ? null : limit.candidates.get());
            }
            for (SimplePredicate common : predicate.common) {
                if (isPartPredicate(common)) {
                    continue;
                }
                infos = ArrayUtils.addAll(infos, common.candidates == null ? null : common.candidates.get());
            }
        }
        return infos;
    }

    @Nullable
    private ItemStack placeAt(BlockPos pos, @Nullable BlockInfo[] infos) {
        if (infos == null) {
            return null;
        }
        List<ItemStack> candidates = this.scratchCandidates;
        Set<Block> candidateBlocks = this.scratchCandidateBlocks;
        candidates.clear();
        candidateBlocks.clear();
        for (BlockInfo info : infos) {
            Block block = info.getBlockState().getBlock();
            if (block == Blocks.AIR) {
                continue;
            }
            if (!placeParts && block instanceof MetaMachineBlock) {
                continue;
            }
            candidateBlocks.add(block);
            candidates.add(info.getItemStackForm());
        }
        int category = preferredCategory(candidateBlocks);
        if (category >= 0) {
            ItemStack preferred = new ItemStack(moduleBlocks[category]);
            if (!preferred.isEmpty()) {
                for (int i = candidates.size() - 1; i >= 0; i--) {
                    if (candidates.get(i).getItem() == preferred.getItem()) {
                        candidates.remove(i);
                    }
                }
                candidates.add(0, preferred);
            }
        }
        for (ItemStack candidate : candidates) {
            if (candidate.isEmpty() || !(candidate.getItem() instanceof BlockItem)) {
                continue;
            }
            if (source.extract(candidate, 1, true) < 1) {
                continue;
            }
            if (!player.mayUseItemAt(pos, Direction.UP, candidate)) {
                return null;
            }
            ItemStack placing = candidate.copy();
            BlockPlaceContext context = new BlockPlaceContext(world, player, InteractionHand.MAIN_HAND, placing,
                    BlockHitResult.miss(player.getEyePosition(0), Direction.UP, pos));
            if (((BlockItem) placing.getItem()).place(context) != InteractionResult.FAIL) {
                source.extract(candidate, 1, false);
                return candidate;
            }
        }
        return null;
    }

    @Nullable
    private BlockState tryReplace(BlockPos pos, TraceabilityPredicate predicate) {
        BlockState current = world.getBlockState(pos);
        Block currentBlock = current.getBlock();
        List<BuilderModuleCategory> categories = BuilderModules.ALL;
        for (int i = 0; i < categories.size(); i++) {
            if (!moduleReplace[i] || !categories.get(i).contains(currentBlock)) {
                continue;
            }
            Block target = moduleBlocks[i];
            if (target == currentBlock || !candidateBlocks(predicate).contains(target)) {
                return null;
            }
            ItemStack targetStack = new ItemStack(target);
            if (targetStack.isEmpty() || !(targetStack.getItem() instanceof BlockItem blockItem) ||
                    source.extract(targetStack, 1, true) < 1) {
                return null;
            }
            if (!player.mayUseItemAt(pos, Direction.UP, targetStack) ||
                    ForgeHooks.onBlockBreakEvent(world, player.gameMode.getGameModeForPlayer(), player, pos) == -1) {
                return null;
            }
            world.removeBlock(pos, false);
            ItemStack placing = targetStack.copy();
            BlockPlaceContext context = new BlockPlaceContext(world, player, InteractionHand.MAIN_HAND, placing,
                    BlockHitResult.miss(player.getEyePosition(0), Direction.UP, pos));
            if (blockItem.place(context) == InteractionResult.FAIL) {
                world.setBlock(pos, current, 3);
                return null;
            }
            source.extract(targetStack, 1, false);
            ItemStack remainder = source.give(new ItemStack(currentBlock));
            if (!remainder.isEmpty()) {
                player.drop(remainder, false);
            }
            return world.getBlockState(pos);
        }
        return null;
    }

    private int preferredCategory(Set<Block> candidateBlocks) {
        if (candidateBlocks.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < moduleBlocks.length; i++) {
            if (candidateBlocks.contains(moduleBlocks[i])) {
                return i;
            }
        }
        return -1;
    }

    private Set<Block> candidateBlocks(TraceabilityPredicate predicate) {
        Set<Block> cached = candidateCache.get(predicate);
        if (cached != null) {
            return cached;
        }
        Set<Block> blocks = new ReferenceOpenHashSet<>();
        collectCandidateBlocks(predicate.limited, blocks);
        collectCandidateBlocks(predicate.common, blocks);
        candidateCache.put(predicate, blocks);
        return blocks;
    }

    private static void collectCandidateBlocks(List<SimplePredicate> predicates, Set<Block> out) {
        for (SimplePredicate simple : predicates) {
            if (simple.candidates == null) {
                continue;
            }
            BlockInfo[] infos = simple.candidates.get();
            if (infos == null) {
                continue;
            }
            for (BlockInfo info : infos) {
                Block block = info.getBlockState().getBlock();
                if (block != Blocks.AIR) {
                    out.add(block);
                }
            }
        }
    }

    private boolean isPartPredicate(SimplePredicate simple) {
        if (placeParts) {
            return false;
        }
        if (partPredicateCache.containsKey(simple)) {
            return partPredicateCache.getBoolean(simple);
        }
        boolean isPart = false;
        if (simple.candidates != null) {
            BlockInfo[] infos = simple.candidates.get();
            if (infos != null && infos.length > 0) {
                isPart = true;
                for (BlockInfo info : infos) {
                    if (!(info.getBlockState().getBlock() instanceof MetaMachineBlock)) {
                        isPart = false;
                        break;
                    }
                }
            }
        }
        partPredicateCache.put(simple, isPart);
        return isPart;
    }

    private void resetFacing(BlockPos pos, BlockState blockState, Direction facing,
                             BiPredicate<BlockPos, Direction> checker, Consumer<BlockState> consumer) {
        if (blockState.hasProperty(BlockStateProperties.FACING)) {
            tryFacings(blockState, pos, checker, consumer, BlockStateProperties.FACING,
                    facing == null ? FACINGS : ArrayUtils.addAll(new Direction[] { facing }, FACINGS));
        } else if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            tryFacings(blockState, pos, checker, consumer, BlockStateProperties.HORIZONTAL_FACING,
                    facing == null || facing.getAxis() == Direction.Axis.Y ? FACINGS_H :
                            ArrayUtils.addAll(new Direction[] { facing }, FACINGS_H));
        }
    }

    private void tryFacings(BlockState blockState, BlockPos pos, BiPredicate<BlockPos, Direction> checker,
                            Consumer<BlockState> consumer, Property<Direction> property, Direction[] facings) {
        Direction found = null;
        for (Direction facing : facings) {
            if (checker.test(pos, facing)) {
                found = facing;
                break;
            }
        }
        if (found == null) {
            found = Direction.NORTH;
        }
        consumer.accept(blockState.setValue(property, found));
    }

    private BlockPos setActualRelativeOffset(int x, int y, int z, Direction facing, Direction upwardsFacing,
                                             boolean isFlipped) {
        int[] c0 = new int[] { x, y, z }, c1 = new int[3];
        if (facing == Direction.UP || facing == Direction.DOWN) {
            Direction of = facing == Direction.DOWN ? upwardsFacing : upwardsFacing.getOpposite();
            for (int i = 0; i < 3; i++) {
                switch (structureDir[i].getActualDirection(of)) {
                    case UP -> c1[1] = c0[i];
                    case DOWN -> c1[1] = -c0[i];
                    case WEST -> c1[0] = -c0[i];
                    case EAST -> c1[0] = c0[i];
                    case NORTH -> c1[2] = -c0[i];
                    case SOUTH -> c1[2] = c0[i];
                }
            }
            int xOffset = upwardsFacing.getStepX();
            int zOffset = upwardsFacing.getStepZ();
            int tmp;
            if (xOffset == 0) {
                tmp = c1[2];
                c1[2] = zOffset > 0 ? c1[1] : -c1[1];
                c1[1] = zOffset > 0 ? -tmp : tmp;
            } else {
                tmp = c1[0];
                c1[0] = xOffset > 0 ? c1[1] : -c1[1];
                c1[1] = xOffset > 0 ? -tmp : tmp;
            }
            if (isFlipped) {
                if (upwardsFacing == Direction.NORTH || upwardsFacing == Direction.SOUTH) {
                    c1[0] = -c1[0];
                } else {
                    c1[2] = -c1[2];
                }
            }
        } else {
            for (int i = 0; i < 3; i++) {
                switch (structureDir[i].getActualDirection(facing)) {
                    case UP -> c1[1] = c0[i];
                    case DOWN -> c1[1] = -c0[i];
                    case WEST -> c1[0] = -c0[i];
                    case EAST -> c1[0] = c0[i];
                    case NORTH -> c1[2] = -c0[i];
                    case SOUTH -> c1[2] = c0[i];
                }
            }
            if (upwardsFacing == Direction.WEST || upwardsFacing == Direction.EAST) {
                int xOffset = upwardsFacing == Direction.EAST ? facing.getClockWise().getStepX() :
                        facing.getClockWise().getOpposite().getStepX();
                int zOffset = upwardsFacing == Direction.EAST ? facing.getClockWise().getStepZ() :
                        facing.getClockWise().getOpposite().getStepZ();
                int tmp;
                if (xOffset == 0) {
                    tmp = c1[2];
                    c1[2] = zOffset > 0 ? -c1[1] : c1[1];
                    c1[1] = zOffset > 0 ? tmp : -tmp;
                } else {
                    tmp = c1[0];
                    c1[0] = xOffset > 0 ? -c1[1] : c1[1];
                    c1[1] = xOffset > 0 ? tmp : -tmp;
                }
            } else if (upwardsFacing == Direction.SOUTH) {
                c1[1] = -c1[1];
                if (facing.getStepX() == 0) {
                    c1[0] = -c1[0];
                } else {
                    c1[2] = -c1[2];
                }
            }
            if (isFlipped) {
                if (upwardsFacing == Direction.NORTH || upwardsFacing == Direction.SOUTH) {
                    if (facing == Direction.NORTH || facing == Direction.SOUTH) {
                        c1[0] = -c1[0];
                    } else {
                        c1[2] = -c1[2];
                    }
                } else {
                    c1[1] = -c1[1];
                }
            }
        }
        return new BlockPos(c1[0], c1[1], c1[2]);
    }

    public enum BuildResult {
        OK,
        NO_PATTERN,
        TOO_LARGE
    }
}
