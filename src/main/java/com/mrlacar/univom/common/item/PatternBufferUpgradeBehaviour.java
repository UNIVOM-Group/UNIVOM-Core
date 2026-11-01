package com.mrlacar.univom.common.item;

import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.machine.MachineCoverContainer;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import com.lowdragmc.lowdraglib.syncdata.accessor.IManagedAccessor;
import com.lowdragmc.lowdraglib.syncdata.managed.IRef;
import com.mrlacar.univom.integration.ae2.machine.WirelessMEPatternBufferPartMachine;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public abstract class PatternBufferUpgradeBehaviour<T extends MetaMachine> implements IInteractionItem, IAddInformation {

    private static final Set<String> MANUAL_FIELDS = Set.of("patternInventory", "internalInventory", "proxies",
            "inventory", "circuitInventory");
    private static final Set<String> ROLLBACK_FIELDS = Set.of("patternInventory", "shareInventory", "coverContainer");

    private final String tooltip;
    private final String proxyLinked;

    protected PatternBufferUpgradeBehaviour(String tooltip, String proxyLinked) {
        this.tooltip = tooltip;
        this.proxyLinked = proxyLinked;
    }

    @Nullable
    protected abstract T source(@Nullable MetaMachine machine);

    protected abstract MachineDefinition target();

    protected abstract boolean hasProxies(T buffer);

    protected abstract CustomItemStackHandler patternInventory(T buffer);

    protected abstract CustomItemStackHandler shareInventory(T buffer);

    protected abstract ITagSerializable<CompoundTag>[] internalInventory(T buffer);

    protected ItemStack refund(T buffer) {
        return ItemStack.EMPTY;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !context.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        T buffer = source(MetaMachine.getMachine(level, pos));
        if (buffer == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!player.mayUseItemAt(pos, context.getClickedFace(), stack) ||
                !MachineOwner.canBreakOwnerMachine(player, buffer)) {
            return InteractionResult.FAIL;
        }
        if (hasProxies(buffer)) {
            player.displayClientMessage(Component.translatable(proxyLinked), true);
            return InteractionResult.FAIL;
        }
        if (!upgrade(level, pos, buffer, player)) {
            return InteractionResult.FAIL;
        }
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable(tooltip).withStyle(ChatFormatting.GRAY));
    }

    private boolean upgrade(Level level, BlockPos pos, T buffer, Player player) {
        CompoundTag data = IManagedAccessor.readManagedFields(buffer, new CompoundTag());
        ItemStack[] patterns = drain(patternInventory(buffer));
        ItemStack[] shared = drain(shareInventory(buffer));
        ITagSerializable<CompoundTag>[] slots = internalInventory(buffer);
        CompoundTag[] contents = new CompoundTag[slots.length];
        for (int i = 0; i < slots.length; i++) {
            contents[i] = slots[i].serializeNBT();
        }
        ItemStack refund = refund(buffer);
        MachineCoverContainer covers = buffer.getCoverContainer();
        for (Direction side : GTUtil.DIRECTIONS) {
            CoverBehavior cover = covers.getCoverAtSide(side);
            if (cover != null) {
                cover.onUnload();
                covers.setCoverAtSide(null, side);
            }
        }

        BlockState previous = level.getBlockState(pos);
        BlockState state = target().defaultBlockState();
        for (Property<?> property : previous.getProperties()) {
            if (state.hasProperty(property)) {
                state = copyProperty(previous, state, property);
            }
        }
        if (!level.setBlock(pos, state, Block.UPDATE_ALL)) {
            IManagedAccessor.writePersistedFields(data, select(buffer, ROLLBACK_FIELDS, true));
            covers.onLoad();
            return false;
        }
        if (!(MetaMachine.getMachine(level, pos) instanceof WirelessMEPatternBufferPartMachine upgraded)) {
            release(level, pos, patterns);
            release(level, pos, shared);
            return true;
        }

        IManagedAccessor.writePersistedFields(data, select(upgraded, MANUAL_FIELDS, false));
        CustomItemStackHandler inventory = upgraded.getPatternInventory();
        for (int i = 0; i < patterns.length; i++) {
            inventory.setStackInSlot(i, patterns[i]);
        }
        WirelessMEPatternBufferPartMachine.InternalSlot[] targets = upgraded.getInternalInventory();
        for (int i = 0; i < contents.length; i++) {
            targets[i].deserializeNBT(contents[i]);
        }
        upgraded.getCoverContainer().onLoad();
        upgraded.getSyncStorage().markAllDirty();
        upgraded.markDirty();

        if (!refund.isEmpty()) {
            player.getInventory().placeItemBackInInventory(refund);
        }
        return true;
    }

    private static ItemStack[] drain(CustomItemStackHandler handler) {
        ItemStack[] stacks = new ItemStack[handler.getSlots()];
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = handler.getStackInSlot(i);
            handler.setStackInSlot(i, ItemStack.EMPTY);
        }
        return stacks;
    }

    private static void release(Level level, BlockPos pos, ItemStack[] stacks) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
            }
        }
    }

    private static IRef[] select(MetaMachine machine, Set<String> names, boolean include) {
        return Arrays.stream(machine.getSyncStorage().getPersistedFields())
                .filter(ref -> names.contains(ref.getKey().getName()) == include)
                .toArray(IRef[]::new);
    }

    private static <P extends Comparable<P>> BlockState copyProperty(BlockState from, BlockState to,
                                                                     Property<P> property) {
        return to.setValue(property, from.getValue(property));
    }
}
