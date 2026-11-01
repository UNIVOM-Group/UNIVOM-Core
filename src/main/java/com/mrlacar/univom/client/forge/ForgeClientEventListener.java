package com.mrlacar.univom.client.forge;

import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.client.renderer.StructureBoxRenderHandler;
import com.mrlacar.univom.client.tooltip.MoleculeTooltipHandler;
import com.mrlacar.univom.world.client.ModuleHighlightManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UNIVOM.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class ForgeClientEventListener {
    @SubscribeEvent
    public static void onRenderWorldLast(RenderLevelStageEvent event) {
        var stage = event.getStage();
        if (stage == RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) {
            StructureBoxRenderHandler.renderStructureSelect(event.getPoseStack(), event.getCamera());
        }
    }

    @SubscribeEvent
    public static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
        MoleculeTooltipHandler.decorate(event.getItemStack(), event.getTooltipElements());
    }

    @SubscribeEvent
    public static void onClientLogout(LoggingOut event) {
        ClientTagCache.clear();
        ModuleHighlightManager.clear();
    }
}
