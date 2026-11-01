package com.mrlacar.univom.api.machine.feature;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface IMachineConnectorInteractable {
    default InteractionResult onMachineConnectorUse(Player player, ItemStack machineConnector) {
        return InteractionResult.PASS;
    }

    default InteractionResult onMachineConnectorShiftUse(Player player, ItemStack machineConnector) {
        return InteractionResult.PASS;
    }
}
