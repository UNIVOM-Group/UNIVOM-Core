package com.mrlacar.univom.common.pipelike.pressure;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.world.level.Level;

public final class PressureMonitor {

    private static final ReferenceOpenHashSet<LevelPressurePipeNet> ACTIVE = new ReferenceOpenHashSet<>();
    private static final LevelPressurePipeNet[] EMPTY = new LevelPressurePipeNet[0];

    private PressureMonitor() {}

    static void activate(LevelPressurePipeNet data) {
        if (data.getWorld() != null) {
            ACTIVE.add(data);
        }
    }

    public static void tick(int serverTick) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        for (LevelPressurePipeNet data : ACTIVE.toArray(EMPTY)) {
            if (!data.tick(serverTick)) {
                ACTIVE.remove(data);
            }
        }
    }

    public static void unload(Level level) {
        ACTIVE.removeIf(data -> data.getWorld() == level);
    }
}
