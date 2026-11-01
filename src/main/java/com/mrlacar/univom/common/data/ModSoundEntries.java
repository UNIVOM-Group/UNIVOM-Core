package com.mrlacar.univom.common.data;

import com.gregtechceu.gtceu.api.sound.SoundEntry;

import static com.mrlacar.univom.api.registry.UNIVOMRegistration.REGISTRATE;

public class ModSoundEntries {
    public static final SoundEntry RADIO_FREQUENCY_SOURCE_CHAMBER = REGISTRATE.sound("radio_frequency_source_chamber").build();
    public static final SoundEntry PRESSURE_VENT = REGISTRATE.sound("pressure_vent").build();
    public static final SoundEntry PRESSURE_WARNING = REGISTRATE.sound("pressure_warning").build();

    public static void init() {}
}
