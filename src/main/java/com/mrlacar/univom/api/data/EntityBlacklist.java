package com.mrlacar.univom.api.data;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.Tags;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class EntityBlacklist {
    private static final Set<EntityType<?>> BLACKLISTED_TYPES = ConcurrentHashMap.newKeySet();

    static {
        BLACKLISTED_TYPES.add(EntityType.VILLAGER);
        BLACKLISTED_TYPES.add(EntityType.WANDERING_TRADER);
        BLACKLISTED_TYPES.add(EntityType.WARDEN);
        BLACKLISTED_TYPES.add(EntityType.ENDER_DRAGON);
        BLACKLISTED_TYPES.add(EntityType.WITHER);
        BLACKLISTED_TYPES.add(EntityType.PLAYER);
        BLACKLISTED_TYPES.add(EntityType.ARMOR_STAND);
    }

    @SuppressWarnings("unused")
    public static void addCustomBlacklist(EntityType<?> entityType) {
        if (entityType != null) {
            BLACKLISTED_TYPES.add(entityType);
        }
    }

    public static boolean isBlacklisted(LivingEntity entity) {
        if (entity == null) return true;

        return entity instanceof Player || isBlacklisted(entity.getType()) || !entity.canChangeDimensions();
    }

    public static boolean isBlacklisted(EntityType<?> type) {
        if (type == null) return true;
        return BLACKLISTED_TYPES.contains(type) || type.is(Tags.EntityTypes.BOSSES);
    }
}
