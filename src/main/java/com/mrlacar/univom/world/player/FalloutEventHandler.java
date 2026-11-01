package com.mrlacar.univom.world.player;

import com.gregtechceu.gtceu.api.data.chemical.material.properties.HazardProperty;
import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.common.data.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import static com.mrlacar.univom.common.data.ModEffects.IRRADIATION;

@Mod.EventBusSubscriber(modid = UNIVOM.MOD_ID)
public class FalloutEventHandler {

    private static final String TAG_FALLOUT_MARKER = "univom_fallout";
    private static final String TAG_EXPIRY = "expiry_time";
    private static final String TAG_EXPOSURE = "univom_radiation_exposure";
    private static final String TAG_PRUSSIAN_BLUE = "univom_prussian_blue_duration";

    private static final double FALLOUT_RADIUS = 40.0;
    private static final double FALLOUT_RADIUS_SQR = FALLOUT_RADIUS * FALLOUT_RADIUS;

    private static final int EXPOSURE_PER_SECOND = 20;
    private static final int DECAY_PER_SECOND = 10;
    private static final int DECAY_PER_SECOND_TREATED = 60;
    private static final int EFFECT_THRESHOLD = 400;
    private static final int PRUSSIAN_BLUE_SECONDS = 60;

    private static final Set<Marker> ACTIVE_FALLOUTS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<LivingEntity> TRACKED_IRRADIATED_ENTITIES = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<LivingEntity> IRRADIATED_THIS_TICK = Collections.newSetFromMap(new IdentityHashMap<>());

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;

        Entity entity = event.getEntity();

        if (entity instanceof Marker marker) {
            if (marker.getTags().contains(TAG_FALLOUT_MARKER)) {
                ACTIVE_FALLOUTS.add(marker);
            }
            return;
        }

        if (entity instanceof Player player) {
            if (player.getPersistentData().getInt(TAG_EXPOSURE) > 0) {
                TRACKED_IRRADIATED_ENTITIES.add(player);
            }
            return;
        }

        if (entity instanceof LivingEntity living && living.hasEffect(IRRADIATION.get())) {
            TRACKED_IRRADIATED_ENTITIES.add(living);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;

        long currentTime = serverLevel.getGameTime();
        if (currentTime % 20 != 0) return;
        if (ACTIVE_FALLOUTS.isEmpty() && TRACKED_IRRADIATED_ENTITIES.isEmpty()) return;

        IRRADIATED_THIS_TICK.clear();

        tickFalloutSources(serverLevel, currentTime);
        tickIrradiatedEntities(serverLevel);

        IRRADIATED_THIS_TICK.clear();
    }

    private static void tickFalloutSources(ServerLevel serverLevel, long currentTime) {
        Iterator<Marker> markerIterator = ACTIVE_FALLOUTS.iterator();
        while (markerIterator.hasNext()) {
            Marker marker = markerIterator.next();

            if (marker.isRemoved()) {
                markerIterator.remove();
                continue;
            }
            if (marker.level() != serverLevel) continue;

            if (currentTime > marker.getPersistentData().getLong(TAG_EXPIRY)) {
                marker.discard();
                markerIterator.remove();
                continue;
            }

            double mx = marker.getX();
            double my = marker.getY();
            double mz = marker.getZ();

            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, mx, my + 2.0, mz, 10, 8.0, 4.0, 8.0, 0.02);
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, mx, my + 1.0, mz, 10, 10.0, 3.0, 10.0, 0.02);
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH, mx, my + 10.0, mz, 20, 15.0, 10.0, 15.0, 0.0);

            AABB area = new AABB(
                    mx - FALLOUT_RADIUS, my - FALLOUT_RADIUS, mz - FALLOUT_RADIUS,
                    mx + FALLOUT_RADIUS, my + FALLOUT_RADIUS, mz + FALLOUT_RADIUS);

            List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e.distanceToSqr(mx, my, mz) <= FALLOUT_RADIUS_SQR);

            for (LivingEntity entity : victims) {
                IRRADIATED_THIS_TICK.add(entity);
                TRACKED_IRRADIATED_ENTITIES.add(entity);
            }
        }
    }

    private static void tickIrradiatedEntities(ServerLevel serverLevel) {
        Iterator<LivingEntity> entityIterator = TRACKED_IRRADIATED_ENTITIES.iterator();
        while (entityIterator.hasNext()) {
            LivingEntity entity = entityIterator.next();

            if (!entity.isAlive() || entity.isRemoved()) {
                entityIterator.remove();
                continue;
            }
            if (entity.level() != serverLevel) continue;

            CompoundTag data = entity.getPersistentData();
            int exposure = data.getInt(TAG_EXPOSURE);
            int oldExposure = exposure;
            boolean inFallout = IRRADIATED_THIS_TICK.contains(entity);

            int pbDuration = data.getInt(TAG_PRUSSIAN_BLUE);
            boolean hasPrussianBlue = pbDuration > 0;
            if (hasPrussianBlue) {
                data.putInt(TAG_PRUSSIAN_BLUE, pbDuration - 1);
            }

            if (inFallout) {
                if (!HazardProperty.ProtectionType.FULL.isProtected(entity)) {
                    exposure += EXPOSURE_PER_SECOND;
                }
            } else if (exposure > 0) {
                exposure = Math.max(0, exposure - (hasPrussianBlue ? DECAY_PER_SECOND_TREATED : DECAY_PER_SECOND));
            }

            if (exposure != oldExposure) {
                data.putInt(TAG_EXPOSURE, exposure);

                if (exposure < EFFECT_THRESHOLD && entity.hasEffect(IRRADIATION.get())) {
                    entity.removeEffect(IRRADIATION.get());
                }
            }

            if (exposure >= EFFECT_THRESHOLD) {
                int amp = (exposure >= 1200) ? 2 : ((exposure >= 800) ? 1 : 0);
                MobEffectInstance radiationEffect = new MobEffectInstance(IRRADIATION.get(), 120, amp, false, true, true);
                radiationEffect.getCurativeItems().clear();
                entity.addEffect(radiationEffect);
            }

            if (!inFallout && exposure <= 0 && !hasPrussianBlue) {
                entityIterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (event.getAmount() > 1.0F) return;
        if (event.getEntity().level().isClientSide) return;
        if (event.getEntity().hasEffect(IRRADIATION.get())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemConsumed(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity().level().isClientSide) return;

        ItemStack stack = event.getItem();
        if (stack.getItem() == ModItems.RADIOGARDASE_PILL.get()) {
            event.getEntity().getPersistentData().putInt(TAG_PRUSSIAN_BLUE, PRUSSIAN_BLUE_SECONDS);
        }
    }
}
