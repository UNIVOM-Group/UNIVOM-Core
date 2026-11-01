package com.mrlacar.univom.client.tooltip;

import com.mrlacar.univom.api.chemistry.ChemSymbols;
import com.mrlacar.univom.api.chemistry.Molecule;
import com.mrlacar.univom.api.chemistry.MoleculeParser;
import com.mrlacar.univom.config.ConfigHolder;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialStack;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import it.unimi.dsi.fastutil.objects.ReferenceSets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class MoleculeAtlas {

    private static final Logger LOGGER = LogManager.getLogger("UNIVOM/Chemistry");
    private static final String MOLECULE_DIR = "molecules";
    private static final String ALLOY_DIR = "chemistry";
    private static final String SUFFIX = ".json";

    private static Reference2ObjectMap<Material, Molecule> molecules = Reference2ObjectMaps.emptyMap();
    private static ReferenceSet<Material> alloys = ReferenceSets.emptySet();
    private static ReferenceSet<Material> excluded = ReferenceSets.emptySet();

    private MoleculeAtlas() {}

    @Nullable
    public static Molecule molecule(Material material) {
        return molecules.get(material);
    }

    public static boolean isAlloy(Material material) {
        if (excluded.contains(material)) {
            return false;
        }
        if (alloys.contains(material)) {
            return true;
        }
        if (!ConfigHolder.INSTANCE.moleculeTooltipSetting.alloyAutoDetect) {
            return false;
        }
        ImmutableList<MaterialStack> components = material.getMaterialComponents();
        return components != null && components.size() >= 2;
    }

    public static SimplePreparableReloadListener<Loaded> listener() {
        return new Listener();
    }

    public record Loaded(Reference2ObjectMap<Material, Molecule> molecules, ReferenceSet<Material> alloys,
                         ReferenceSet<Material> excluded) {}

    private static final class Listener extends SimplePreparableReloadListener<Loaded> {

        @Override
        protected @NotNull Loaded prepare(@NotNull ResourceManager manager, @NotNull ProfilerFiller profiler) {
            Reference2ObjectMap<Material, Molecule> parsed = new Reference2ObjectOpenHashMap<>(256);
            ReferenceSet<Material> alloyList = new ReferenceOpenHashSet<>(64);
            ReferenceSet<Material> excludedList = new ReferenceOpenHashSet<>(16);
            readMolecules(manager, parsed);
            readAlloys(manager, alloyList, excludedList);
            return new Loaded(parsed, alloyList, excludedList);
        }

        @Override
        protected void apply(@NotNull Loaded loaded, @NotNull ResourceManager manager,
                             @NotNull ProfilerFiller profiler) {
            molecules = loaded.molecules();
            alloys = loaded.alloys();
            excluded = loaded.excluded();
            MoleculeLayout.invalidate();
            AlloyLayout.invalidate();
            AlloySlices.invalidate();
            ChemColor.invalidate();
            ChemSymbols.invalidate();
        }

        @Override
        public @NotNull String getName() {
            return "UNIVOM molecule drawings";
        }
    }

    private static void readMolecules(ResourceManager manager, Reference2ObjectMap<Material, Molecule> out) {
        Map<ResourceLocation, Resource> found = manager.listResources(MOLECULE_DIR,
                path -> path.getPath().endsWith(SUFFIX));
        for (Map.Entry<ResourceLocation, Resource> entry : found.entrySet()) {
            ResourceLocation file = entry.getKey();
            Material material = materialOf(file, MOLECULE_DIR);
            if (material == null) {
                continue;
            }
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonObject root = GsonHelper.parse(reader);
                Molecule molecule = MoleculeParser.parse(root);
                if (!molecule.isEmpty()) {
                    out.put(material, molecule);
                }
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Failed to load molecule {}", file, e);
            }
        }
    }

    private static void readAlloys(ResourceManager manager, ReferenceSet<Material> included,
                                   ReferenceSet<Material> excluded) {
        Map<ResourceLocation, Resource> found = manager.listResources(ALLOY_DIR,
                path -> path.getPath().endsWith("alloys" + SUFFIX));
        for (Map.Entry<ResourceLocation, Resource> entry : found.entrySet()) {
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (root.isJsonArray()) {
                    collect(root.getAsJsonArray(), included);
                    continue;
                }
                JsonObject object = root.getAsJsonObject();
                if (object.has("materials")) {
                    collect(GsonHelper.getAsJsonArray(object, "materials"), included);
                }
                if (object.has("excluded")) {
                    collect(GsonHelper.getAsJsonArray(object, "excluded"), excluded);
                }
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Failed to load alloy list {}", entry.getKey(), e);
            }
        }
    }

    private static void collect(JsonArray array, ReferenceSet<Material> out) {
        for (JsonElement element : array) {
            Material material = GTCEuAPI.materialManager.getMaterial(element.getAsString());
            if (material != null && !material.isNull()) {
                out.add(material);
            }
        }
    }

    @Nullable
    private static Material materialOf(ResourceLocation file, String directory) {
        String path = file.getPath();
        int start = directory.length() + 1;
        if (path.length() <= start + SUFFIX.length()) {
            return null;
        }
        String name = path.substring(start, path.length() - SUFFIX.length());
        Material material = GTCEuAPI.materialManager.getMaterial(file.getNamespace() + ":" + name);
        return material == null || material.isNull() ? null : material;
    }
}
