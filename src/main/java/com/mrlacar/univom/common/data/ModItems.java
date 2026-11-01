package com.mrlacar.univom.common.data;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.item.armor.ArmorComponentItem;
import com.gregtechceu.gtceu.api.item.component.ElectricStats;
import com.gregtechceu.gtceu.api.item.component.IItemComponent;
import com.gregtechceu.gtceu.common.item.CoverPlaceBehavior;
import com.gregtechceu.gtceu.common.item.TooltipBehavior;
import com.gregtechceu.gtceu.common.item.armor.GTArmorMaterials;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.mrlacar.univom.api.data.particle.ParticleDefinition;
import com.mrlacar.univom.common.item.*;
import com.mrlacar.univom.common.item.armor.AdvancedSpaceFederationArmor;
import com.mrlacar.univom.common.item.armor.SpaceFederationArmor;
import com.mrlacar.univom.common.item.armor.SpaceFederationBootsItem;
import com.mrlacar.univom.common.item.armor.SpaceFederationChestplateItem;
import com.mrlacar.univom.config.ConfigHolder;
import com.mrlacar.univom.data.recipes.ModCustomTags;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.Tags;

import java.util.HashMap;
import java.util.Map;

import static com.mrlacar.univom.api.registry.UNIVOMRegistration.REGISTRATE;
import static com.mrlacar.univom.utils.RegisterUtils.registerDepletedFuelRod;

public class ModItems {
    static {
        REGISTRATE.creativeModeTab(() -> ModCreativeModeTabs.ITEM);
    }

    public static final Map<String, ItemEntry<ComponentItem>> PARTICLE_MODELS = new HashMap<>();

    public static ItemEntry<Item> ELECTRIC_MOTOR_ULV = REGISTRATE
            .item("ulv_electric_motor", Item::new)
            .lang("ULV Electric Motor")
            .tag(CustomTags.ELECTRIC_MOTORS)
            .register();

    public static ItemEntry<ComponentItem> ELECTRIC_PUMP_ULV = REGISTRATE
            .item("ulv_electric_pump", ComponentItem::create)
            .lang("ULV Electric Pump")
            .onRegister(attach(new CoverPlaceBehavior(ModCovers.ULV_PUMP)))
            .onRegister(attach(new TooltipBehavior(lines -> {
                lines.add(Component.translatable("item.gtceu.electric.pump.tooltip"));
                lines.add(Component.translatable("gtceu.universal.tooltip.fluid_transfer_rate",
                        FormattingUtil.formatNumbers(1280 / 20 / 2)));
            })))
            .tag(CustomTags.ELECTRIC_PUMPS)
            .register();

    public static ItemEntry<ComponentItem> FLUID_REGULATOR_ULV = REGISTRATE
            .item("ulv_fluid_regulator", ComponentItem::create)
            .lang("ULV Fluid Regulator")
            .onRegister(attach(new CoverPlaceBehavior(ModCovers.ULV_FLUID_REGULATOR)))
            .onRegister(attach(new TooltipBehavior(lines -> {
                lines.add(Component.translatable("item.gtceu.fluid.regulator.tooltip"));
                lines.add(Component.translatable("gtceu.universal.tooltip.fluid_transfer_rate", 1280 / 20 / 2));
            })))
            .tag(CustomTags.FLUID_REGULATORS)
            .register();

    public static ItemEntry<ComponentItem> CONVEYOR_MODULE_ULV = REGISTRATE
            .item("ulv_conveyor_module", ComponentItem::create)
            .lang("ULV Conveyor Module")
            .onRegister(attach(new CoverPlaceBehavior(ModCovers.ULV_CONVEYOR)))
            .onRegister(attach(new TooltipBehavior(lines -> {
                lines.add(Component.translatable("item.gtceu.conveyor.module.tooltip"));
                lines.add(Component.translatable("gtceu.universal.tooltip.item_transfer_rate", 2));
            })))
            .tag(CustomTags.CONVEYOR_MODULES)
            .register();

    public static ItemEntry<Item> ELECTRIC_PISTON_ULV = REGISTRATE
            .item("ulv_electric_piston", Item::new)
            .lang("ULV Electric Piston")
            .tag(CustomTags.ELECTRIC_PISTONS)
            .register();

    public static ItemEntry<ComponentItem> ROBOT_ARM_ULV = REGISTRATE
            .item("ulv_robot_arm", ComponentItem::create)
            .lang("ULV Robot Arm")
            .onRegister(attach(new CoverPlaceBehavior(ModCovers.ULV_ROBOT_ARM)))
            .onRegister(attach(new TooltipBehavior(lines -> {
                lines.add(Component.translatable("item.gtceu.robot.arm.tooltip"));
                lines.add(Component.translatable("gtceu.universal.tooltip.item_transfer_rate", 2));
            })))
            .tag(CustomTags.ROBOT_ARMS)
            .register();

    public static ItemEntry<Item> SILICON_SOLAR_GRADE_SEED_CRYSTAL = REGISTRATE
            .item("silicon_solar_grade_seed_crystal", Item::new)
            .lang("Silicon Solar Grade Seed Crystal")
            .register();

    public static final ItemEntry<SystemInfoCardBehaviour> SYSTEM_INFO_CARD = REGISTRATE
            .item("system_info_card", SystemInfoCardBehaviour::new)
            .lang("System Info Card")
            .properties(p -> p.stacksTo(1))
            .register();

    public static final ItemEntry<PlanetInfoCardBehaviour> PLANET_INFO_CARD = REGISTRATE
            .item("planet_info_card", PlanetInfoCardBehaviour::new)
            .lang("Planet Info Card")
            .properties(p -> p.stacksTo(1))
            .register();

    public static ItemEntry<Item> PROBE_CONTAINER = REGISTRATE
            .item("probe_container", Item::new)
            .lang("Probe Container")
            .register();

    public static ItemEntry<Item> BASIC_SURFACE_MAPPING_PROBE = REGISTRATE
            .item("basic_surface_mapping_probe", Item::new)
            .lang("Basic Surface Mapping Probe")
            .register();

    public static ItemEntry<Item> ADVANCED_MULTISPECTRAL_RECON_PROBE = REGISTRATE
            .item("advanced_multispectral_recon_probe", Item::new)
            .lang("Advanced Multispectral Recon Probe")
            .register();

    public static ItemEntry<Item> QUANTUM_TOPOLOGY_MAPPING_PROBE = REGISTRATE
            .item("quantum_topology_mapping_probe", Item::new)
            .lang("Quantum Topology Mapping Probe")
            .register();

    public static ItemEntry<Item> DEEP_SPACE_SURVEY_PROBE = REGISTRATE
            .item("deep_space_survey_probe", Item::new)
            .lang("Deep Space Survey Probe")
            .register();


    public static ItemEntry<ComponentItem> MACHINE_CONNECTOR = REGISTRATE
            .item("machine_connector", ComponentItem::create)
            .lang("Machine Connector")
            .onRegister(attach(ElectricStats.createElectricItem(100_000L, GTValues.LV), new MachineConnectorBehaviour()))
            .register();

    public static final ItemEntry<ComponentItem> CONTROL_ROD_GRAPHITE = REGISTRATE.item("control_rod_graphite", ComponentItem::create)
            .lang("Graphite Control Rod")
            .properties(p -> p.stacksTo(1))
            .tag(ModCustomTags.CONTROL_ROD)
            .onRegister(attach(new ControlRodBehaviour(0.5)))
            .register();

    public static final ItemEntry<ComponentItem> CONTROL_ROD_CADMIUM = REGISTRATE.item("control_rod_cadmium", ComponentItem::create)
            .lang("Cadmium Control Rod")
            .properties(p -> p.stacksTo(1))
            .tag(ModCustomTags.CONTROL_ROD)
            .onRegister(attach(new ControlRodBehaviour(0.8)))
            .register();

    public static final ItemEntry<ComponentItem> CONTROL_ROD_EUROPIUM = REGISTRATE.item("control_rod_europium", ComponentItem::create)
            .lang("Europium Control Rod")
            .properties(p -> p.stacksTo(1))
            .tag(ModCustomTags.CONTROL_ROD)
            .onRegister(attach(new ControlRodBehaviour(0.95)))
            .register();

    public static final ItemEntry<ComponentItem> CONTROL_ROD_NEUTRONIUM = REGISTRATE.item("control_rod_neutronium", ComponentItem::create)
            .lang("Neutronium Control Rod")
            .properties(p -> p.stacksTo(1))
            .tag(ModCustomTags.CONTROL_ROD)
            .onRegister(attach(new ControlRodBehaviour(1.0)))
            .register();

    public static final ItemEntry<Item> DEPLETED_FUEL_ROD_LEU = registerDepletedFuelRod("leu");
    public static final ItemEntry<Item> DEPLETED_FUEL_ROD_HDU = registerDepletedFuelRod("hdu");
    public static final ItemEntry<Item> DEPLETED_FUEL_ROD_HEU = registerDepletedFuelRod("heu");
    public static final ItemEntry<Item> DEPLETED_FUEL_ROD_MOX = registerDepletedFuelRod("mox");
    public static final ItemEntry<Item> DEPLETED_FUEL_ROD_HDP = registerDepletedFuelRod("hdp");
    public static final ItemEntry<Item> DEPLETED_FUEL_ROD_NQ_AM_SI = registerDepletedFuelRod("nq_am_si");
    public static final ItemEntry<Item> DEPLETED_FUEL_ROD_ENRICHED_NQ = registerDepletedFuelRod("enriched_nq");
    public static final ItemEntry<Item> DEPLETED_FUEL_ROD_NAQUADRIA = registerDepletedFuelRod("naquadria");

    public static final ItemEntry<ComponentItem> FUEL_ROD_LEU = REGISTRATE.item("fuel_rod_leu", ComponentItem::create)
            .lang("Low Enriched Uranium Fuel Rod")
            .properties(p -> p.stacksTo(1))
            .tag(ModCustomTags.FUEL_ROD)
            .onRegister(attach(new FuelRodBehaviour(250, 100000, DEPLETED_FUEL_ROD_LEU::asStack)))
            .register();

    public static final ItemEntry<ComponentItem> FUEL_ROD_HDU = REGISTRATE.item("fuel_rod_hdu", ComponentItem::create)
            .lang("High Density Uranium Fuel Rod")
            .properties(p -> p.stacksTo(1))
            .tag(ModCustomTags.FUEL_ROD)
            .onRegister(attach(new FuelRodBehaviour(400, 120000, DEPLETED_FUEL_ROD_HDU::asStack)))
            .register();

    public static final ItemEntry<ComponentItem> FUEL_ROD_HEU = REGISTRATE.item("fuel_rod_heu", ComponentItem::create)
            .lang("High Enriched Uranium Fuel Rod")
            .properties(p -> p.stacksTo(1))
            .tag(ModCustomTags.FUEL_ROD)
            .onRegister(attach(new FuelRodBehaviour(600, 80000, DEPLETED_FUEL_ROD_HEU::asStack)))
            .register();

    public static final ItemEntry<ComponentItem> FUEL_ROD_MOX = REGISTRATE.item("fuel_rod_mox", ComponentItem::create)
            .lang("MOX Fuel Rod")
            .properties(p -> p.stacksTo(1))
            .tag(ModCustomTags.FUEL_ROD)
            .onRegister(attach(new FuelRodBehaviour(850, 60000, DEPLETED_FUEL_ROD_MOX::asStack)))
            .register();

    public static final ItemEntry<ComponentItem> FUEL_ROD_HDP = REGISTRATE.item("fuel_rod_hdp", ComponentItem::create)
            .lang("High Density Plutonium Fuel Rod")
            .properties(p -> p.stacksTo(1))
            .tag(ModCustomTags.FUEL_ROD)
            .onRegister(attach(new FuelRodBehaviour(1200, 50000, DEPLETED_FUEL_ROD_HDP::asStack)))
            .register();

    public static final ItemEntry<ComponentItem> FUEL_ROD_NQ_AM_SI = REGISTRATE.item("fuel_rod_nq_am_si", ComponentItem::create)
            .lang("Americium-Silicide Naquadah Fuel Rod")
            .properties(p -> p.stacksTo(1))
            .tag(ModCustomTags.FUEL_ROD)
            .onRegister(attach(new FuelRodBehaviour(3300, 20000, DEPLETED_FUEL_ROD_NQ_AM_SI::asStack)))
            .register();

    public static final ItemEntry<ComponentItem> FUEL_ROD_ENRICHED_NQ = REGISTRATE.item("fuel_rod_enriched_nq", ComponentItem::create)
            .lang("Enriched Naquadah Fuel Rod")
            .properties(p -> p.stacksTo(1))
            .tag(ModCustomTags.FUEL_ROD)
            .onRegister(attach(new FuelRodBehaviour(5500, 15000, DEPLETED_FUEL_ROD_ENRICHED_NQ::asStack)))
            .register();

    public static final ItemEntry<ComponentItem> FUEL_ROD_NAQUADRIA = REGISTRATE.item("fuel_rod_naquadria", ComponentItem::create)
            .lang("Naquadria Fuel Rod")
            .properties(p -> p.stacksTo(1))
            .tag(ModCustomTags.FUEL_ROD)
            .onRegister(attach(new FuelRodBehaviour(8500, 10000, DEPLETED_FUEL_ROD_NAQUADRIA::asStack)))
            .register();

    public static ItemEntry<ComponentItem> RADIOGARDASE_PILL = REGISTRATE.item("radiogardase_pill", ComponentItem::create)
            .lang("Radiogardase Pill")
            .properties(p -> p.food(ModFoods.RADIOGARDASE))
            .onRegister(attach(new RadiogardasePillBehaviour()))
            .register();

    public static final ItemEntry<ComponentItem> MOB_CONTAINER = REGISTRATE
            .item("mob_container", ComponentItem::create)
            .lang("Mob Container")
            .properties(p -> p.stacksTo(1))
            .onRegister(attach(new MobContainerBehaviour()))
            .register();

    public static ItemEntry<Item> NANOGRID_NETHERITE_PRECURSOR_DUST = REGISTRATE
            .item("nanogrid_netherite_precursor_dust", Item::new)
            .lang("Nanogrid Netherite Precursor Dust")
            .register();

    public static ItemEntry<HotNetheriteIngotBehaviour> HOT_NETHERITE_INGOT = REGISTRATE
            .item("hot_netherite_ingot", HotNetheriteIngotBehaviour::new)
            .lang("Hot Netherite Ingot")
            .register();

    public static final ItemEntry<ComponentItem> INFINITE_SPRAY_CAN = REGISTRATE
            .item("infinite_spray_can", ComponentItem::create)
            .lang("Infinite Spray Can")
            .properties(p -> p.stacksTo(1))
            .onRegister(attach(new InfiniteColorSprayBehaviour()))
            .register();

    public static ItemEntry<ArmorComponentItem> SPACE_FEDERATION_HELMET = REGISTRATE
            .item("space_federation_helmet",
                    (p) -> new ArmorComponentItem(GTArmorMaterials.ARMOR, ArmorItem.Type.HELMET, p)
                            .setArmorLogic(new SpaceFederationArmor(ArmorItem.Type.HELMET,
                                    ConfigHolder.INSTANCE.spaceFederationArmorSetting.energyPerUse,
                                    ConfigHolder.INSTANCE.spaceFederationArmorSetting.getCapacity(),
                                    ConfigHolder.INSTANCE.spaceFederationArmorSetting.voltageTier)))
            .lang("Space Federation Armor Helmet")
            .properties(p -> p.rarity(Rarity.EPIC))
            .tag(Tags.Items.ARMORS_HELMETS)
            .tag(CustomTags.PPE_ARMOR)
            .register();

    public static ItemEntry<SpaceFederationChestplateItem> SPACE_FEDERATION_CHESTPLATE = REGISTRATE
            .item("space_federation_chestplate",
                    (p) -> (SpaceFederationChestplateItem) new SpaceFederationChestplateItem(GTArmorMaterials.ARMOR, p,
                            ConfigHolder.INSTANCE.spaceFederationArmorSetting.oxygenTankCapacity)
                            .setArmorLogic(new AdvancedSpaceFederationArmor(
                                    ConfigHolder.INSTANCE.spaceFederationArmorSetting.energyPerUse,
                                    ConfigHolder.INSTANCE.spaceFederationArmorSetting.getCapacity(),
                                    ConfigHolder.INSTANCE.spaceFederationArmorSetting.voltageTier)))
            .lang("Space Federation Armor Chestplate")
            .properties(p -> p.rarity(Rarity.EPIC))
            .tag(Tags.Items.ARMORS_CHESTPLATES)
            .tag(ItemTags.FREEZE_IMMUNE_WEARABLES)
            .tag(CustomTags.PPE_ARMOR)
            .register();

    public static ItemEntry<ArmorComponentItem> SPACE_FEDERATION_LEGGINGS = REGISTRATE
            .item("space_federation_leggings",
                    (p) -> new ArmorComponentItem(GTArmorMaterials.ARMOR, ArmorItem.Type.LEGGINGS, p)
                            .setArmorLogic(new SpaceFederationArmor(ArmorItem.Type.LEGGINGS,
                                    ConfigHolder.INSTANCE.spaceFederationArmorSetting.energyPerUse,
                                    ConfigHolder.INSTANCE.spaceFederationArmorSetting.getCapacity(),
                                    ConfigHolder.INSTANCE.spaceFederationArmorSetting.voltageTier)))
            .lang("Space Federation Armor Leggings")
            .properties(p -> p.rarity(Rarity.EPIC))
            .tag(Tags.Items.ARMORS_LEGGINGS)
            .tag(CustomTags.PPE_ARMOR)
            .register();

    public static ItemEntry<SpaceFederationBootsItem> SPACE_FEDERATION_BOOTS = REGISTRATE
            .item("space_federation_boots",
                    (p) -> (SpaceFederationBootsItem) new SpaceFederationBootsItem(GTArmorMaterials.ARMOR, p)
                            .setArmorLogic(new SpaceFederationArmor(ArmorItem.Type.BOOTS,
                                    ConfigHolder.INSTANCE.spaceFederationArmorSetting.energyPerUse,
                                    ConfigHolder.INSTANCE.spaceFederationArmorSetting.getCapacity(),
                                    ConfigHolder.INSTANCE.spaceFederationArmorSetting.voltageTier)))
            .lang("Space Federation Armor Boots")
            .properties(p -> p.rarity(Rarity.EPIC))
            .tag(Tags.Items.ARMORS_BOOTS)
            .tag(CustomTags.PPE_ARMOR)
            .tag(CustomTags.STEP_BOOTS)
            .register();

    public static ItemEntry<ComponentItem> STRUCTURE_WAND = REGISTRATE
            .item("structure_wand", ComponentItem::create)
            .lang("Structure Wand")
            .onRegister(attach(StructureWandBehaviour.INSTANCE))
            .register();

    public static ItemEntry<ComponentItem> TOOL_KIT = REGISTRATE
            .item("tool_kit", ComponentItem::create)
            .lang("Tool Kit")
            .properties(p -> p.stacksTo(1))
            .onRegister(attach(ToolKitBehaviour.INSTANCE))
            .register();

    public static ItemEntry<ComponentItem> MULTIBLOCK_MACHINE_BUILDER = REGISTRATE
            .item("multiblock_machine_builder", ComponentItem::create)
            .lang("Multiblock Machine Builder")
            .properties(p -> p.stacksTo(1))
            .onRegister(attach(MultiblockBuilderBehaviour.INSTANCE))
            .register();

    public static ItemEntry<FoilComponentItem> ADVANCED_ME_PATTERN_BUFFER_UPGRADE = REGISTRATE
            .item("advanced_me_pattern_buffer_upgrade", FoilComponentItem::new)
            .lang("Advanced ME Pattern Buffer Upgrade")
            .onRegister(attach(AdvancedMEPatternBufferUpgradeBehaviour.INSTANCE))
            .register();

    public static ItemEntry<FoilComponentItem> EXTENDED_ME_PATTERN_BUFFER_UPGRADE = REGISTRATE
            .item("extended_me_pattern_buffer_upgrade", FoilComponentItem::new)
            .lang("Extended ME Pattern Buffer Upgrade")
            .onRegister(attach(ExtendedMEPatternBufferUpgradeBehaviour.INSTANCE))
            .register();

    public static void init() {
        for (ParticleDefinition def : ModParticles.getAllParticles()) {
            String itemName = "model_" + def.name;

            ItemEntry<ComponentItem> entry = REGISTRATE.item(itemName, ComponentItem::create)
                    .lang(Component.translatable("particle.univom." + def.name) + " Model")
                    .onRegister(attach(new ParticleModelBehaviour(def)))
                    .register();

            PARTICLE_MODELS.put(def.name, entry);
        }
    }

    public static <T extends ComponentItem> NonNullConsumer<T> attach(IItemComponent... components) {
        return (item) -> item.attachComponents(components);
    }
}
