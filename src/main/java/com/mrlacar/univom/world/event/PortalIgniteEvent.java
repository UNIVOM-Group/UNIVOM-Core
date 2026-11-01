package com.mrlacar.univom.world.event;

import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.common.data.ModBlocks;
import com.mrlacar.univom.world.level.portal.NetherTeleporter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = UNIVOM.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PortalIgniteEvent {
    private static final Set<UUID> TRAVELING_ENTITIES = new HashSet<>();

    @SubscribeEvent
    public static void onTravel(EntityTravelToDimensionEvent event) {
        Entity entity = event.getEntity();

        if (TRAVELING_ENTITIES.contains(entity.getUUID())) {
            return;
        }

        Level level = entity.level();
        boolean inCustomPortal = false;

        if (level.getBlockState(entity.blockPosition()).is(ModBlocks.NETHER_PORTAL.get()) ||
                level.getBlockState(entity.blockPosition().above()).is(ModBlocks.NETHER_PORTAL.get())) {
            inCustomPortal = true;
        } else {
            AABB box = entity.getBoundingBox();
            Iterable<BlockPos> intersectingBlocks = BlockPos.betweenClosed(
                    Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ),
                    Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ)
            );
            for (BlockPos p : intersectingBlocks) {
                if (level.getBlockState(p).is(ModBlocks.NETHER_PORTAL.get())) {
                    inCustomPortal = true;
                    break;
                }
            }
        }

        if (inCustomPortal) {
            event.setCanceled(true);

            if (!level.isClientSide) {
                ServerLevel serverLevel = (ServerLevel) level;
                ResourceLocation targetDim = level.dimension() == Level.NETHER ? Level.OVERWORLD.location() : Level.NETHER.location();
                ServerLevel destination = serverLevel.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, targetDim));

                if (destination != null) {
                    entity.setPortalCooldown();

                    TRAVELING_ENTITIES.add(entity.getUUID());
                    try {
                        entity.changeDimension(destination, new NetherTeleporter(entity.blockPosition()));
                    } finally {
                        TRAVELING_ENTITIES.remove(entity.getUUID());
                    }
                }
            }
        }
    }
}
