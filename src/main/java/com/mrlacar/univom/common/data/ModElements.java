package com.mrlacar.univom.common.data;

import com.gregtechceu.gtceu.api.data.chemical.Element;

import static com.gregtechceu.gtceu.common.data.GTElements.createAndRegister;

public class ModElements {
    public static final Element SiliconSolarGrade = createAndRegister(14, 14, -1, null, "SiliconSolarGrade", "Si*", false);

    public static void init() {}
}
