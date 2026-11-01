package com.mrlacar.univom.mixin.minecraft.world.level.block;

import com.mrlacar.univom.common.data.ModBlocks;
import com.mrlacar.univom.world.level.portal.NetherPortalShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(BaseFireBlock.class)
public class BaseFireBlockMixin {

    private static boolean univom$isEmpty(BlockState state) {
        return state.isAir() || state.is(BlockTags.FIRE) || state.is(ModBlocks.NETHER_PORTAL.get());
    }

    private static boolean univom$couldBePortalInterior(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
        int minY = Math.max(level.getMinBuildHeight(), pos.getY() - NetherPortalShape.MAX_HEIGHT);

        while (cursor.getY() > minY) {
            cursor.move(Direction.DOWN);
            BlockState below = level.getBlockState(cursor);
            if (!univom$isEmpty(below)) {
                return below.is(ModBlocks.FLUIX_RESONANCE_OBSIDIAN.get());
            }
        }
        return false;
    }

    @Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
    private void checkNetherPortalIgnition(BlockState state, Level level, BlockPos pos, BlockState oldState,
                                           boolean isMoving, CallbackInfo ci) {
        if (level.isClientSide) return;
        if (oldState.is(state.getBlock())) return;
        if (!univom$couldBePortalInterior(level, pos)) return;

        Optional<NetherPortalShape> shape = NetherPortalShape.findEmptyPortalShape(level, pos, Direction.Axis.X);

        if (shape.isPresent()) {
            shape.get().createPortalBlocks();
            ci.cancel();
        }
    }
}
