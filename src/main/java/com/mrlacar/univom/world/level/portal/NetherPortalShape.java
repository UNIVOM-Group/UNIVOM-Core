package com.mrlacar.univom.world.level.portal;

import com.mrlacar.univom.common.data.ModBlocks;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Predicate;

public class NetherPortalShape {
    private static final int MIN_WIDTH = 2;
    public static final int MAX_WIDTH = 21;
    private static final int MIN_HEIGHT = 3;
    public static final int MAX_HEIGHT = 21;
    private static final BlockBehaviour.StatePredicate FRAME = (state, getter, pos) -> state.is(ModBlocks.FLUIX_RESONANCE_OBSIDIAN.get());
    private static final float SAFE_TRAVEL_MAX_ENTITY_XY = 4.0F;
    private static final double SAFE_TRAVEL_MAX_VERTICAL_DELTA = 1.0D;
    private final LevelAccessor level;
    private final Direction.Axis axis;
    private final Direction rightDir;
    private int numPortalBlocks;
    @Nullable
    private BlockPos bottomLeft;
    private int height;
    private final int width;

    public static Optional<NetherPortalShape> findEmptyPortalShape(LevelAccessor level, BlockPos bottomLeft, Direction.Axis axis) {
        return findPortalShape(level, bottomLeft, (NetherPortalShape) -> NetherPortalShape.isValid() && NetherPortalShape.numPortalBlocks == 0, axis);
    }

    public static Optional<NetherPortalShape> findPortalShape(LevelAccessor level, BlockPos bottomLeft, Predicate<NetherPortalShape> predicate, Direction.Axis axis) {
        Optional<NetherPortalShape> optional = Optional.of(new NetherPortalShape(level, bottomLeft, axis)).filter(predicate);
        if (optional.isPresent()) {
            return optional;
        } else {
            Direction.Axis orthogonalAxis = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
            return Optional.of(new NetherPortalShape(level, bottomLeft, orthogonalAxis)).filter(predicate);
        }
    }

    public NetherPortalShape(LevelAccessor level, BlockPos bottomLeft, Direction.Axis axis) {
        this.level = level;
        this.axis = axis;
        this.rightDir = axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
        this.bottomLeft = this.calculateBottomLeft(bottomLeft);
        if (this.bottomLeft == null) {
            this.bottomLeft = bottomLeft;
            this.width = 1;
            this.height = 1;
        } else {
            this.width = this.calculateWidth();
            if (this.width > 0) {
                this.height = this.calculateHeight();
            }
        }
    }

    @Nullable
    private BlockPos calculateBottomLeft(BlockPos pos) {
        for(int i = Math.max(this.level.getMinBuildHeight(), pos.getY() - 21); pos.getY() > i && isEmpty(this.level.getBlockState(pos.below())); pos = pos.below()) {
        }

        Direction direction = this.rightDir.getOpposite();
        int j = this.getDistanceUntilEdgeAboveFrame(pos, direction) - 1;
        return j < 0 ? null : pos.relative(direction, j);
    }

    private int calculateWidth() {
        int i = this.getDistanceUntilEdgeAboveFrame(this.bottomLeft, this.rightDir);
        return i >= 2 && i <= 21 ? i : 0;
    }

    private int getDistanceUntilEdgeAboveFrame(BlockPos pos, Direction direction) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for(int i = 0; i <= 21; ++i) {
            mutablePos.set(pos).move(direction, i);
            BlockState blockstate = this.level.getBlockState(mutablePos);
            if (!isEmpty(blockstate)) {
                if (FRAME.test(blockstate, this.level, mutablePos)) {
                    return i;
                }
                break;
            }
            BlockState blockstate1 = this.level.getBlockState(mutablePos.move(Direction.DOWN));
            if (!FRAME.test(blockstate1, this.level, mutablePos)) {
                break;
            }
        }
        return 0;
    }

    private int calculateHeight() {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int i = this.getDistanceUntilTop(mutablePos);
        return i >= 3 && i <= 21 && this.hasTopFrame(mutablePos, i) ? i : 0;
    }

    private boolean hasTopFrame(BlockPos.MutableBlockPos pos, int distanceToTop) {
        for(int i = 0; i < this.width; ++i) {
            BlockPos.MutableBlockPos mutablePos = pos.set(this.bottomLeft).move(Direction.UP, distanceToTop).move(this.rightDir, i);
            if (!FRAME.test(this.level.getBlockState(mutablePos), this.level, mutablePos)) {
                return false;
            }
        }
        return true;
    }

    private int getDistanceUntilTop(BlockPos.MutableBlockPos pos) {
        for(int i = 0; i < 21; ++i) {
            pos.set(this.bottomLeft).move(Direction.UP, i).move(this.rightDir, -1);
            if (!FRAME.test(this.level.getBlockState(pos), this.level, pos)) {
                return i;
            }

            pos.set(this.bottomLeft).move(Direction.UP, i).move(this.rightDir, this.width);
            if (!FRAME.test(this.level.getBlockState(pos), this.level, pos)) {
                return i;
            }

            for(int j = 0; j < this.width; ++j) {
                pos.set(this.bottomLeft).move(Direction.UP, i).move(this.rightDir, j);
                BlockState blockstate = this.level.getBlockState(pos);
                if (!isEmpty(blockstate)) {
                    return i;
                }
                if (blockstate.is(ModBlocks.NETHER_PORTAL.get())) {
                    ++this.numPortalBlocks;
                }
            }
        }
        return 21;
    }

    private static boolean isEmpty(BlockState state) {
        return state.isAir() || state.is(BlockTags.FIRE) || state.is(ModBlocks.NETHER_PORTAL.get());
    }

    public boolean isValid() {
        return this.bottomLeft != null && this.width >= 2 && this.width <= 21 && this.height >= 3 && this.height <= 21;
    }

    public void createPortalBlocks() {
        BlockState blockstate = ModBlocks.NETHER_PORTAL.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_AXIS, this.axis);
        BlockPos.betweenClosed(this.bottomLeft, this.bottomLeft.relative(Direction.UP, this.height - 1).relative(this.rightDir, this.width - 1)).forEach((pos) -> {
            this.level.setBlock(pos, blockstate, 18);
        });
    }

    public boolean isComplete() {
        return this.isValid() && this.numPortalBlocks == this.width * this.height;
    }

    public static Vec3 getRelativePosition(BlockUtil.FoundRectangle foundRectangle, Direction.Axis axis, Vec3 pos, EntityDimensions entityDimensions) {
        double d0 = (double)foundRectangle.axis1Size - (double)entityDimensions.width;
        double d1 = (double)foundRectangle.axis2Size - (double)entityDimensions.height;
        BlockPos blockpos = foundRectangle.minCorner;
        double d2;
        if (d0 > 0.0D) {
            float f = (float)blockpos.get(axis) + entityDimensions.width / 2.0F;
            d2 = Mth.clamp(Mth.inverseLerp(pos.get(axis) - (double)f, 0.0D, d0), 0.0D, 1.0D);
        } else {
            d2 = 0.5D;
        }

        double d4;
        if (d1 > 0.0D) {
            Direction.Axis verticalAxis = Direction.Axis.Y;
            d4 = Mth.clamp(Mth.inverseLerp(pos.get(verticalAxis) - (double)blockpos.get(verticalAxis), 0.0D, d1), 0.0D, 1.0D);
        } else {
            d4 = 0.0D;
        }

        Direction.Axis orthogonalAxis = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        double d3 = pos.get(orthogonalAxis) - ((double)blockpos.get(orthogonalAxis) + 0.5D);
        return new Vec3(d2, d4, d3);
    }

    public static PortalInfo createPortalInfo(ServerLevel level, BlockUtil.FoundRectangle portalPos, Direction.Axis axis, Vec3 relativePos, Entity entity, Vec3 velocity, float yRot, float xRot) {
        BlockPos blockpos = portalPos.minCorner;
        BlockState blockstate = level.getBlockState(blockpos);
        Direction.Axis portalAxis = blockstate.getOptionalValue(BlockStateProperties.HORIZONTAL_AXIS).orElse(Direction.Axis.X);
        double d0 = portalPos.axis1Size;
        double d1 = portalPos.axis2Size;
        EntityDimensions entitydimensions = entity.getDimensions(entity.getPose());
        int i = axis == portalAxis ? 0 : 90;
        Vec3 vec3 = axis == portalAxis ? velocity : new Vec3(velocity.z, velocity.y, -velocity.x);
        double d2 = (double)entitydimensions.width / 2.0D + (d0 - (double)entitydimensions.width) * relativePos.x();
        double d3 = (d1 - (double)entitydimensions.height) * relativePos.y();
        double d4 = 0.5D + relativePos.z();
        boolean isXAxis = portalAxis == Direction.Axis.X;
        Vec3 vec31 = new Vec3((double)blockpos.getX() + (isXAxis ? d2 : d4), (double)blockpos.getY() + d3, (double)blockpos.getZ() + (isXAxis ? d4 : d2));
        Vec3 vec32 = findCollisionFreePosition(vec31, level, entity, entitydimensions);
        return new PortalInfo(vec32, vec3, yRot + (float)i, xRot);
    }

    private static Vec3 findCollisionFreePosition(Vec3 pos, ServerLevel level, Entity entity, EntityDimensions dimensions) {
        if (!(dimensions.width > 4.0F) && !(dimensions.height > 4.0F)) {
            double d0 = (double)dimensions.height / 2.0D;
            Vec3 vec3 = pos.add(0.0D, d0, 0.0D);
            VoxelShape voxelshape = Shapes.create(AABB.ofSize(vec3, dimensions.width, 0.0D, dimensions.width).expandTowards(0.0D, 1.0D, 0.0D).inflate(1.0E-6D));
            Optional<Vec3> optional = level.findFreePosition(entity, voxelshape, vec3, dimensions.width, dimensions.height, dimensions.width);
            Optional<Vec3> optional1 = optional.map((freePos) -> freePos.subtract(0.0D, d0, 0.0D));
            return optional1.orElse(pos);
        } else {
            return pos;
        }
    }
}
