package com.mrlacar.univom.world.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModuleHighlightManager {

    public record Highlight(AABB box, long expiresAt, int color) {}

    private static final long HIGHLIGHT_DURATION_MS = 10_000L;
    private static final int HIGHLIGHT_COLOR = 0x00FF00;

    public static final Map<BlockPos, Highlight> HIGHLIGHT_POSITIONS = new ConcurrentHashMap<>();

    public static void addHighlight(BlockPos pos) {
        addHighlight(pos, new AABB(pos), HIGHLIGHT_DURATION_MS, HIGHLIGHT_COLOR);
    }

    public static void addHighlight(BlockPos key, AABB box, long durationMs, int color) {
        HIGHLIGHT_POSITIONS.put(key.immutable(), new Highlight(
                box.inflate(0.002D),
                System.currentTimeMillis() + durationMs,
                color));
    }

    public static void clear() {
        HIGHLIGHT_POSITIONS.clear();
    }
}
