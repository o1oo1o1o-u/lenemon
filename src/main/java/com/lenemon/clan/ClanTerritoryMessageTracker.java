package com.lenemon.clan;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Detecte les transitions de claims pour afficher les messages d'entree/sortie.
 */
public final class ClanTerritoryMessageTracker {

    private static final Map<UUID, String> lastChunkKeys = new HashMap<>();
    private static final Map<UUID, Long> lastMessageAt = new HashMap<>();

    private ClanTerritoryMessageTracker() {}

    public static void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            String currentKey = currentChunkKey(player);
            String previousKey = lastChunkKeys.put(player.getUuid(), currentKey);
            if (currentKey.equals(previousKey)) continue;

            Clan previousClan = ownerFromKey(player, previousKey);
            Clan currentClan = ownerFromKey(player, currentKey);

            if (sameClan(previousClan, currentClan)) continue;
            if (!canSend(player.getUuid(), now)) continue;

            if (currentClan != null) {
                player.sendMessage(Text.literal(formatMessage(currentClan.enterMessage, currentClan)), false);
                lastMessageAt.put(player.getUuid(), now);
            } else if (previousClan != null) {
                player.sendMessage(Text.literal(formatMessage(previousClan.leaveMessage, previousClan)), false);
                lastMessageAt.put(player.getUuid(), now);
            }
        }
    }

    public static void onPlayerDisconnect(UUID uuid) {
        lastChunkKeys.remove(uuid);
        lastMessageAt.remove(uuid);
    }

    private static boolean canSend(UUID uuid, long now) {
        long cooldown = Math.max(0L, ClanConfig.get().territoryMessageCooldownMs);
        return now - lastMessageAt.getOrDefault(uuid, 0L) >= cooldown;
    }

    private static boolean sameClan(Clan a, Clan b) {
        if (a == null || b == null) return a == b;
        return a.id.equals(b.id);
    }

    private static String currentChunkKey(ServerPlayerEntity player) {
        ChunkPos chunkPos = player.getChunkPos();
        return ClanWorldData.chunkKey(player.getWorld().getRegistryKey(), chunkPos);
    }

    private static Clan ownerFromKey(ServerPlayerEntity player, String key) {
        if (key == null || key.isEmpty()) return null;
        ChunkCoords coords = parseChunkKey(key);
        if (coords == null) return null;
        return ClanWorldData.getChunkOwner(player.getWorld().getRegistryKey(), coords.x, coords.z);
    }

    private static ChunkCoords parseChunkKey(String key) {
        int lastColon = key.lastIndexOf(':');
        int prevColon = key.lastIndexOf(':', lastColon - 1);
        if (prevColon < 0 || lastColon < 0) return null;
        try {
            return new ChunkCoords(
                    Integer.parseInt(key.substring(prevColon + 1, lastColon)),
                    Integer.parseInt(key.substring(lastColon + 1))
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String formatMessage(String template, Clan clan) {
        String safe = template == null || template.isBlank()
                ? "Territoire du clan {clan}"
                : template;
        return translateColorCodes(
                safe.replace("{clan}", clan.name).replace("{tag}", clan.tag)
        );
    }

    private static String translateColorCodes(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '&' && i + 1 < value.length()) {
                char next = Character.toLowerCase(value.charAt(i + 1));
                if ((next >= '0' && next <= '9')
                        || (next >= 'a' && next <= 'f')
                        || (next >= 'k' && next <= 'o')
                        || next == 'r') {
                    out.append('§').append(next);
                    i++;
                    continue;
                }
            }
            out.append(current);
        }
        return out.toString();
    }

    private record ChunkCoords(int x, int z) {}
}
