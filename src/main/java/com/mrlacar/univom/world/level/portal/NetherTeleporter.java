package com.mrlacar.univom.world.level.portal;

import com.mrlacar.univom.common.data.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import java.util.function.Function;

public class NetherTeleporter implements ITeleporter {
    private final BlockPos enterPos;

    public NetherTeleporter(BlockPos enterPos) {
        this.enterPos = enterPos;
    }

    @Override
    public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
        double coordinateScale = entity.level().dimensionType().coordinateScale() / destWorld.dimensionType().coordinateScale();
        double targetX = entity.getX() * coordinateScale;
        double targetY = entity.getY();
        double targetZ = entity.getZ() * coordinateScale;

        if (destWorld.dimension() == Level.NETHER) {
            targetY = Math.min(targetY, 115.0);
        }

        BlockPos destPos = new BlockPos((int) targetX, (int) targetY, (int) targetZ);

        BlockPos existingPortal = findExistingPortal(destWorld, destPos);

        if (existingPortal == null) {
            createPortal(destWorld, destPos);
            existingPortal = destPos;
        }

        return new PortalInfo(
                new Vec3(existingPortal.getX() + 0.5, existingPortal.getY(), existingPortal.getZ() + 0.5),
                entity.getDeltaMovement(),
                entity.getYRot(),
                entity.getXRot()
        );
    }

    private BlockPos findExistingPortal(ServerLevel destWorld, BlockPos searchCenter) {
        int radius = 16;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -64; y <= 64; y++) {
                    BlockPos checkPos = searchCenter.offset(x, y, z);
                    if (destWorld.isOutsideBuildHeight(checkPos)) continue;

                    if (destWorld.getBlockState(checkPos).is(ModBlocks.NETHER_PORTAL.get())) {
                        return checkPos;
                    }
                }
            }
        }
        return null;
    }

    private void createPortal(ServerLevel destWorld, BlockPos destPos) {
        BlockState frame = ModBlocks.FLUIX_RESONANCE_OBSIDIAN.get().defaultBlockState();
        BlockState portal = ModBlocks.NETHER_PORTAL.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_AXIS, Direction.Axis.X);

        BlockPos bestPos = destPos;
        boolean foundSafeSpot = false;

        for (BlockPos.MutableBlockPos mutable : BlockPos.spiralAround(destPos, 16, Direction.EAST, Direction.SOUTH)) {
            BlockPos testColumn = mutable.immutable();
            if (destWorld.isOutsideBuildHeight(testColumn)) continue;

            int maxY = Math.min(destWorld.getMaxBuildHeight() - 5, destPos.getY() + 10);
            int minY = Math.max(destWorld.getMinBuildHeight() + 5, destPos.getY() - 10);

            for (int y = maxY; y >= minY; y--) {
                BlockPos checkPos = testColumn.atY(y);

                if (destWorld.getBlockState(checkPos).isSolidRender(destWorld, checkPos) &&
                        destWorld.getBlockState(checkPos.above(1)).isAir() &&
                        destWorld.getBlockState(checkPos.above(2)).isAir() &&
                        destWorld.getBlockState(checkPos.above(3)).isAir() &&
                        destWorld.getBlockState(checkPos.above(4)).isAir()) {

                    bestPos = checkPos.above();
                    foundSafeSpot = true;
                    break;
                }
            }
            if (foundSafeSpot) break;
        }

        for (int y = -1; y < 4; y++) {
            for (int x = -1; x < 3; x++) {
                BlockPos pos = bestPos.offset(x, y, 0);

                if (x == -1 || x == 2 || y == -1 || y == 3) {
                    destWorld.setBlockAndUpdate(pos, frame);
                } else {
                    destWorld.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }

                destWorld.setBlockAndUpdate(pos.offset(0, 0, 1), Blocks.AIR.defaultBlockState());
                destWorld.setBlockAndUpdate(pos.offset(0, 0, -1), Blocks.AIR.defaultBlockState());
            }
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 2; x++) {
                destWorld.setBlockAndUpdate(bestPos.offset(x, y, 0), portal);
            }
        }

        for (int x = -1; x < 3; x++) {
            destWorld.setBlockAndUpdate(bestPos.offset(x, -1, 1), frame);
            destWorld.setBlockAndUpdate(bestPos.offset(x, -1, -1), frame);
        }
    }
}
