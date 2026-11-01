package com.mrlacar.univom.common.item.armor;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.item.armor.ArmorUtils;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.item.armor.QuarkTechSuite;
import com.gregtechceu.gtceu.core.IFireImmuneEntity;
import com.gregtechceu.gtceu.utils.input.SyncedKeyMappings;
import com.mrlacar.univom.UNIVOM;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.google.common.collect.Multimap;

import java.util.List;

public class SpaceFederationArmor extends QuarkTechSuite implements IFederationArmor {

    private static final ResourceLocation TEXTURE_1 = UNIVOM.id("textures/armor/space_federation_armor_1.png");
    private static final ResourceLocation TEXTURE_2 = UNIVOM.id("textures/armor/space_federation_armor_2.png");

    private float charge = 0.0F;
    private static final byte RUNNING_TIMER = 10;
    private static final byte JUMPING_TIMER = 10;
    private static final double LEGGING_ACCEL = 0.085D;

    public SpaceFederationArmor(ArmorItem.Type slot, int energyPerUse, long capacity, int tier) {
        super(slot, energyPerUse, capacity, tier);
    }

    @Override
    public void onArmorTick(Level world, Player player, ItemStack itemStack) {
        IElectricItem item = GTCapabilityHelper.getElectricItem(itemStack);
        if (item == null)
            return;

        CompoundTag data = itemStack.getOrCreateTag();
        byte toggleTimer = data.contains("toggleTimer") ? data.getByte("toggleTimer") : 0;
        int nightVisionTimer = data.contains("nightVisionTimer") ? data.getInt("nightVisionTimer") :
                ArmorUtils.NIGHTVISION_DURATION;
        byte runningTimer = data.contains("runningTimer") ? data.getByte("runningTimer") : RUNNING_TIMER;
        byte toggleBootsTimer = data.contains("toggleBootsTimer") ? data.getByte("toggleBootsTimer") : JUMPING_TIMER;

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestplate.is(GTItems.QUANTUM_CHESTPLATE.get()) &&
                !chestplate.is(GTItems.QUANTUM_CHESTPLATE_ADVANCED.get()) &&
                !IFederationArmor.isFederationPiece(chestplate)) {
            if (!world.isClientSide) ((IFireImmuneEntity) player).gtceu$setFireImmune(false);
        }

        boolean ret = false;
        if (type == ArmorItem.Type.HELMET) {

            if (!world.isClientSide) {
                ret = supplyAir(item, player) || supplyFood(item, player);
                removeNegativeEffects(item, player);
            }

            boolean nightVision = data.contains("nightVision") && data.getBoolean("nightVision");
            if (toggleTimer == 0 && SyncedKeyMappings.ARMOR_MODE_SWITCH.isKeyDown(player)) {
                nightVision = !nightVision;
                toggleTimer = 5;
                if (item.getCharge() < ArmorUtils.MIN_NIGHTVISION_CHARGE) {
                    nightVision = false;
                    player.displayClientMessage(Component.translatable("metaarmor.qts.nightvision.error"), true);
                } else {
                    player.displayClientMessage(Component
                            .translatable("metaarmor.qts.nightvision." + (nightVision ? "enabled" : "disabled")), true);
                }
            }

            if (nightVision) {
                player.removeEffect(MobEffects.BLINDNESS);
                if (nightVisionTimer <= ArmorUtils.NIGHT_VISION_RESET) {
                    nightVisionTimer = ArmorUtils.NIGHTVISION_DURATION;
                    player.addEffect(
                            new MobEffectInstance(MobEffects.NIGHT_VISION, ArmorUtils.NIGHTVISION_DURATION, 0, true,
                                    false));
                    item.discharge((4), this.tier, true, false, false);
                }
            } else {
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
            data.putBoolean("nightVision", nightVision);

            if (nightVisionTimer > 0) nightVisionTimer--;
            if (toggleTimer > 0) toggleTimer--;

            data.putInt("nightVisionTimer", nightVisionTimer);
            data.putByte("toggleTimer", toggleTimer);
        } else if (type == ArmorItem.Type.CHESTPLATE && !player.fireImmune()) {
            ((IFireImmuneEntity) player).gtceu$setFireImmune(true);
            if (player.isOnFire()) player.extinguishFire();
        } else if (type == ArmorItem.Type.LEGGINGS) {
            boolean canUseEnergy = item.canUse(energyPerUse / 100);
            boolean sprinting = SyncedKeyMappings.VANILLA_FORWARD.isKeyDown(player) && player.isSprinting();
            boolean jumping = SyncedKeyMappings.VANILLA_JUMP.isKeyDown(player);
            boolean sneaking = SyncedKeyMappings.VANILLA_SNEAK.isKeyDown(player);

            if (canUseEnergy && sprinting) {
                if (runningTimer == 0) {
                    runningTimer = RUNNING_TIMER;
                    item.discharge(energyPerUse / 100, item.getTier(), true, false, false);
                }
            }
            if (canUseEnergy && (player.onGround() || player.isInWater()) && sprinting) {
                float speed = 0.25F;
                if (player.isInWater()) {
                    speed = 0.1F;
                    if (jumping) {
                        player.push(0.0, 0.1, 0.0);
                        player.hurtMarked = true;
                    }
                }
                player.moveRelative(speed, new Vec3(0, 0, 1));
            } else if (canUseEnergy && player.isInWater() && (sneaking || jumping)) {
                if (sneaking)
                    player.push(0.0, -LEGGING_ACCEL, 0.0);
                if (jumping)
                    player.push(0.0, LEGGING_ACCEL, 0.0);
            }

            if (runningTimer > 0) runningTimer--;
            data.putByte("runningTimer", runningTimer);
        } else if (type == ArmorItem.Type.BOOTS) {
            boolean canUseEnergy = item.canUse(energyPerUse / 100);
            boolean jumping = SyncedKeyMappings.VANILLA_JUMP.isKeyDown(player);
            boolean boostedJump = data.contains("boostedJump") && data.getBoolean("boostedJump");
            if (toggleBootsTimer == 0 && SyncedKeyMappings.BOOTS_ENABLE.isKeyDown(player)) {
                boostedJump = !boostedJump;
                toggleBootsTimer = JUMPING_TIMER;
                player.displayClientMessage(Component
                        .translatable("metaarmor.qts.boosted_jump." + (boostedJump ? "enabled" : "disabled")), true);
            }
            if (boostedJump) {
                if (!world.isClientSide) {
                    boolean onGround = !data.contains("onGround") || data.getBoolean("onGround");
                    if (onGround && !player.onGround() && jumping) {
                        item.discharge(energyPerUse / 100, item.getTier(), true, false, false);
                        ret = true;
                    }

                    if (player.onGround() != onGround) {
                        data.putBoolean("onGround", player.onGround());
                    }
                } else {
                    if (canUseEnergy && player.onGround()) {
                        this.charge = 1.0F;
                    }

                    Vec3 delta = player.getDeltaMovement();
                    if (delta.y >= 0.0D && this.charge > 0.0F && !player.isInWater()) {
                        if (jumping) {
                            if (this.charge == 1.0F) {
                                player.setDeltaMovement(delta.x * 3.6D, delta.y, delta.z * 3.6D);
                            }
                            player.addDeltaMovement(new Vec3(0.0, this.charge * 0.32, 0.0));
                            this.charge = (float) (this.charge * 0.7D);
                        } else if (this.charge < 1.0F) {
                            this.charge = 0.0F;
                        }
                    }
                }
            }

            boolean stepAssist = data.contains("stepAssist") && data.getBoolean("stepAssist");
            if (toggleBootsTimer == 0 && SyncedKeyMappings.STEP_ASSIST_ENABLE.isKeyDown(player)) {
                stepAssist = !stepAssist;
                toggleBootsTimer = 5;
                if (world.isClientSide()) player.displayClientMessage(Component
                        .translatable("metaarmor.qts.step_assist." + (stepAssist ? "enabled" : "disabled")), true);
                data.putBoolean("stepAssist", stepAssist);
            }

            data.putBoolean("boostedJump", boostedJump);
            if (toggleBootsTimer > 0) toggleBootsTimer--;
            data.putInt("toggleBootsTimer", toggleBootsTimer);
        }

        if (ret) {
            player.inventoryMenu.sendAllDataToRemote();
        }
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        return IFederationArmor.addBonusAttributes(super.getAttributeModifiers(slot, stack), type);
    }

    @Override
    public int getArmorDisplay(Player player, ItemStack armor, EquipmentSlot slot) {
        return IFederationArmor.addBonusArmorDisplay(super.getArmorDisplay(player, armor, slot));
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return slot != EquipmentSlot.LEGS ? TEXTURE_1 : TEXTURE_2;
    }

    @Override
    public void addInfo(ItemStack itemStack, List<Component> lines) {
        super.addInfo(itemStack, lines);
        if (type == ArmorItem.Type.HELMET) {
            lines.add(Component.translatable("univom.tooltip.space_federation_armor.oxygen"));
        }
        lines.add(Component.translatable("univom.tooltip.space_federation_armor.full_set"));
    }
}
