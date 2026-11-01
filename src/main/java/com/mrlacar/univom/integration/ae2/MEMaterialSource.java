package com.mrlacar.univom.integration.ae2;

import com.mrlacar.univom.api.machine.builder.MaterialSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import org.jetbrains.annotations.Nullable;

public class MEMaterialSource implements MaterialSource {

    private static final Direction[] SIDES = Direction.values();

    private final MEStorage storage;
    private final IActionSource actionSource;

    private MEMaterialSource(MEStorage storage, IActionSource actionSource) {
        this.storage = storage;
        this.actionSource = actionSource;
    }

    @Nullable
    public static MEMaterialSource resolve(ServerPlayer player, @Nullable GlobalPos network) {
        IGrid grid = resolveGrid(player.server, network);
        if (grid == null) {
            return null;
        }
        IStorageService service = grid.getStorageService();
        return service == null ? null : new MEMaterialSource(service.getInventory(), IActionSource.ofPlayer(player));
    }

    @Nullable
    private static IGrid resolveGrid(MinecraftServer server, @Nullable GlobalPos network) {
        if (network == null) {
            return null;
        }
        ServerLevel level = server.getLevel(network.dimension());
        if (level == null) {
            return null;
        }
        BlockPos pos = network.pos();
        if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return null;
        }
        IInWorldGridNodeHost host = GridHelper.getNodeHost(level, pos);
        if (host == null) {
            return null;
        }
        for (Direction side : SIDES) {
            IGridNode node = host.getGridNode(side);
            if (node != null && node.isActive()) {
                return node.getGrid();
            }
        }
        return null;
    }

    @Override
    public int extract(ItemStack template, int amount, boolean simulate) {
        if (template.isEmpty() || amount <= 0) {
            return 0;
        }
        Actionable mode = simulate ? Actionable.SIMULATE : Actionable.MODULATE;
        return (int) storage.extract(AEItemKey.of(template), amount, mode, actionSource);
    }

    @Override
    public ItemStack give(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        AEItemKey key = AEItemKey.of(stack);
        if (key == null) {
            return stack;
        }
        long accepted = storage.insert(key, stack.getCount(), Actionable.SIMULATE, actionSource);
        if (accepted <= 0) {
            return stack;
        }
        long inserted = storage.insert(key, accepted, Actionable.MODULATE, actionSource);
        if (inserted >= stack.getCount()) {
            return ItemStack.EMPTY;
        }
        ItemStack remainder = stack.copy();
        remainder.setCount(stack.getCount() - (int) inserted);
        return remainder;
    }

}
