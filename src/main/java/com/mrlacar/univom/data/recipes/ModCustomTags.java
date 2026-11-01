package com.mrlacar.univom.data.recipes;

import com.mrlacar.univom.api.data.tag.ModTagUtil;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModCustomTags {
    public static final TagKey<Item> CONTROL_ROD = ModTagUtil.createModItemTag("control_rod");
    public static final TagKey<Item> DEPLETED_FUEL_ROD = ModTagUtil.createModItemTag("depleted_fuel_rod");
    public static final TagKey<Item> FUEL_ROD = ModTagUtil.createModItemTag("fuel_rod");
}
