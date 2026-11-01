package com.mrlacar.univom.world.event.forge;

import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.common.pipelike.pressure.PressureMonitor;
import com.mrlacar.univom.data.player.WirelessEnergyNetworkSavedData;
import com.mrlacar.univom.network.wireless.WirelessEnergyNetwork;
import com.mrlacar.univom.utils.WirelessEnergyNetworkUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UNIVOM.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeCommonEventListener {
    @SubscribeEvent
    public static void onServerTickEvent(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        PressureMonitor.tick(event.getServer().getTickCount());
        if (event.getServer().getTickCount() % 20 != 0) return;

        WirelessEnergyNetworkSavedData data = WirelessEnergyNetworkSavedData.INSTANCE;
        if (data == null) return;

        boolean needsSave = false;
        for (WirelessEnergyNetwork network : data.networkMap.values()) {
            network.updateStats();
            if (network.getCurrentAdded() > 0 || network.getCurrentRemoved() > 0) {
                needsSave = true;
            }
        }
        if (needsSave) {
            data.setDirty();
        }
    }

    @SubscribeEvent
    public static void serverSetup(LevelEvent.Load event) {
        if (WirelessEnergyNetworkSavedData.INSTANCE != null) return;
        if (event.getLevel() instanceof ServerLevel level) {
            ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
            if (overworld != null) {
                WirelessEnergyNetworkSavedData.INSTANCE = WirelessEnergyNetworkSavedData.getOrCreate(overworld);
            }
        }
    }

    @SubscribeEvent
    public static void serverTearDown(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            PressureMonitor.unload(level);
            if (level.dimension() == Level.OVERWORLD) {
                WirelessEnergyNetworkSavedData.INSTANCE = null;
                WirelessEnergyNetworkUtils.clearTeamCache();
            }
        }
    }
}
