package com.mrlacar.univom.mixin.adastra.client.screens;

import com.mrlacar.univom.client.forge.ClientTagCache;
import earth.terrarium.adastra.api.planets.Planet;
import earth.terrarium.adastra.client.components.LabeledImageButton;
import earth.terrarium.adastra.client.screens.PlanetsScreen;
import earth.terrarium.adastra.common.menus.PlanetsMenu;
import earth.terrarium.adastra.common.planets.AdAstraData;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mixin(PlanetsScreen.class)
public abstract class PlanetUnlockMixin extends AbstractContainerScreen<PlanetsMenu> {
    @Shadow(remap = false)
    private @Nullable ResourceLocation selectedSolarSystem;

    @Shadow(remap = false)
    private @Nullable Planet selectedPlanet;

    @Shadow(remap = false)
    private int pageIndex;

    @Shadow(remap = false)
    @Final
    private List<Button> buttons;

    public PlanetUnlockMixin(PlanetsMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Inject(method = "createPlanetButtons", at = @At("HEAD"), cancellable = true, remap = false)
    private void interceptCreatePlanetButtons(CallbackInfo ci) {
        PlanetsMenu menu = this.getMenu();

        for (Planet planet : menu.getSortedPlanets()) {
            if (!planet.isSpace() && menu.tier() >= planet.tier() && planet.solarSystem().equals(this.selectedSolarSystem)) {

                String planetInfo = planet.dimension().location().toString();

                boolean isUnlocked = planetInfo.equals("minecraft:overworld") || ClientTagCache.hasTag(planetInfo);

                Component displayText = isUnlocked ? menu.getPlanetName(planet.dimension()) : Component.literal("???");

                LabeledImageButton button = new LabeledImageButton(10, 0, 99, 20, 0, 0, 20, PlanetsScreen.BUTTON, 99, 40, (b) -> {
                    if (isUnlocked) {
                        this.pageIndex = 2;
                        this.selectedPlanet = planet;
                        this.rebuildWidgets();
                    }
                }, displayText);

                if (!isUnlocked) button.active = false;

                this.buttons.add(this.addWidget(button));
            }
        }
        ci.cancel();
    }

    @Inject(method = "createSolarSystemButtons", at = @At("HEAD"), cancellable = true, remap = false)
    private void interceptCreateSolarSystemButtons(CallbackInfo ci) {
        this.selectedSolarSystem = null;
        List<ResourceLocation> solarSystems = new ArrayList<>(AdAstraData.solarSystems());
        solarSystems.sort(Comparator.comparing(ResourceLocation::getPath));

        PlanetsMenu menu = this.getMenu();

        solarSystems.forEach((solarSystem) -> {

            String systemId = solarSystem.toString();
            boolean isUnlocked = systemId.equals("ad_astra:solar_system") || ClientTagCache.hasTag(systemId);

            if (!isUnlocked) {
                for (Planet planet : menu.getSortedPlanets()) {
                    if (planet.solarSystem().equals(solarSystem)) {
                        String planetInfo = planet.dimension().location().toString();

                        if (planetInfo.equals("minecraft:overworld") || ClientTagCache.hasTag(planetInfo)) {
                            isUnlocked = true;
                            break;
                        }
                    }
                }
            }

            final boolean finalIsUnlocked = isUnlocked;

            Component displayText = finalIsUnlocked ? Component.translatableWithFallback("solar_system.%s.%s".formatted(solarSystem.getNamespace(), solarSystem.getPath()), PlanetsScreen.title(solarSystem.getPath())) : Component.literal("???");

            LabeledImageButton button = new LabeledImageButton(10, 0, 99, 20, 0, 0, 20, PlanetsScreen.BUTTON, 99, 40, (b) -> {
                if (finalIsUnlocked) {
                    this.pageIndex = 1;
                    this.selectedSolarSystem = solarSystem;
                    this.rebuildWidgets();
                }
            }, displayText);

            if (!finalIsUnlocked) button.active = false;

            this.buttons.add(this.addWidget(button));
        });

        ci.cancel();
    }
}
