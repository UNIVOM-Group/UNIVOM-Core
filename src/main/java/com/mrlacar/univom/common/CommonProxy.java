package com.mrlacar.univom.common;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.event.MaterialEvent;
import com.gregtechceu.gtceu.api.data.chemical.material.event.MaterialRegistryEvent;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;
import com.gregtechceu.gtceu.common.unification.material.MaterialRegistryManager;
import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.api.capability.forge.ModCapability;
import com.mrlacar.univom.common.data.*;
import com.mrlacar.univom.config.ConfigHolder;
import com.mrlacar.univom.integration.adastra.SpaceFederationEnvironmentHandler;
import com.mrlacar.univom.integration.ae2.BuilderGridLinkableHandler;
import com.mrlacar.univom.network.ModNetwork;
import com.lowdragmc.lowdraglib.LDLib;
import appeng.api.features.GridLinkables;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import static com.mrlacar.univom.api.registry.UNIVOMRegistration.REGISTRATE;

@SuppressWarnings("removal")
public class CommonProxy {
    public CommonProxy() {
        CommonProxy.init();
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        REGISTRATE.registerEventListeners(eventBus);
        eventBus.addListener(this::networkSetup);
        eventBus.addListener(CommonProxy::registerMaterialRegistry);
        eventBus.addListener(CommonProxy::addMaterials);
        eventBus.addGenericListener(MachineDefinition.class, this::registerMachines);
        eventBus.addGenericListener(GTRecipeType.class, this::registerRecipeTypes);
        eventBus.addGenericListener(RecipeConditionType.class, CommonProxy::registerRecipeConditions);
    }

    public static void init() {
        ConfigHolder.init();
        REGISTRATE.registerRegistrate();
        ModItems.init();
        ModBlocks.init();
        ModBlockEntities.init();
        ModCreativeModeTabs.init();
        ModEffects.init();
        if (LDLib.isModLoaded("ad_astra")) {
            SpaceFederationEnvironmentHandler.init();
        }
    }

    private void networkSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
        event.enqueueWork(() -> GridLinkables.register(ModItems.MULTIBLOCK_MACHINE_BUILDER.get(),
                BuilderGridLinkableHandler.INSTANCE));
    }

    private static void registerMaterialRegistry(MaterialRegistryEvent event) {
        MaterialRegistryManager.getInstance().createRegistry(UNIVOM.MOD_ID);
    }

    private static void addMaterials(MaterialEvent event) {
        ModMaterials.init();
    }

    private void registerMachines(GTCEuAPI.RegisterEvent<ResourceLocation, MachineDefinition> event) {
        ModMachines.init();
    }

    private void registerRecipeTypes(GTCEuAPI.RegisterEvent<ResourceLocation, GTRecipeType> event) {
        ModRecipeTypes.init();
    }

    private static void registerRecipeConditions(GTCEuAPI.RegisterEvent<ResourceLocation, RecipeConditionType<?>> event) {
        ModRecipeConditions.init();
    }

    @SubscribeEvent
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        ModCapability.register(event);
    }
}
