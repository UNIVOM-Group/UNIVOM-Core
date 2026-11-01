package com.mrlacar.univom.integration.jade;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.mrlacar.univom.common.block.PressurePipeBlock;
import com.mrlacar.univom.common.blockentity.PressurePipeBlockEntity;
import com.mrlacar.univom.integration.jade.provider.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class ModJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(new GridConnectedMachineProvider(), BlockEntity.class);
        registration.registerBlockDataProvider(new AEWirelessMachineProvider(), BlockEntity.class);
        registration.registerBlockDataProvider(new AstronomicalAnalysisStationProvider(), BlockEntity.class);
        registration.registerBlockDataProvider(new FissionReactorProvider(), BlockEntity.class);
        registration.registerBlockDataProvider(new ProbeRailgunProvider(), BlockEntity.class);
        registration.registerBlockDataProvider(new WirelessOpticalComputationHatchMachineProvider(), BlockEntity.class);
        registration.registerBlockDataProvider(new WirelessOpticalDataHatchMachineProvider(), BlockEntity.class);
        registration.registerBlockDataProvider(new MolecularConformationSynthesizerMEPatternBufferPartMachineProvider(), BlockEntity.class);
        registration.registerBlockDataProvider(new WirelessMEPatternBufferProvider(), BlockEntity.class);
        registration.registerBlockDataProvider(new WirelessMEPatternBufferProxyProvider(), BlockEntity.class);
        registration.registerBlockDataProvider(new PressureNetworkProvider(), PressurePipeBlockEntity.class);
        registration.registerBlockDataProvider(new GasCompressorProvider(), MetaMachineBlockEntity.class);
        registration.registerBlockDataProvider(new PressureChamberProvider(), MetaMachineBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new GridConnectedMachineProvider(), Block.class);
        registration.registerBlockComponent(new AEWirelessMachineProvider(), Block.class);
        registration.registerBlockComponent(new AstronomicalAnalysisStationProvider(), Block.class);
        registration.registerBlockComponent(new FissionReactorProvider(), Block.class);
        registration.registerBlockComponent(new ProbeRailgunProvider(), Block.class);
        registration.registerBlockComponent(new WirelessOpticalComputationHatchMachineProvider(), Block.class);
        registration.registerBlockComponent(new WirelessOpticalDataHatchMachineProvider(), Block.class);
        registration.registerBlockComponent(new MolecularConformationSynthesizerMEPatternBufferPartMachineProvider(), Block.class);
        registration.registerBlockComponent(new WirelessMEPatternBufferProvider(), Block.class);
        registration.registerBlockComponent(new WirelessMEPatternBufferProxyProvider(), Block.class);
        registration.registerBlockComponent(new PressureNetworkProvider(), PressurePipeBlock.class);
        registration.registerBlockComponent(new GasCompressorProvider(), MetaMachineBlock.class);
        registration.registerBlockComponent(new PressureChamberProvider(), MetaMachineBlock.class);
    }
}
