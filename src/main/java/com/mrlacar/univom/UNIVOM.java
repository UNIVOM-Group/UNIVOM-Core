package com.mrlacar.univom;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.mrlacar.univom.api.registry.UNIVOMRegistration;
import com.mrlacar.univom.client.ClientProxy;
import com.mrlacar.univom.common.CommonProxy;
import com.mrlacar.univom.common.data.*;
import com.mrlacar.univom.data.recipes.RecipeRemoval;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

import java.util.function.Consumer;

@Mod(UNIVOM.MOD_ID)
public final class UNIVOM {
    public static final String MOD_ID = "univom";
    public static final String NAME = "UNIVOM";

    public UNIVOM() {
        DistExecutor.unsafeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static ResourceLocation id(String name) {
        return ResourceLocation.tryBuild(MOD_ID, name);
    }

    public static ResourceLocation location(String path) {
        return ResourceLocation.tryBuild(MOD_ID, path);
    }

    @GTAddon
    public static class MultiverseApocalypseAddon implements IGTAddon {

        @Override
        public GTRegistrate getRegistrate() {
            return UNIVOMRegistration.REGISTRATE;
        }

        @Override
        public boolean requiresHighTier() {
            return true;
        }

        @Override
        public void initializeAddon() {}

        @Override
        public String addonModId() {
            return MOD_ID;
        }

        @Override
        public void registerElements() {
            ModElements.init();
        }

        @Override
        public void registerCovers() {
            for (var covers : ModCovers.COVERS) {
                GTRegistries.COVERS.register(covers.getId(), covers);
            }
            ((ObjectArrayList<CoverDefinition>) ModCovers.COVERS).trim();
        }

        @Override
        public void addRecipes(Consumer<FinishedRecipe> provider) {
            ModRecipes.init(provider);
        }

        @Override
        public void removeRecipes(Consumer<ResourceLocation> consumer) {
            RecipeRemoval.init(consumer);
        }

        @Override
        public void registerSounds() {
            ModSoundEntries.init();
        }

        @Override
        public void registerRecipeCapabilities() {
            ModRecipeCapabilities.init();
        }
    }
}
