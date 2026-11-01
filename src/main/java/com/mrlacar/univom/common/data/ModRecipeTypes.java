package com.mrlacar.univom.common.data;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTSoundEntries;
import com.gregtechceu.gtceu.integration.xei.handlers.item.CycleItemStackHandler;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.mrlacar.univom.api.gui.ModGuiTextures;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.*;
import static com.lowdragmc.lowdraglib.gui.texture.ProgressTexture.FillDirection.LEFT_TO_RIGHT;
import static com.mrlacar.univom.api.ModValues.*;

public class ModRecipeTypes {
    //Simple//
    public final static GTRecipeType SUPERCONDUCTOR_ASSEMBLER_RECIPES = register("superconductor_assembler", ELECTRIC)
            .setMaxIOSize(6, 1, 3, 0)
            .setEUIO(IO.IN)
            .setSlotOverlay(false, false, GuiTextures.WIREMILL_OVERLAY)
            .setProgressBar(GuiTextures.PROGRESS_BAR_WIREMILL, LEFT_TO_RIGHT)
            .setSound(GTSoundEntries.MOTOR);
    public final static GTRecipeType OXYGEN_DISTRIBUTOR_RECIPES = register("oxygen_distributor", ELECTRIC)
            .setMaxIOSize(0, 0, 1, 0)
            .setEUIO(IO.IN)
            .setProgressBar(GuiTextures.PROGRESS_BAR_GAS_COLLECTOR, LEFT_TO_RIGHT)
            .setSound(GTSoundEntries.COOLING);
    //Multiblock//
    public final static GTRecipeType GREENHOUSE_RECIPES = register("greenhouse", MULTIBLOCK)
            .setMaxIOSize(6, 9, 3, 0)
            .setEUIO(IO.IN)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);

    public final static GTRecipeType REMELTING_FURNACE_RECIPES = register("remelting_furnace", MULTIBLOCK)
            .setMaxIOSize(1, 1, 0, 1)
            .setEUIO(IO.IN)
            .setSlotOverlay(false, false, GuiTextures.EXTRACTOR_OVERLAY)
            .setProgressBar(GuiTextures.PROGRESS_BAR_EXTRACT, LEFT_TO_RIGHT)
            .addDataInfo(data -> {
                int temp = data.getInt("ebf_temp");
                return LocalizationUtils.format("gtceu.recipe.temperature", FormattingUtil.formatTemperature(temp));
            })
            .addDataInfo(data -> {
                int temp = data.getInt("ebf_temp");
                ICoilType requiredCoil = ICoilType.getMinRequiredType(temp);

                if (requiredCoil != null && !requiredCoil.getMaterial().isNull()) {
                    return LocalizationUtils.format("gtceu.recipe.coil.tier",
                            I18n.get(requiredCoil.getMaterial().getUnlocalizedName()));
                }
                return "";
            })
            .setUiBuilder((recipe, widgetGroup) -> {
                int temp = recipe.data.getInt("ebf_temp");
                List<List<ItemStack>> items = new ArrayList<>();
                items.add(GTCEuAPI.HEATING_COILS.entrySet().stream()
                        .filter(coil -> coil.getKey().getCoilTemperature() >= temp)
                        .map(coil -> new ItemStack(coil.getValue().get())).toList());
                widgetGroup.addWidget(new SlotWidget(new CycleItemStackHandler(items), 0,
                        widgetGroup.getSize().width - 54, widgetGroup.getSize().height - 32, false, false));
            })
            .setSound(GTSoundEntries.FURNACE);

    public final static GTRecipeType CRYSTALLIZER_RECIPES = register("crystallizer", MULTIBLOCK)
            .setMaxIOSize(3, 3, 3, 3)
            .setEUIO(IO.IN)
            .addDataInfo(data -> {
                int temp = data.getInt("ebf_temp");
                return LocalizationUtils.format("gtceu.recipe.temperature", FormattingUtil.formatTemperature(temp));
            })
            .addDataInfo(data -> {
                int temp = data.getInt("ebf_temp");
                ICoilType requiredCoil = ICoilType.getMinRequiredType(temp);

                if (requiredCoil != null && !requiredCoil.getMaterial().isNull()) {
                    return LocalizationUtils.format("gtceu.recipe.coil.tier",
                            I18n.get(requiredCoil.getMaterial().getUnlocalizedName()));
                }
                return "";
            })
            .setUiBuilder((recipe, widgetGroup) -> {
                int temp = recipe.data.getInt("ebf_temp");
                List<List<ItemStack>> items = new ArrayList<>();
                items.add(GTCEuAPI.HEATING_COILS.entrySet().stream()
                        .filter(coil -> coil.getKey().getCoilTemperature() >= temp)
                        .map(coil -> new ItemStack(coil.getValue().get())).toList());
                widgetGroup.addWidget(new SlotWidget(new CycleItemStackHandler(items), 0,
                        widgetGroup.getSize().width - 25, widgetGroup.getSize().height - 32, false, false));
            })
            .setSound(GTSoundEntries.COOLING);

    public final static GTRecipeType PROBE_RAILGUN_RECIPES = register("probe_railgun", MULTIBLOCK)
            .setMaxIOSize(3, 0, 3, 0)
            .setEUIO(IO.IN)
            .addDataInfo(data -> {
                if (data.contains(SYSTEM)) {
                    String sysId = data.getString(SYSTEM);
                    String sysName = LocalizationUtils.format("univom.recipe.system." + sysId);
                    return LocalizationUtils.format("univom.recipe.system", sysName);
                }
                return "";
            })
            .addDataInfo(data -> {
                if (data.contains(PLANET)) {
                    String planetId = data.getString(PLANET);
                    String planetName = LocalizationUtils.format("univom.recipe.planet." + planetId);
                    return LocalizationUtils.format("univom.recipe.planet", planetName);
                }
                return "";
            })
            .setMaxTooltips(4);

    public final static GTRecipeType ELECTROCHEMICAL_DIALYSIS_ENGINE_RECIPES = register("electrochemical_dialysis_engine", MULTIBLOCK)
            .setMaxIOSize(0, 0, 2, 2)
            .setEUIO(IO.OUT)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT)
            .setSound(GTSoundEntries.CHEMICAL);

    public final static GTRecipeType RADIO_FREQUENCY_SOURCE_CHAMBER_RECIPES = register("radio_frequency_source_chamber", MULTIBLOCK)
            .setMaxIOSize(1, 0, 1, 0)
            .setEUIO(IO.IN)
            .addDataInfo(data -> {
                if (data.contains(PARTICLE)) {
                    String particleId = data.getString(PARTICLE);
                    String particleName = LocalizationUtils.format("particle.univom." + particleId);
                    return LocalizationUtils.format("univom.recipe.particle", particleName);
                }
                return "";
            })
            .addDataInfo(data -> {
                int flux = data.getInt(FLUX);
                return LocalizationUtils.format("univom.recipe.beam_flux", flux);
            })
            .addDataInfo(data -> {
                int maxEnergy = data.getInt(MAX_ENERGY);
                return LocalizationUtils.format("univom.recipe.beam_max_energy", maxEnergy);
            })
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT)
            .setSound(ModSoundEntries.RADIO_FREQUENCY_SOURCE_CHAMBER)
            .setMaxTooltips(4);

    public final static GTRecipeType LARGE_HADRON_COLLIDER_RECIPES = register("large_hadron_collider", MULTIBLOCK)
            .setMaxIOSize(1, 8, 0, 0)
            .setEUIO(IO.IN)
            .setProgressBar(ModGuiTextures.PROGRESS_BAR_LARGE_HADRON_COLLIDER, LEFT_TO_RIGHT);

    public final static GTRecipeType BEAM_CRAFTER_RECIPES = register("beam_directed_impact_chamber", MULTIBLOCK)
            .setMaxIOSize(4, 1, 4, 0)
            .setEUIO(IO.IN)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);

    public final static GTRecipeType EXXONMOBIL_CHEMICAL_PLANT_RECIPES = register("exxonmobil_chemical_plant", MULTIBLOCK)
            .setMaxIOSize(4, 4, 4, 4)
            .setEUIO(IO.IN)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT)
            .setSound(GTSoundEntries.COOLING);

    public final static GTRecipeType ORE_PROCESSOR_RECIPES = register("ore_processor", MULTIBLOCK)
            .setMaxIOSize(3, 9, 3, 0)
            .setEUIO(IO.IN)
            .setProgressBar(GuiTextures.PROGRESS_BAR_MACERATE, LEFT_TO_RIGHT)
            .setSound(GTSoundEntries.MACERATOR);

    public final static GTRecipeType PRESSURE_CHAMBER_RECIPES = register("pressure_chamber", MULTIBLOCK)
            .setMaxIOSize(6, 3, 0, 0)
            .setSlotOverlay(false, false, GuiTextures.COMPRESSOR_OVERLAY)
            .setProgressBar(GuiTextures.PROGRESS_BAR_COMPRESS, LEFT_TO_RIGHT)
            .setSound(GTSoundEntries.COMPRESSOR);

    public static void init() {}
}
