package com.mrlacar.univom.api.capability.forge;

import com.mrlacar.univom.api.capability.IBeamProvider;
import com.mrlacar.univom.api.capability.IBeamReceiver;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

public class ModCapability {
    public static final Capability<IBeamProvider> BEAM_PROVIDER = CapabilityManager.get(new CapabilityToken<>() {});

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(IBeamProvider.class);
        event.register(IBeamReceiver.class);
    }
}
