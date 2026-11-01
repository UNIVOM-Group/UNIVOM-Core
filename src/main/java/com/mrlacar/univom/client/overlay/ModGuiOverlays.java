package com.mrlacar.univom.client.overlay;

import com.lowdragmc.lowdraglib.LDLib;
import com.mrlacar.univom.UNIVOM;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UNIVOM.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class ModGuiOverlays {

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        if (!LDLib.isModLoaded("ad_astra")) {
            return;
        }
        event.registerAbove(VanillaGuiOverlay.AIR_LEVEL.id(), "space_federation_oxygen",
                SpaceFederationOxygenOverlay.OXYGEN_BAR);
    }
}
