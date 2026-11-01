package com.mrlacar.univom.utils;

import com.lowdragmc.lowdraglib.LDLib;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WirelessEnergyNetworkUtils {
    public static final BigInteger BIG_INTEGER_MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);

    private static final long TEAM_CACHE_TTL_MS = 3000L;
    private static final Map<UUID, TeamEntry> TEAM_CACHE = new ConcurrentHashMap<>();

    private record TeamEntry(UUID teamId, long expiresAt) {}

    public static long getLongValue(BigInteger bigInt) {
        if (bigInt.compareTo(BIG_INTEGER_MAX_LONG) > 0) {
            return Long.MAX_VALUE;
        }
        return bigInt.longValue();
    }

    @Nullable
    public static UUID getTeamUUID(@Nullable UUID playerUUID) {
        if (playerUUID == null) return null;

        long now = System.currentTimeMillis();
        TeamEntry cached = TEAM_CACHE.get(playerUUID);
        if (cached != null && now < cached.expiresAt()) {
            return cached.teamId();
        }

        UUID resolved = resolveTeamUUID(playerUUID);
        TEAM_CACHE.put(playerUUID, new TeamEntry(resolved, now + TEAM_CACHE_TTL_MS));
        return resolved;
    }

    private static UUID resolveTeamUUID(UUID playerUUID) {
        if (LDLib.isModLoaded("ftbteams") && FTBTeamsAPI.api().isManagerLoaded()) {
            Optional<Team> team = FTBTeamsAPI.api().getManager().getTeamForPlayerID(playerUUID);
            return team.map(Team::getTeamId).orElse(playerUUID);
        }
        return playerUUID;
    }

    public static void clearTeamCache() {
        TEAM_CACHE.clear();
    }

    public static Component getPlayer(Player player) {
        if (LDLib.isModLoaded("ftbteams") && FTBTeamsAPI.api().isManagerLoaded()) {
            Optional<Team> team = FTBTeamsAPI.api().getManager().getTeamForPlayerID(player.getUUID());
            if (team.isPresent()) return team.get().getName();
        }
        return player.getName();
    }
}
