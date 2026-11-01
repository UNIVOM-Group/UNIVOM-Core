package com.mrlacar.univom.data.player;

import com.mrlacar.univom.api.data.ChannelKey;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AEWirelessChannelSavedData extends SavedData {
    private static final String DATA_NAME = "ae2_wireless_channels";

    private final Map<ChannelKey, GlobalPos> activeChannels = new HashMap<>();

    private final Map<ChannelKey, Set<GlobalPos>> activeReceivers = new HashMap<>();

    @Nullable
    public static AEWirelessChannelSavedData get(MinecraftServer server) {
        if (server == null) return null;
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return null;
        return overworld.getDataStorage().computeIfAbsent(
                AEWirelessChannelSavedData::load,
                AEWirelessChannelSavedData::new,
                DATA_NAME
        );
    }

    public boolean registerChannel(ChannelKey key, GlobalPos pos) {
        if (key == null || key.isEmpty() || pos == null) return false;

        GlobalPos existing = activeChannels.get(key);
        if (existing != null && !existing.equals(pos)) {
            return false;
        }
        if (pos.equals(existing)) return true;

        activeChannels.put(key, pos);
        setDirty();
        return true;
    }

    public void unregisterChannel(ChannelKey key, GlobalPos owner) {
        if (key == null || key.isEmpty()) return;

        GlobalPos existing = activeChannels.get(key);
        if (existing == null) return;
        if (owner != null && !existing.equals(owner)) return;

        activeChannels.remove(key);
        setDirty();
    }

    public boolean isChannelOwnedBy(ChannelKey key, GlobalPos pos) {
        if (key == null || key.isEmpty() || pos == null) return false;
        return pos.equals(activeChannels.get(key));
    }

    @Nullable
    public GlobalPos getChannelPos(ChannelKey key) {
        if (key == null || key.isEmpty()) return null;
        return activeChannels.get(key);
    }

    public void registerReceiver(ChannelKey key, GlobalPos pos) {
        if (key == null || key.isEmpty() || pos == null) return;
        activeReceivers.computeIfAbsent(key, k -> new HashSet<>()).add(pos);
    }

    public void unregisterReceiver(ChannelKey key, GlobalPos pos) {
        if (key == null || key.isEmpty() || pos == null) return;
        Set<GlobalPos> set = activeReceivers.get(key);
        if (set != null) {
            set.remove(pos);
            if (set.isEmpty()) activeReceivers.remove(key);
        }
    }

    public int getReceiverCount(ChannelKey key) {
        if (key == null || key.isEmpty()) return 0;
        Set<GlobalPos> set = activeReceivers.get(key);
        return set == null ? 0 : set.size();
    }

    public static AEWirelessChannelSavedData load(CompoundTag tag) {
        AEWirelessChannelSavedData manager = new AEWirelessChannelSavedData();
        ListTag list = tag.getList("Channels", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ChannelKey key = ChannelKey.read(entry);
            if (key.isEmpty()) continue;
            GlobalPos pos = GlobalPos.CODEC.parse(NbtOps.INSTANCE, entry.get("Pos")).result().orElse(null);
            if (pos != null) manager.activeChannels.put(key, pos);
        }
        return manager;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<ChannelKey, GlobalPos> entry : activeChannels.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entry.getKey().write(entryTag);
            GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, entry.getValue())
                    .resultOrPartial(s -> {}).ifPresent(posTag -> entryTag.put("Pos", posTag));
            list.add(entryTag);
        }
        tag.put("Channels", list);
        return tag;
    }
}
