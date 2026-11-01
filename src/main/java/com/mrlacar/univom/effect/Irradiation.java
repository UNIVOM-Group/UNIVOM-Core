package com.mrlacar.univom.effect;

import com.mrlacar.univom.common.data.ModDamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class Irradiation extends MobEffect {
    public Irradiation() {
        super(MobEffectCategory.HARMFUL, 0x88FF00);
    }

    @Override
    public void applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;

        float damage = (amplifier + 1) * 2.0f;

        entity.hurt(ModDamageTypes.IRRADIATION.source(entity.level()), damage);

        entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, false));
        if (amplifier >= 1) {
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, false, false));
        }
        if (amplifier >= 2) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, false, false, false));
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        int tickInterval = (amplifier == 0) ? 60 : ((amplifier == 1) ? 40 : 20);
        return duration % tickInterval == 0;
    }
}
