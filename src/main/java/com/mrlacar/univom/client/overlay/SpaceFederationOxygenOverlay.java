package com.mrlacar.univom.client.overlay;

import com.mrlacar.univom.common.item.armor.IFederationArmor;
import com.mrlacar.univom.common.item.armor.SpaceFederationChestplateItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import com.mojang.blaze3d.vertex.PoseStack;
import earth.terrarium.adastra.api.systems.PlanetData;
import earth.terrarium.adastra.client.config.AdAstraConfigClient;
import earth.terrarium.adastra.client.utils.ClientData;

@OnlyIn(Dist.CLIENT)
public final class SpaceFederationOxygenOverlay {

    private static final ResourceLocation OXYGEN_TANK_EMPTY = new ResourceLocation("ad_astra",
            "textures/gui/sprites/overlay/oxygen_tank_empty.png");
    private static final ResourceLocation OXYGEN_TANK = new ResourceLocation("ad_astra",
            "textures/gui/sprites/overlay/oxygen_tank.png");

    private static final int TANK_WIDTH = 62;
    private static final int TANK_HEIGHT = 52;

    private static final int COLOR_EMPTY = 14423100;
    private static final int COLOR_NORMAL = 16777215;
    private static final int COLOR_BREATHABLE = 5635925;

    private SpaceFederationOxygenOverlay() {}

    public static final IGuiOverlay OXYGEN_BAR = (gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isSpectator() || minecraft.options.hideGui) {
            return;
        }
        if (!IFederationArmor.hasFullSet(player)) {
            return;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        int capacity = SpaceFederationChestplateItem.getOxygenCapacity(chestplate);
        if (capacity <= 0) {
            return;
        }

        double ratio = (double) SpaceFederationChestplateItem.getOxygenAmount(chestplate) / capacity;
        int filled = (int) (ratio * TANK_HEIGHT);
        int x = AdAstraConfigClient.oxygenBarX;
        int y = AdAstraConfigClient.oxygenBarY;
        float scale = AdAstraConfigClient.oxygenBarScale;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);

        guiGraphics.blit(OXYGEN_TANK_EMPTY, x, y, 0, 0, TANK_WIDTH, TANK_HEIGHT, TANK_WIDTH, TANK_HEIGHT);
        if (filled > 0) {
            guiGraphics.blit(OXYGEN_TANK, x, y + TANK_HEIGHT - filled, 0, TANK_HEIGHT - filled, TANK_WIDTH, filled,
                    TANK_WIDTH, TANK_HEIGHT);
        }

        String text = String.format("%.1f%%", ratio * 100);
        int color = ratio > 0 ? COLOR_NORMAL : COLOR_EMPTY;
        PlanetData data = ClientData.getLocalData();
        if (data != null && data.oxygen()) {
            color = COLOR_BREATHABLE;
        }
        guiGraphics.drawString(minecraft.font, text,
                (int) (x + (TANK_WIDTH - minecraft.font.width(text)) / 2f), y + TANK_HEIGHT + 3, color);

        poseStack.popPose();
    };
}
