package com.mrlacar.univom.client.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashSet;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class ClientTagCache {
    private static final Set<String> UNLOCKED_PLANET_TAGS = new HashSet<>();

    public static void addTag(String tag) {
        UNLOCKED_PLANET_TAGS.add(tag);
    }

    public static boolean hasTag(String tag) {
        return UNLOCKED_PLANET_TAGS.contains(tag);
    }

    public static void clear() {
        UNLOCKED_PLANET_TAGS.clear();
    }
}
