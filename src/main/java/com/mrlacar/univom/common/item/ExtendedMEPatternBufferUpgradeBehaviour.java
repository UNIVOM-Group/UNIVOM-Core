package com.mrlacar.univom.common.item;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import com.mrlacar.univom.common.data.machines.ModAEMachines;
import com.mrlacar.univom.integration.ae2.machine.WirelessMEPatternBufferPartMachine;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public class ExtendedMEPatternBufferUpgradeBehaviour extends PatternBufferUpgradeBehaviour<WirelessMEPatternBufferPartMachine> {

    public static final ExtendedMEPatternBufferUpgradeBehaviour INSTANCE = new ExtendedMEPatternBufferUpgradeBehaviour();

    private ExtendedMEPatternBufferUpgradeBehaviour() {
        super("univom.tooltip.extended_me_pattern_buffer_upgrade",
                "univom.message.extended_me_pattern_buffer_upgrade.proxy_linked");
    }

    @Override
    protected @Nullable WirelessMEPatternBufferPartMachine source(@Nullable MetaMachine machine) {
        return machine instanceof WirelessMEPatternBufferPartMachine buffer &&
                buffer.getDefinition() == ModAEMachines.ADVANCED_ME_PATTERN_BUFFER ? buffer : null;
    }

    @Override
    protected MachineDefinition target() {
        return ModAEMachines.EXTENDED_ME_PATTERN_BUFFER;
    }

    @Override
    protected boolean hasProxies(WirelessMEPatternBufferPartMachine buffer) {
        return !buffer.getProxies().isEmpty();
    }

    @Override
    protected CustomItemStackHandler patternInventory(WirelessMEPatternBufferPartMachine buffer) {
        return buffer.getPatternInventory();
    }

    @Override
    protected CustomItemStackHandler shareInventory(WirelessMEPatternBufferPartMachine buffer) {
        return buffer.getShareInventory().storage;
    }

    @Override
    protected ITagSerializable<CompoundTag>[] internalInventory(WirelessMEPatternBufferPartMachine buffer) {
        return buffer.getInternalInventory();
    }
}
