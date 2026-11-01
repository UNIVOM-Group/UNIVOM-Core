package com.mrlacar.univom.common.data;

import com.mrlacar.univom.common.blockentity.BeamPipeBlockEntity;
import com.mrlacar.univom.common.blockentity.PressurePipeBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

import static com.mrlacar.univom.api.registry.UNIVOMRegistration.REGISTRATE;

public class ModBlockEntities {
    public static final BlockEntityEntry<BeamPipeBlockEntity> BEAM_PIPE = REGISTRATE
            .blockEntity("beam_pipe", BeamPipeBlockEntity::new)
            .validBlocks(ModBlocks.BEAM_PIPES)
            .register();

    public static final BlockEntityEntry<PressurePipeBlockEntity> PRESSURE_PIPE = REGISTRATE
            .blockEntity("pressure_pipe", PressurePipeBlockEntity::new)
            .validBlocks(ModBlocks.PRESSURE_PIPES)
            .register();

    public static void init() {}
}
