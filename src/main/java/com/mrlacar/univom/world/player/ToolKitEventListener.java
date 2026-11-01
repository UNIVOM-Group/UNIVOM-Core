package com.mrlacar.univom.world.player;

import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.api.item.ToolKit;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UNIVOM.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ToolKitEventListener {

    private ToolKitEventListener() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(MetaMachine.getMachine(event.getLevel(), event.getPos()) instanceof IMaintenanceMachine maintenance) ||
                !maintenance.hasMaintenanceProblems()) {
            return;
        }
        ItemStack kit = ToolKit.findHeldOrWorn(player);
        if (kit == null) {
            return;
        }
        if (!repair(maintenance, kit, player)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static boolean repair(IMaintenanceMachine maintenance, ItemStack kit, ServerPlayer player) {
        boolean fixed = false;
        for (int slot = 0; slot < ToolKit.SLOTS; slot++) {
            if (((maintenance.getMaintenanceProblems() >> slot) & 1) != 0) {
                continue;
            }
            if (!ToolKit.hasUsableTool(kit, slot)) {
                continue;
            }
            maintenance.setMaintenanceFixed(slot);
            ToolKit.damageTool(kit, slot, player, 1);
            fixed = true;
        }
        if (fixed) {
            maintenance.setTaped(false);
        }
        return fixed;
    }
}
