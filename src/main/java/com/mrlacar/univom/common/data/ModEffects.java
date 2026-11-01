package com.mrlacar.univom.common.data;

import com.mrlacar.univom.effect.Irradiation;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.registries.Registries;

import static com.mrlacar.univom.api.registry.UNIVOMRegistration.REGISTRATE;

public class ModEffects {
    public static final RegistryEntry<Irradiation> IRRADIATION = REGISTRATE.simple(
            "irradiation", Registries.MOB_EFFECT, Irradiation::new);

    public static void init() {}
}
