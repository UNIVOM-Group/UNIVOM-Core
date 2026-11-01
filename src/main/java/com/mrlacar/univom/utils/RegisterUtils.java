package com.mrlacar.univom.utils;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.ActiveBlock;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.api.registry.registrate.MachineBuilder;
import com.gregtechceu.gtceu.common.data.GTMedicalConditions;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.common.data.models.GTModels;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.mrlacar.univom.common.machine.multiblock.part.WirelessEnergyHatchPartMachine;
import com.mrlacar.univom.data.recipes.ModCustomTags;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties.IS_FORMED;
import static com.gregtechceu.gtceu.common.data.machines.GTMachineUtils.*;
import static com.gregtechceu.gtceu.utils.FormattingUtil.toEnglishName;
import static com.mrlacar.univom.api.registry.UNIVOMRegistration.REGISTRATE;


public class RegisterUtils {
    private static final Component[] NO_TOOLTIPS = new Component[0];

    //Item//
    public static ItemEntry<Item> registerDepletedFuelRod(String type) {
        String typeName = type.substring(0, 1).toUpperCase() + type.substring(1);
        return REGISTRATE.item("depleted_fuel_rod_" + type, Item::new)
                .lang("Depleted Fuel Rod" + typeName)
                .properties(p -> p.stacksTo(1))
                .tag(ModCustomTags.DEPLETED_FUEL_ROD)
                .register();
    }
    //Block//
    public static BlockEntry<Block> createCasingBlock(String name, ResourceLocation texture) {
        return createCasingBlock(name, Block::new, texture, () -> Blocks.IRON_BLOCK,
                () -> RenderType::solid);
    }

    public static BlockEntry<Block> createCasingBlock(String name,
                                                      NonNullFunction<BlockBehaviour.Properties, Block> blockSupplier,
                                                      ResourceLocation texture,
                                                      NonNullSupplier<? extends Block> properties,
                                                      Supplier<Supplier<RenderType>> type) {
        return REGISTRATE.block(name, blockSupplier)
                .initialProperties(properties)
                .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false))
                .addLayer(type)
                .exBlockstate(GTModels.cubeAllModel(texture))
                .tag(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WRENCH)
                .item(BlockItem::new)
                .build()
                .register();
    }

    public static BlockEntry<GlassBlock> createGlassCasingBlock(String name, ResourceLocation texture,
                                                                Supplier<Supplier<RenderType>> type) {
        return REGISTRATE.block(name, GlassBlock::new)
                .initialProperties(() -> Blocks.GLASS)
                .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false))
                .addLayer(type)
                .exBlockstate(GTModels.cubeAllModel(texture))
                .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .item(BlockItem::new)
                .build()
                .register();
    }

    public static BlockEntry<GlassBlock> createTransparentCasingBlock(String name, ResourceLocation texture,
                                                                      Supplier<Supplier<RenderType>> type) {
        return REGISTRATE.block(name, GlassBlock::new)
                .initialProperties(() -> Blocks.GLASS)
                .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false))
                .addLayer(type)
                .exBlockstate(GTModels.cubeAllModel(texture))
                .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .item(BlockItem::new)
                .build()
                .register();
    }

    public static BlockEntry<ActiveBlock> createActiveCasing(String name, String baseModelPath) {
        return REGISTRATE.block(name, ActiveBlock::new)
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .addLayer(() -> RenderType::cutoutMipped)
                .blockstate(GTModels.createActiveModel(GTCEu.id(baseModelPath)))
                .tag(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WRENCH)
                .item(BlockItem::new)
                .model((ctx, prov) -> prov.withExistingParent(prov.name(ctx), GTCEu.id(baseModelPath)))
                .build()
                .register();
    }
    //Machine//
    public static MachineDefinition[] registerWirelessEnergyHatch(IO io, int amperage, PartAbility ability, int[] tiers) {
        boolean isInput = io == IO.IN;

        String ioName = isInput ? "input" : "output";

        String ampSuffix = amperage == 2 ? "" : "_" + amperage + "a";

        return RegisterUtils.registerTieredMachines(REGISTRATE, "wireless_energy_" + ioName + "_hatch" + ampSuffix,
                (holder, tier) -> new WirelessEnergyHatchPartMachine(holder, tier, io, amperage),
                (tier, builder) -> builder
                        .rotationState(RotationState.ALL)
                        .abilities(ability)
                        .modelProperty(IS_FORMED, false)
                        .tooltips(
                                Component.translatable(isInput ? "gtceu.universal.tooltip.voltage_in" : "gtceu.universal.tooltip.voltage_out", FormattingUtil.formatNumbers(GTValues.V[tier]), GTValues.VNF[tier]),
                                Component.translatable(isInput ? "gtceu.universal.tooltip.amperage_in" : "gtceu.universal.tooltip.amperage_out", amperage),
                                Component.translatable(isInput ? "gtceu.machine.energy_hatch.input.tooltip" : "gtceu.machine.energy_hatch.output.tooltip")
                        )
                        .overlayTieredHullModel("energy_" + ioName + "_hatch")
                        .register(),
                tiers);
    }

    public static MachineDefinition[] registerSimpleMachines(GTRegistrate registrate, String name,
                                                             GTRecipeType recipeType,
                                                             Int2IntFunction tankScalingFunction,
                                                             boolean hasPollutionDebuff) {
        return registerSimpleMachines(registrate, name, recipeType, tankScalingFunction, hasPollutionDebuff,
                ELECTRIC_TIERS);
    }

    public static MachineDefinition[] registerSimpleMachines(String name, GTRecipeType recipeType) {
        return registerSimpleMachines(REGISTRATE, name, recipeType);
    }

    public static MachineDefinition[] registerSimpleMachines(GTRegistrate registrate, String name,
                                                             GTRecipeType recipeType) {
        return registerSimpleMachines(registrate, name, recipeType, defaultTankSizeFunction);
    }

    public static MachineDefinition[] registerSimpleMachines(GTRegistrate registrate, String name,
                                                             GTRecipeType recipeType,
                                                             Int2IntFunction tankScalingFunction) {
        return registerSimpleMachines(registrate, name, recipeType, tankScalingFunction, false);
    }

    public static MachineDefinition[] registerSimpleMachines(GTRegistrate registrate,
                                                             String name,
                                                             GTRecipeType recipeType,
                                                             Int2IntFunction tankScalingFunction,
                                                             boolean hasPollutionDebuff,
                                                             int... tiers) {
        return registerSimpleMachines(registrate, name, recipeType, tankScalingFunction, hasPollutionDebuff,
                (holder, tier) -> new SimpleTieredMachine(holder, tier, tankScalingFunction),
                GTRecipeModifiers.OC_NON_PERFECT, tier -> NO_TOOLTIPS, tiers);
    }

    public static MachineDefinition[] registerSimpleMachines(GTRegistrate registrate,
                                                             String name,
                                                             GTRecipeType recipeType,
                                                             Int2IntFunction tankScalingFunction,
                                                             boolean hasPollutionDebuff,
                                                             BiFunction<IMachineBlockEntity, Integer, MetaMachine> factory,
                                                             RecipeModifier recipeModifier,
                                                             Int2ObjectFunction<Component[]> extraTooltips,
                                                             int... tiers) {
        return registerTieredMachines(registrate, name, factory, (tier, builder) -> {
                    if (hasPollutionDebuff) {
                        builder.recipeModifiers(GTRecipeModifiers.ENVIRONMENT_REQUIREMENT
                                                .apply(GTMedicalConditions.CARBON_MONOXIDE_POISONING, 100 * tier),
                                        recipeModifier)
                                .conditionalTooltip(defaultEnvironmentRequirement(),
                                        ConfigHolder.INSTANCE.gameplay.environmentalHazards);
                    } else {
                        builder.recipeModifier(recipeModifier);
                    }
                    return builder
                            .langValue("%s %s %s".formatted(VLVH[tier], toEnglishName(name), VLVT[tier]))
                            .editableUI(SimpleTieredMachine.EDITABLE_UI_CREATOR.apply(GTCEu.id(name), recipeType))
                            .rotationState(RotationState.NON_Y_AXIS)
                            .recipeType(recipeType)
                            .workableTieredHullModel(GTCEu.id("block/machines/" + name))
                            .tooltips(workableTiered(tier, GTValues.V[tier], GTValues.V[tier] * 64, recipeType,
                                    tankScalingFunction.applyAsInt(tier), true))
                            .tooltips(extraTooltips.apply(tier))
                            .register();
                },
                tiers);
    }

    public static MachineDefinition[] registerTieredMachines(GTRegistrate registrate,
                                                             String name,
                                                             BiFunction<IMachineBlockEntity, Integer, MetaMachine> factory,
                                                             BiFunction<Integer, MachineBuilder<MachineDefinition, ?>, MachineDefinition> builder,
                                                             int... tiers) {
        MachineDefinition[] definitions = new MachineDefinition[GTValues.TIER_COUNT];
        for (int tier : tiers) {
            var register = registrate
                    .machine(GTValues.VN[tier].toLowerCase(Locale.ROOT) + "_" + name,
                            holder -> factory.apply(holder, tier))
                    .tier(tier);
            definitions[tier] = builder.apply(tier, register);
        }
        return definitions;
    }
}
