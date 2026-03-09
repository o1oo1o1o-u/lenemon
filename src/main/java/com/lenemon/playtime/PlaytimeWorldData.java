package com.lenemon.playtime;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlaytimeWorldData extends PersistentState {

    private static final String KEY = "lenemon_playtime_data";

    private final ConcurrentHashMap<UUID, PlayerProgress> players = new ConcurrentHashMap<>();

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();
        for (var entry : players.entrySet()) {
            NbtCompound playerNbt = new NbtCompound();
            playerNbt.putUuid("uuid", entry.getKey());
            playerNbt.putLong("playtimeTicks", entry.getValue().playtimeTicks);
            NbtList claimed = new NbtList();
            for (String tierId : entry.getValue().claimedTierIds) {
                claimed.add(NbtString.of(tierId));
            }
            playerNbt.put("claimedTierIds", claimed);
            list.add(playerNbt);
        }
        nbt.put("players", list);
        return nbt;
    }

    public static PlaytimeWorldData readNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
        PlaytimeWorldData data = new PlaytimeWorldData();
        NbtList list = nbt.getList("players", 10);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound playerNbt = list.getCompound(i);
            UUID uuid = playerNbt.getUuid("uuid");
            PlayerProgress progress = new PlayerProgress();
            progress.playtimeTicks = playerNbt.getLong("playtimeTicks");
            NbtList claimed = playerNbt.getList("claimedTierIds", 8);
            for (int j = 0; j < claimed.size(); j++) {
                progress.claimedTierIds.add(claimed.getString(j));
            }
            data.players.put(uuid, progress);
        }
        return data;
    }

    public static PlaytimeWorldData get(ServerWorld world) {
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(new Type<>(PlaytimeWorldData::new, PlaytimeWorldData::readNbt, null), KEY);
    }

    public long syncPlaytime(UUID uuid, long playtimeTicks) {
        PlayerProgress progress = players.computeIfAbsent(uuid, ignored -> new PlayerProgress());
        long sanitized = Math.max(progress.playtimeTicks, playtimeTicks);
        if (sanitized != progress.playtimeTicks) {
            progress.playtimeTicks = sanitized;
            markDirty();
        }
        return progress.playtimeTicks;
    }

    public long getPlaytimeTicks(UUID uuid) {
        return players.getOrDefault(uuid, new PlayerProgress()).playtimeTicks;
    }

    public boolean isClaimed(UUID uuid, String tierId) {
        return players.getOrDefault(uuid, new PlayerProgress()).claimedTierIds.contains(tierId);
    }

    public boolean markClaimed(UUID uuid, String tierId) {
        PlayerProgress progress = players.computeIfAbsent(uuid, ignored -> new PlayerProgress());
        boolean added = progress.claimedTierIds.add(tierId);
        if (added) markDirty();
        return added;
    }

    public boolean unclaim(UUID uuid, String tierId) {
        PlayerProgress progress = players.get(uuid);
        if (progress == null) return false;
        boolean removed = progress.claimedTierIds.remove(tierId);
        if (removed) markDirty();
        return removed;
    }

    public int clearClaims(UUID uuid) {
        PlayerProgress progress = players.get(uuid);
        if (progress == null) return 0;
        int count = progress.claimedTierIds.size();
        if (count > 0) {
            progress.claimedTierIds.clear();
            markDirty();
        }
        return count;
    }

    public int getClaimedCount(UUID uuid, List<String> tierIds) {
        PlayerProgress progress = players.get(uuid);
        if (progress == null) return 0;
        int claimed = 0;
        for (String tierId : tierIds) {
            if (progress.claimedTierIds.contains(tierId)) claimed++;
        }
        return claimed;
    }

    private static final class PlayerProgress {
        long playtimeTicks = 0L;
        Set<String> claimedTierIds = new LinkedHashSet<>();
    }
}
