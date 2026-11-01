package com.mrlacar.univom.world.event;

import com.mrlacar.univom.UNIVOM;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UNIVOM.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DisableNetherPortalEvent {
    @SubscribeEvent
    public static void DisableNetherPortal(BlockEvent.PortalSpawnEvent event) {
        event.setCanceled(true);
    }
}
