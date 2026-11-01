package com.mrlacar.univom.common.data;

import com.gregtechceu.gtceu.common.data.GTCreativeModeTabs;
import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.common.data.machines.ModMultiMachines;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.world.item.CreativeModeTab;

import static com.mrlacar.univom.api.registry.UNIVOMRegistration.REGISTRATE;

public class ModCreativeModeTabs {
    public static final RegistryEntry<CreativeModeTab> ITEM = REGISTRATE
            .defaultCreativeTab("item", builder -> builder
                    .displayItems(new GTCreativeModeTabs.RegistrateDisplayItemsGenerator("item", REGISTRATE))
                    .title(REGISTRATE.addLang("itemGroup", UNIVOM.id("item"), UNIVOM.NAME + " Item"))
                    .icon(ModItems.FUEL_ROD_LEU::asStack)
                    .build())
            .register();

    public static final RegistryEntry<CreativeModeTab> BLOCK = REGISTRATE
            .defaultCreativeTab("block", builder -> builder
                    .displayItems(new GTCreativeModeTabs.RegistrateDisplayItemsGenerator("block", REGISTRATE))
                    .title(REGISTRATE.addLang("itemGroup", UNIVOM.id("block"), UNIVOM.NAME + " Block"))
                    .icon(ModBlocks.CASING_FISSION::asStack)
                    .build())
            .register();

    public static final RegistryEntry<CreativeModeTab> MACHINE = REGISTRATE
            .defaultCreativeTab("machine", builder -> builder
                    .displayItems(new GTCreativeModeTabs.RegistrateDisplayItemsGenerator("machine", REGISTRATE))
                    .title(REGISTRATE.addLang("itemGroup", UNIVOM.id("machine"), UNIVOM.NAME + " Machine"))
                    .icon(ModMultiMachines.PROBE_RAILGUN::asStack)
                    .build())
            .register();

    public static void init() {}
}
