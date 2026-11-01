package com.mrlacar.univom.common.item;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.data.machines.GTAEMachines;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.ae2.machine.MEPatternBufferPartMachine;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import com.mrlacar.univom.common.data.machines.ModAEMachines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class AdvancedMEPatternBufferUpgradeBehaviour extends PatternBufferUpgradeBehaviour<MEPatternBufferPartMachine> {

    public static final AdvancedMEPatternBufferUpgradeBehaviour INSTANCE = new AdvancedMEPatternBufferUpgradeBehaviour();

    private AdvancedMEPatternBufferUpgradeBehaviour() {
        super("univom.tooltip.advanced_me_pattern_buffer_upgrade",
                "univom.message.advanced_me_pattern_buffer_upgrade.proxy_linked");
    }

    @Override
    protected @Nullable MEPatternBufferPartMachine source(@Nullable MetaMachine machine) {
        return machine instanceof MEPatternBufferPartMachine buffer &&
                buffer.getDefinition() == GTAEMachines.ME_PATTERN_BUFFER ? buffer : null;
    }

    @Override
    protected MachineDefinition target() {
        return ModAEMachines.ADVANCED_ME_PATTERN_BUFFER;
    }

    @Override
    protected boolean hasProxies(MEPatternBufferPartMachine buffer) {
        return !buffer.getProxies().isEmpty();
    }

    @Override
    protected CustomItemStackHandler patternInventory(MEPatternBufferPartMachine buffer) {
        return buffer.getPatternInventory();
    }

    @Override
    protected CustomItemStackHandler shareInventory(MEPatternBufferPartMachine buffer) {
        return buffer.getShareInventory().storage;
    }

    @Override
    protected ITagSerializable<CompoundTag>[] internalInventory(MEPatternBufferPartMachine buffer) {
        return buffer.getInternalInventory();
    }

    @Override
    protected ItemStack refund(MEPatternBufferPartMachine buffer) {
        return ConfigHolder.INSTANCE.machines.ghostCircuit ? ItemStack.EMPTY :
                buffer.getCircuitInventory().storage.getStackInSlot(0).copy();
    }
}
