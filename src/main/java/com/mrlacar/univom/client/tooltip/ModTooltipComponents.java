package com.mrlacar.univom.client.tooltip;

import com.mrlacar.univom.UNIVOM;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UNIVOM.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class ModTooltipComponents {

    @SubscribeEvent
    public static void registerComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(MoleculeTooltip.class, ClientMoleculeTooltip::new);
        event.register(AlloyTooltip.class, ClientAlloyTooltip::new);
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(MoleculeAtlas.listener());
    }
}
