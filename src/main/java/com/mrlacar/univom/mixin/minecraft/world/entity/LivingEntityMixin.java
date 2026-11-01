package com.mrlacar.univom.mixin.minecraft.world.entity;

import com.mrlacar.univom.common.item.armor.IFederationArmor;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    protected abstract boolean onSoulSpeedBlock();

    @Inject(method = "getBlockSpeedFactor", at = @At("HEAD"), cancellable = true)
    private void univom$federationBootsIgnoreSoulSpeedBlocks(CallbackInfoReturnable<Float> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (IFederationArmor.isFederationPiece(entity.getItemBySlot(EquipmentSlot.FEET)) && onSoulSpeedBlock()) {
            cir.setReturnValue(1.0F);
        }
    }
}
