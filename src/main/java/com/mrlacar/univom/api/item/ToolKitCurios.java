package com.mrlacar.univom.api.item;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

public final class ToolKitCurios {

    private ToolKitCurios() {}

    @Nullable
    public static ItemStack find(Player player) {
        if (!GTCEu.Mods.isCuriosLoaded()) {
            return null;
        }
        return CuriosApi.getCuriosInventory(player).resolve()
                .flatMap(inventory -> inventory.findFirstCurio(ToolKit::isToolKit))
                .map(SlotResult::stack)
                .orElse(null);
    }
}
