package com.mrlacar.univom.world.player;

import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.network.ModNetwork;
import com.mrlacar.univom.network.packets.SyncUnlockTagPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.minecraftforge.network.PacketDistributor.*;

@Mod.EventBusSubscriber(modid = UNIVOM.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlanetUnlockManager {
    @SubscribeEvent
    public static void onPlayerPickup(EntityItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) checkAndUnlock(player, event.getItem().getItem());
    }

    @SubscribeEvent
    public static void onPlayerCraft(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) checkAndUnlock(player, event.getCrafting());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            for (String tag : player.getTags()) {
                if (tag.startsWith("ad_astra:") || tag.startsWith("univom:")) ModNetwork.CHANNEL.send(PLAYER.with(() -> player), new SyncUnlockTagPacket(tag));
            }
        }
    }

    private static void checkAndUnlock(ServerPlayer player, ItemStack itemStack) {
        if (itemStack.hasTag() && itemStack.getTag().contains("PlanetInfo")) {

            String targetTag = itemStack.getTag().getString("PlanetInfo");
            ResourceLocation planetInfo = ResourceLocation.tryParse(targetTag);

            if (!targetTag.isEmpty() && !player.getTags().contains(targetTag)) {

                player.addTag(targetTag);

                ModNetwork.CHANNEL.send(PLAYER.with(() -> player), new SyncUnlockTagPacket(targetTag));

                if (planetInfo != null) player.sendSystemMessage(Component.translatable("univom.message.planet_unlock").append(Component.translatable("dimension." + planetInfo.getNamespace() + "." + planetInfo.getPath())).withStyle(ChatFormatting.AQUA));
            }
        }
    }
}
