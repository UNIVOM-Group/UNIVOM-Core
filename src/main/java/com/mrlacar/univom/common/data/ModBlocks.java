package com.mrlacar.univom.common.data;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.block.ActiveBlock;
import com.gregtechceu.gtceu.api.item.PipeBlockItem;
import com.gregtechceu.gtceu.common.data.models.GTModels;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.common.block.BeamPipeBlock;
import com.mrlacar.univom.common.block.NetherPortalBlock;
import com.mrlacar.univom.common.block.PressurePipeBlock;
import com.mrlacar.univom.common.block.VentValveBlock;
import com.mrlacar.univom.common.item.PressurePipeBlockItem;
import com.mrlacar.univom.common.pipelike.beam.BeamPipeType;
import com.mrlacar.univom.common.pipelike.pressure.PressurePipeType;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlassBlock;

import static com.mrlacar.univom.api.registry.UNIVOMRegistration.REGISTRATE;
import static com.mrlacar.univom.utils.RegisterUtils.*;

public class ModBlocks {
    static {
        REGISTRATE.creativeModeTab(() -> ModCreativeModeTabs.BLOCK);
    }

    public static final BlockEntry<BeamPipeBlock>[] BEAM_PIPES = new BlockEntry[BeamPipeType.values().length];
    public static final BlockEntry<PressurePipeBlock>[] PRESSURE_PIPES = new BlockEntry[PressurePipeType.values().length];

    public static final BlockEntry<Block> CASING_NCCT_STEEL = createCasingBlock("ncct_steel_machine_casing",
            UNIVOM.id("block/casings/solid/machine_casing_ncct_steel"));
    public static final BlockEntry<Block> CASING_FISSION = createCasingBlock("fission_machine_casing",
            UNIVOM.id("block/casings/solid/machine_casing_fission"));
    public static final BlockEntry<Block> CASING_AGRICULTURE_SAFE = createCasingBlock("agriculture_safe_machine_casing",
            UNIVOM.id("block/casings/solid/machine_casing_agriculture_safe"));
    public static final BlockEntry<Block> CASING_FLUIX_TITANIUM_ALLOY = createCasingBlock("fluix_titanium_alloy_machine_casing",
            UNIVOM.id("block/casings/solid/machine_casing_fluix_titanium_alloy"));
    public static final BlockEntry<Block> CASING_RADIO_FREQUENCY_SOURCE = createCasingBlock("radio_frequency_source_machine_casing",
            UNIVOM.id("block/casings/solid/machine_casing_radio_frequency_source"));
    public static final BlockEntry<Block> CASING_SHIELDED_ACCELERATOR = createCasingBlock("shielded_accelerator_machine_casing",
            UNIVOM.id("block/casings/solid/machine_casing_shielded_accelerator"));
    public static final BlockEntry<Block> CASING_COLLIDER_MAIN_RING = createCasingBlock("collider_main_ring_machine_casing",
            UNIVOM.id("block/casings/solid/machine_casing_collider_main_ring"));
    public static final BlockEntry<Block> CASING_NIOBIUM_CAVITY = createCasingBlock("niobium_cavity_machine_casing",
            UNIVOM.id("block/casings/solid/machine_casing_niobium_cavity"));
    public static final BlockEntry<Block> CASING_MUON_TRACKING = createCasingBlock("muon_tracking_machine_casing",
            UNIVOM.id("block/casings/solid/machine_casing_muon_tracking"));
    public static final BlockEntry<Block> CASING_TOROIDAL_CALORIMETER = createCasingBlock("toroidal_calorimeter_machine_casing",
            UNIVOM.id("block/casings/solid/machine_casing_toroidal_calorimeter"));
    public static final BlockEntry<Block> CASING_HEAVY_ION_CONSTRAINT = createCasingBlock("heavy_ion_constraint_machine_casing",
            UNIVOM.id("block/casings/solid/machine_casing_heavy_ion_constraint"));
    public static final BlockEntry<Block> CASING_ANTIMATTER_RESOLUTION = createCasingBlock("antimatter_resolution_machine_casing",
            UNIVOM.id("block/casings/solid/machine_casing_antimatter_resolution"));
    public static final BlockEntry<Block> CASING_EXTREME_VACUUM_FROST_PROOF = createCasingBlock("extreme_vacuum_frostproof_machine_casing",
            UNIVOM.id("block/casings/solid/machine_casing_extreme_vacuum_frost_proof"));
    public static final BlockEntry<Block> CASING_SUPERCONDUCTING_MAGNETIC_TRAP = createCasingBlock("superconducting_magnetic_trap_machine_casing",
            UNIVOM.id("block/casings/solid/machine_casing_superconducting_magnetic_trap"));
    public static final BlockEntry<Block> CASING_REFRACTORY_TRANSMUTATION_BARRIER = createCasingBlock("refractory_transmutation_barrier_machine_casing",
            UNIVOM.id("block/casings/solid/machine_casing_refractory_transmutation_barrier"));
    public static final BlockEntry<Block> CASING_CYANEL_914 = createCasingBlock("cyanel_914_machine_casing",
            UNIVOM.id("block/casings/solid/machine_casing_cyanel_914"));
    public static final BlockEntry<Block> CASING_PRESSURE_CHAMBER = createCasingBlock("pressure_chamber_machine_casing",
            UNIVOM.id("block/casings/solid/machine_casing_pressure_chamber"));

    public static final BlockEntry<GlassBlock> CASING_FISSION_GLASS = createGlassCasingBlock("fission_glass",
            UNIVOM.id("block/casings/transparent/fission_glass"), () -> RenderType::cutoutMipped);
    public static final BlockEntry<GlassBlock> CASING_SHIELDED_ACCELERATOR_GLASS = createGlassCasingBlock("shielded_accelerator_glass",
            UNIVOM.id("block/casings/transparent/shielded_accelerator_glass"), () -> RenderType::translucent);

    public static final BlockEntry<ActiveBlock> MOB_CRUSHING_WHEELS = createActiveCasing("mob_crushing_wheels",
            "block/variant/mob_crushing_wheels");
    public static final BlockEntry<Block> ELECTROMAGNETIC_ACCELERATION_COIL_BLOCK = createCasingBlock("electromagnetic_acceleration_coil_block",
            UNIVOM.id("block/casings/unique/electromagnetic_acceleration_coil_block"));
    public static final BlockEntry<GlassBlock> PROTON_EXCHANGE_MEMBRANE_BLOCK = createTransparentCasingBlock("proton_exchange_membrane_block",
            UNIVOM.id("block/casings/unique/proton_exchange_membrane_block"), () -> RenderType::translucent);
    public static final BlockEntry<Block> FISSION_FUEL_ASSEMBLY = createCasingBlock("fission_fuel_assembly",
            UNIVOM.id("block/casings/unique/fission_fuel_assembly"));
    public static final BlockEntry<Block> INDUSTRIAL_MUD_BOX = createCasingBlock("industrial_mud_box",
            UNIVOM.id("block/casings/unique/industrial_mud_box"));
    public static final BlockEntry<Block> THERMIONIC_EMITTER_MODULE = createCasingBlock("thermionic_emitter_module",
            UNIVOM.id("block/casings/unique/thermionic_emitter_module"));
    public static final BlockEntry<Block> COOLANT_DELIVERY_MODULE = createCasingBlock("coolant_delivery_module",
            UNIVOM.id("block/casings/unique/coolant_delivery_module"));
    public static final BlockEntry<Block> ELECTROMAGNETIC_FOCUSING_LENS_MODULE = createCasingBlock("electromagnetic_focusing_lens_module",
            UNIVOM.id("block/casings/unique/electromagnetic_focusing_lens_module"));

    public static final BlockEntry<Block> FLUIX_RESONANCE_OBSIDIAN = REGISTRATE
            .block("fluix_resonance_obsidian", Block::new)
            .initialProperties(() -> Blocks.OBSIDIAN)
            .lang("Fluix Resonance Obsidian")
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .properties(p -> p.lightLevel(state -> 6))
            .defaultBlockstate()
            .simpleItem()
            .register();

    public static final BlockEntry<Block> INFERNAL_METALLOPHYTE_BLOCK = REGISTRATE
            .block("infernal_metallophyte_block", Block::new)
            .initialProperties(() -> Blocks.MOSS_BLOCK)
            .lang("Infernal Metallophyte Block")
            .tag(BlockTags.MINEABLE_WITH_SHOVEL)
            .defaultBlockstate()
            .simpleItem()
            .register();

    public static final BlockEntry<NetherPortalBlock> NETHER_PORTAL = REGISTRATE.block("nether_portal", NetherPortalBlock::new)
            .initialProperties(() -> Blocks.NETHER_PORTAL)
            .properties(p -> p.noCollission().lightLevel(state -> 11).noLootTable())
            .addLayer(() -> RenderType::translucent)
            .register();

    private static void generateBeamPipeBlocks() {
        GTCEu.LOGGER.debug("Generating UNIVOM Beam Pipe Blocks...");
        for (int i = 0; i < BeamPipeType.values().length; ++i) {
            registerBeamPipeBlock(i);
        }
        GTCEu.LOGGER.debug("Generating UNIVOM Beam Pipe Blocks... Complete!");
    }

    private static void registerBeamPipeBlock(int index) {
        var type = BeamPipeType.values()[index];
        var entry = REGISTRATE
                .block("%s_beam_pipe".formatted(type.getSerializedName()), (p) -> new BeamPipeBlock(p, type))
                .lang("Beam Fiber Cable")
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(p -> p.dynamicShape().noOcclusion().forceSolidOn())
                .gtBlockstate(GTModels::createPipeBlockModel)
                .defaultLoot()
                .tag(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WIRE_CUTTER)
                .addLayer(() -> RenderType::cutoutMipped)
                .addLayer(() -> RenderType::translucent)
                .color(() -> BeamPipeBlock::tintedColor)
                .item(PipeBlockItem::new)
                .model(NonNullBiConsumer.noop())
                .build()
                .register();
        BEAM_PIPES[index] = entry;
    }

    private static void generatePressurePipeBlocks() {
        GTCEu.LOGGER.debug("Generating UNIVOM Pressure Pipe Blocks...");
        for (int i = 0; i < PressurePipeType.values().length; ++i) {
            PRESSURE_PIPES[i] = PressurePipeType.values()[i].isPipe() ? registerPressurePipeBlock(i) :
                    registerPressureVesselBlock(i);
        }
        GTCEu.LOGGER.debug("Generating UNIVOM Pressure Pipe Blocks... Complete!");
    }

    private static BlockEntry<PressurePipeBlock> registerPressurePipeBlock(int index) {
        var type = PressurePipeType.values()[index];
        return REGISTRATE
                .block(type.getSerializedName(), (p) -> new PressurePipeBlock(p, type))
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(p -> p.dynamicShape().noOcclusion().forceSolidOn())
                .gtBlockstate(GTModels::createPipeBlockModel)
                .defaultLoot()
                .tag(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WRENCH)
                .addLayer(() -> RenderType::cutoutMipped)
                .color(() -> PressurePipeBlock::tintedColor)
                .item(PressurePipeBlockItem::new)
                .model(NonNullBiConsumer.noop())
                .color(() -> PressurePipeBlock::tintedItemColor)
                .build()
                .register();
    }

    private static BlockEntry<PressurePipeBlock> registerPressureVesselBlock(int index) {
        var type = PressurePipeType.values()[index];
        return REGISTRATE
                .block(type.getSerializedName(), (p) -> type.isVentValve() ? new VentValveBlock(p, type) :
                        new PressurePipeBlock(p, type))
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .blockstate(NonNullBiConsumer.noop())
                .defaultLoot()
                .tag(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WRENCH)
                .color(() -> PressurePipeBlock::tintedColor)
                .item(PressurePipeBlockItem::new)
                .model(NonNullBiConsumer.noop())
                .color(() -> PressurePipeBlock::tintedItemColor)
                .build()
                .register();
    }

    public static void init() {
        REGISTRATE.creativeModeTab(() -> ModCreativeModeTabs.BLOCK);
        generateBeamPipeBlocks();
        generatePressurePipeBlocks();
    }
}
