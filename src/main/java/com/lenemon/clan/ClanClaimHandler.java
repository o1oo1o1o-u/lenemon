package com.lenemon.clan;

import com.lenemon.network.clan.ClanClaimActionPayload;
import com.lenemon.network.clan.ClanClaimMapPayload;
import com.lenemon.network.clan.ClanClaimModePayload;
import com.lenemon.network.clan.ClanClaimResultPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.*;

/**
 * Logique metier du systeme de claim de territoire pour les clans.
 * Gere le mode claim, la validation, l'achat de level economique.
 *
 * Toutes les methodes publiques doivent etre appelees depuis le thread serveur.
 */
public class ClanClaimHandler {

    private static final String PREFIX = "§6[Clan] §r";
    private static final String ERROR  = "§c[Clan] §r";

    /** Joueurs actuellement en mode claim actif. */
    private static final Set<UUID> claimModePlayers = new HashSet<>();

    private ClanClaimHandler() {}

    // -------------------------------------------------------------------------
    // Mode Claim
    // -------------------------------------------------------------------------

    /**
     * Toggle le mode claim pour le joueur.
     * Si deja actif, le desactive. Sinon l'active.
     */
    public static void toggleClaimMode(ServerPlayerEntity player, MinecraftServer server) {
        UUID uuid = player.getUuid();
        if (claimModePlayers.contains(uuid)) {
            exitClaimMode(player);
        } else {
            enterClaimMode(player, server);
        }
    }

    public static boolean isInClaimMode(UUID uuid) {
        return claimModePlayers.contains(uuid);
    }

    public static void enterClaimMode(ServerPlayerEntity player, MinecraftServer server) {
        UUID uuid = player.getUuid();
        Clan clan = ClanWorldData.getClanOf(uuid);
        if (clan == null) {
            player.sendMessage(Text.literal(ERROR + "Tu n'es dans aucun clan."), false);
            return;
        }

        if (!hasPermission(clan, uuid, "claim")) {
            player.sendMessage(Text.literal(ERROR + "Tu n'as pas la permission de claimer des chunks."), false);
            return;
        }

        if (!isAllowedDimension(player)) {
            player.sendMessage(Text.literal(ERROR + "Tu ne peux claimer que dans l'Overworld."), false);
            return;
        }

        claimModePlayers.add(uuid);

        ServerPlayNetworking.send(player, new ClanClaimModePayload(
                true, clan.maxClaims(), clan.claimedChunks.size()));

        sendClaimMap(player, clan);

        player.sendMessage(Text.literal(PREFIX +
                "Mode claim active. Deplace-toi sur un chunk puis :"), false);
        player.sendMessage(Text.literal(
                "§e/clan claim §7- Claimer | §e/clan unclaim §7- Retirer | §e/clan claim exit §7- Quitter"), false);
    }

    /**
     * Quitte le mode claim.
     */
    public static void exitClaimMode(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        if (!claimModePlayers.remove(uuid)) return;
        ServerPlayNetworking.send(player, new ClanClaimModePayload(false, 0, 0));
        player.sendMessage(Text.literal(PREFIX + "Mode claim desactive."), false);
    }

    /**
     * Appele a la deconnexion pour nettoyer l'etat du joueur.
     */
    public static void onPlayerDisconnect(UUID uuid) {
        claimModePlayers.remove(uuid);
    }

    // -------------------------------------------------------------------------
    // Claim / Unclaim
    // -------------------------------------------------------------------------

    /**
     * Claim le chunk sous les pieds du joueur.
     */
    public static void claimCurrentChunk(ServerPlayerEntity player, MinecraftServer server) {
        UUID uuid = player.getUuid();
        Clan clan = ClanWorldData.getClanOf(uuid);
        if (clan == null) { sendError(player, "Tu n'es dans aucun clan."); return; }
        if (!hasPermission(clan, uuid, "claim")) { sendError(player, "Permission insuffisante."); return; }
        if (!isAllowedDimension(player)) { sendError(player, "Claims uniquement en Overworld."); return; }

        RegistryKey<World> dim = player.getWorld().getRegistryKey();
        ChunkPos chunkPos = player.getChunkPos();
        String key = ClanWorldData.chunkKey(dim, chunkPos);

        String error = validateClaim(clan, dim, chunkPos, key);
        if (error != null) {
            sendClaimResult(player, false, error, clan, chunkPos, false);
            return;
        }

        ClanWorldData.claimChunk(clan.id, dim, chunkPos);
        sendClaimResult(player, true,
                PREFIX + "Chunk §e[" + chunkPos.x + ", " + chunkPos.z + "] §rclaim avec succes !",
                clan, chunkPos, true);

        if (claimModePlayers.contains(uuid)) {
            sendClaimMap(player, clan);
        }
    }

    /**
     * Unclaim le chunk sous les pieds du joueur.
     */
    public static void unclaimCurrentChunk(ServerPlayerEntity player, MinecraftServer server) {
        UUID uuid = player.getUuid();
        Clan clan = ClanWorldData.getClanOf(uuid);
        if (clan == null) { sendError(player, "Tu n'es dans aucun clan."); return; }
        if (!hasPermission(clan, uuid, "claim")) { sendError(player, "Permission insuffisante."); return; }

        RegistryKey<World> dim = player.getWorld().getRegistryKey();
        ChunkPos chunkPos = player.getChunkPos();
        String key = ClanWorldData.chunkKey(dim, chunkPos);

        if (!clan.claimedChunks.contains(key)) {
            sendError(player, "Ce chunk n'appartient pas a ton clan.");
            return;
        }

        if (!wouldRemainContiguous(clan, key)) {
            sendError(player, "Retirer ce chunk briserait la connexite du territoire.");
            return;
        }

        ClanWorldData.unclaimChunk(clan.id, dim, chunkPos);
        sendClaimResult(player, true,
                PREFIX + "Chunk §e[" + chunkPos.x + ", " + chunkPos.z + "] §rretire du territoire.",
                clan, chunkPos, false);

        if (claimModePlayers.contains(uuid)) {
            sendClaimMap(player, clan);
        }
    }

    // -------------------------------------------------------------------------
    // Achat de level
    // -------------------------------------------------------------------------

    /**
     * Achete le prochain level economique pour le clan.
     * Debite la banque du clan. Reserve a l'owner (ou a ceux avec permission buy_level).
     */
    public static void handleBuyLevel(ServerPlayerEntity player, MinecraftServer server) {
        UUID uuid = player.getUuid();
        Clan clan = ClanWorldData.getClanOf(uuid);
        if (clan == null) { sendError(player, "Tu n'es dans aucun clan."); return; }
        if (!hasPermission(clan, uuid, "buy_level")) {
            sendError(player, "Permission insuffisante pour acheter un level.");
            return;
        }

        ClanConfig cfg = ClanConfig.get();
        if (clan.clanLevel >= cfg.maxClanLevel) {
            sendError(player, "Le clan est deja au niveau maximum (" + cfg.maxClanLevel + ").");
            return;
        }

        long price = cfg.levelUpPrice(clan.clanLevel);
        if (clan.bankBalance < price) {
            sendError(player, "La banque du clan n'a que §e" + clan.bankBalance +
                    " §rcoins (§e" + price + " §rnecessaires).");
            return;
        }

        ClanWorldData.setBank(clan.id, clan.bankBalance - price);
        int newLevel = clan.clanLevel + 1;
        ClanWorldData.setClanLevel(clan.id, newLevel);

        String msg = PREFIX + "§e" + player.getName().getString() +
                " §ra achete le §6Level " + newLevel +
                " §rpour le clan ! (§e" + price + " §rcoins)";
        for (UUID memberUUID : clan.members.keySet()) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(memberUUID);
            if (p != null) p.sendMessage(Text.literal(msg), false);
        }
    }

    // -------------------------------------------------------------------------
    // Handler C2S
    // -------------------------------------------------------------------------

    /**
     * Receiver C2S pour ClanClaimActionPayload.
     * Dispatch vers claim/unclaim/exit.
     */
    public static void handle(ClanClaimActionPayload payload,
                               ServerPlayNetworking.Context ctx) {
        ctx.server().execute(() -> {
            ServerPlayerEntity player = ctx.player();
            switch (payload.action()) {
                case "claim"   -> claimCurrentChunk(player, ctx.server());
                case "unclaim" -> unclaimCurrentChunk(player, ctx.server());
                case "exit"    -> exitClaimMode(player);
                default        -> player.sendMessage(Text.literal(ERROR + "Action inconnue : " + payload.action()), false);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    /**
     * Valide qu'un chunk peut etre claim par un clan.
     * Retourne null si OK, un message d'erreur sinon.
     */
    private static String validateClaim(Clan clan, RegistryKey<World> dim,
                                        ChunkPos pos, String key) {
        ClanConfig cfg = ClanConfig.get();

        // 1. Deja claim ?
        if (ClanWorldData.isChunkClaimed(dim, pos)) {
            Clan existing = ClanWorldData.getChunkOwner(dim, pos);
            if (existing != null && existing.id.equals(clan.id)) {
                return ERROR + "Ce chunk est deja claim par ton clan.";
            }
            return ERROR + "Ce chunk est deja claim par le clan §e" +
                    (existing != null ? existing.name : "inconnu") + "§r.";
        }

        // 2. Claims disponibles ?
        if (clan.remainingClaims() <= 0) {
            return ERROR + "Plus de claims disponibles (§e" +
                    clan.claimedChunks.size() + "§r/§e" + clan.maxClaims() +
                    "§r). Achete un level superieur.";
        }

        // 3. Connexite (sauf premier claim)
        if (!clan.claimedChunks.isEmpty()) {
            boolean adjacent = false;
            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] d : dirs) {
                String adjKey = ClanWorldData.chunkKey(dim, pos.x + d[0], pos.z + d[1]);
                if (clan.claimedChunks.contains(adjKey)) {
                    adjacent = true;
                    break;
                }
            }
            if (!adjacent) {
                return ERROR + "Le chunk doit etre adjacent a ton territoire existant.";
            }
        }

        // 4. Buffer de distance par rapport aux autres clans (Chebyshev)
        int buffer = cfg.claimBufferDistance;
        for (int dx = -buffer; dx <= buffer; dx++) {
            for (int dz = -buffer; dz <= buffer; dz++) {
                if (dx == 0 && dz == 0) continue;
                Clan nearOwner = ClanWorldData.getChunkOwner(dim, pos.x + dx, pos.z + dz);
                if (nearOwner != null && !nearOwner.id.equals(clan.id)) {
                    return ERROR + "Trop proche du territoire du clan §e" +
                            nearOwner.name + " §r(distance minimum : " + buffer + " chunks).";
                }
            }
        }

        return null; // validation OK
    }

    /**
     * Verifie que retirer un chunk ne casse pas la connexite du territoire.
     * Utilise un BFS depuis un chunk restant arbitraire.
     * O(N) ou N = nombre de chunks du clan.
     */
    private static boolean wouldRemainContiguous(Clan clan, String removedKey) {
        Set<String> remaining = new HashSet<>(clan.claimedChunks);
        remaining.remove(removedKey);
        if (remaining.isEmpty()) return true;

        String start = remaining.iterator().next();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int lastColon = current.lastIndexOf(':');
            int prevColon = current.lastIndexOf(':', lastColon - 1);
            String dimPart = current.substring(0, prevColon);
            int cx, cz;
            try {
                cx = Integer.parseInt(current.substring(prevColon + 1, lastColon));
                cz = Integer.parseInt(current.substring(lastColon + 1));
            } catch (NumberFormatException e) {
                continue;
            }

            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] d : dirs) {
                String adj = dimPart + ":" + (cx + d[0]) + ":" + (cz + d[1]);
                if (remaining.contains(adj) && !visited.contains(adj)) {
                    visited.add(adj);
                    queue.add(adj);
                }
            }
        }

        return visited.size() == remaining.size();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static boolean hasPermission(Clan clan, UUID playerUUID, String action) {
        String requiredRankId = clan.permissions.getOrDefault(action, "owner");
        String playerRankId   = ClanManager.getMemberRankId(clan, playerUUID);

        ClanRank required = clan.getRankById(requiredRankId);
        ClanRank player_  = clan.getRankById(playerRankId);
        if (required == null || player_ == null) return false;

        // Un joueur avec un sortOrder <= au requis a la permission
        return player_.sortOrder <= required.sortOrder;
    }

    private static boolean isAllowedDimension(ServerPlayerEntity player) {
        String dimId = player.getWorld().getRegistryKey().getValue().toString();
        return dimId.equals(ClanConfig.get().claimAllowedDimension);
    }

    private static void sendError(ServerPlayerEntity player, String msg) {
        player.sendMessage(Text.literal(ERROR + msg), false);
    }

    private static void sendClaimResult(ServerPlayerEntity player, boolean success,
                                        String message, Clan clan,
                                        ChunkPos pos, boolean claimed) {
        // Le message est affiche cote client via ClanClaimResultPayload — pas de sendMessage ici
        ServerPlayNetworking.send(player, new ClanClaimResultPayload(
                success, message, clan.maxClaims(), clan.claimedChunks.size(),
                pos.x, pos.z, claimed));
    }

    /**
     * Envoie la liste des chunks du clan et des chunks proches d'autres clans.
     */
    private static void sendClaimMap(ServerPlayerEntity player, Clan clan) {
        RegistryKey<World> dim = player.getWorld().getRegistryKey();

        List<ClanClaimMapPayload.ChunkEntry> own = new ArrayList<>();
        for (String key : clan.claimedChunks) {
            int[] coords = parseChunkKey(key);
            if (coords != null) own.add(new ClanClaimMapPayload.ChunkEntry(coords[0], coords[1]));
        }

        List<ClanClaimMapPayload.ChunkEntry> nearby = new ArrayList<>();
        ChunkPos playerChunk = player.getChunkPos();
        int scanRadius = 20;
        for (int dx = -scanRadius; dx <= scanRadius; dx++) {
            for (int dz = -scanRadius; dz <= scanRadius; dz++) {
                int cx = playerChunk.x + dx;
                int cz = playerChunk.z + dz;
                Clan owner = ClanWorldData.getChunkOwner(dim, cx, cz);
                if (owner != null && !owner.id.equals(clan.id)) {
                    nearby.add(new ClanClaimMapPayload.ChunkEntry(cx, cz));
                }
            }
        }

        ServerPlayNetworking.send(player, new ClanClaimMapPayload(
                own, nearby, clan.maxClaims(), clan.claimedChunks.size()));
    }

    /** Parse "minecraft:overworld:42:-17" -> [42, -17] ou null. */
    private static int[] parseChunkKey(String key) {
        int lastColon = key.lastIndexOf(':');
        int prevColon = key.lastIndexOf(':', lastColon - 1);
        if (prevColon < 0 || lastColon < 0) return null;
        try {
            int cx = Integer.parseInt(key.substring(prevColon + 1, lastColon));
            int cz = Integer.parseInt(key.substring(lastColon + 1));
            return new int[]{cx, cz};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
