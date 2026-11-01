package com.mrlacar.univom.api.machine.builder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public final class BuilderModuleCategory {

    private final String name;
    private final String translationKey;
    private final Supplier<List<Block>> order;

    private volatile Resolved resolved;

    private BuilderModuleCategory(String name, Supplier<List<Block>> order) {
        this.name = name;
        this.translationKey = "univom.builder.module." + name;
        this.order = order;
    }

    public static BuilderModuleCategory of(String name, Supplier<List<Block>> order) {
        return new BuilderModuleCategory(name, order);
    }

    public String name() {
        return name;
    }

    public String translationKey() {
        return translationKey;
    }

    public List<Block> blocks() {
        return resolve().blocks;
    }

    public Block defaultBlock() {
        return resolve().blocks.get(0);
    }

    public String defaultBlockId() {
        return resolve().defaultId;
    }

    public boolean contains(Block block) {
        return resolve().lookup.contains(block);
    }

    @Nullable
    public Block byId(String id) {
        if (id.isEmpty()) {
            return null;
        }
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return null;
        }
        Block block = BuiltInRegistries.BLOCK.get(location);
        return contains(block) ? block : null;
    }

    public static String idOf(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private Resolved resolve() {
        Resolved current = resolved;
        if (current != null) {
            return current;
        }
        return build();
    }

    private synchronized Resolved build() {
        if (resolved != null) {
            return resolved;
        }
        List<Block> blocks = order.get();
        Resolved built = new Resolved(blocks, new ReferenceOpenHashSet<>(blocks), idOf(blocks.get(0)));
        resolved = built;
        return built;
    }

    private record Resolved(List<Block> blocks, Set<Block> lookup, String defaultId) {}
}
