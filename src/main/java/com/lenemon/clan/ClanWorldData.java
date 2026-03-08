package com.lenemon.clan;

import com.google.gson.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Persistance JSON de tous les clans du serveur.
 * Pattern identique a HunterWorldData : fichier JSON dans world root.
 * Fichier : <world>/lenemon_clans.json
 *
 * Toutes les methodes publiques doivent etre appelees depuis le thread serveur.
 */
public class ClanWorldData {

    private static final Logger LOGGER = LoggerFactory.getLogger("lenemon/clan");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Map clanId -> Clan */
    private static final Map<UUID, Clan> clans = new LinkedHashMap<>();

    /** Index inverse : playerUUID -> clanId (pour lookup O(1)). */
    private static final Map<UUID, UUID> playerClanIndex = new HashMap<>();

    /** Index inverse : chunkKey -> clanId (pour protection territoire O(1)). */
    private static final Map<String, UUID> chunkOwnerIndex = new HashMap<>();

    /**
     * Derniere connexion des joueurs (hors-ligne).
     * Timestamp Unix ms. Cle = joueur UUID.
     * Persiste en JSON independamment des clans.
     */
    private static final Map<UUID, Long> memberLastSeen = new HashMap<>();

    private static MinecraftServer server;

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    /**
     * A appeler dans ServerLifecycleEvents.SERVER_STARTED.
     * Charge les donnees depuis le disque.
     */
    public static void register(MinecraftServer srv) {
        server = srv;
        load();
    }

    // -------------------------------------------------------------------------
    // Acces aux donnees
    // -------------------------------------------------------------------------

    /** Retourne tous les clans. */
    public static Collection<Clan> getAll() {
        return clans.values();
    }

    /** Retourne un clan par son ID, ou null. */
    public static Clan getById(UUID clanId) {
        return clans.get(clanId);
    }

    /** Retourne le clan d'un joueur, ou null s'il n'est dans aucun clan. */
    public static Clan getClanOf(UUID playerUUID) {
        UUID clanId = playerClanIndex.get(playerUUID);
        if (clanId == null) return null;
        return clans.get(clanId);
    }

    /** Retourne true si un joueur est dans un clan. */
    public static boolean isInClan(UUID playerUUID) {
        return playerClanIndex.containsKey(playerUUID);
    }

    /** Retourne true si un nom de clan est deja utilise (insensible a la casse). */
    public static boolean isNameTaken(String name) {
        String lower = name.toLowerCase();
        return clans.values().stream().anyMatch(c -> c.name.toLowerCase().equals(lower));
    }

    /** Retourne true si un tag de clan est deja utilise (insensible a la casse). */
    public static boolean isTagTaken(String tag) {
        String upper = tag.toUpperCase();
        return clans.values().stream().anyMatch(c -> c.tag.equals(upper));
    }

    /**
     * Retourne le timestamp de derniere deconnexion d'un joueur, ou 0 si inconnu.
     */
    public static long getLastSeen(UUID playerUUID) {
        return memberLastSeen.getOrDefault(playerUUID, 0L);
    }

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    /**
     * Ajoute un nouveau clan et sauvegarde.
     */
    public static void addClan(Clan clan) {
        clans.put(clan.id, clan);
        // Indexer tous les membres (normalement juste l'owner a la creation)
        for (UUID memberUUID : clan.members.keySet()) {
            playerClanIndex.put(memberUUID, clan.id);
        }
        save();
    }

    /**
     * Ajoute un membre a un clan existant et sauvegarde.
     */
    public static void addMember(UUID clanId, UUID playerUUID, ClanRole role) {
        Clan clan = clans.get(clanId);
        if (clan == null) return;
        clan.members.put(playerUUID, role);
        clan.memberRanks.putIfAbsent(playerUUID, switch (role) {
            case OWNER -> "owner"; case OFFICER -> "officer"; default -> "member";
        });
        playerClanIndex.put(playerUUID, clanId);
        save();
    }

    /**
     * Retire un membre d'un clan et sauvegarde.
     */
    public static void removeMember(UUID clanId, UUID playerUUID) {
        Clan clan = clans.get(clanId);
        if (clan == null) return;
        clan.members.remove(playerUUID);
        clan.memberRanks.remove(playerUUID);
        playerClanIndex.remove(playerUUID);
        save();
    }

    /**
     * Change le role d'un membre et sauvegarde.
     */
    public static void setRole(UUID clanId, UUID playerUUID, ClanRole role) {
        Clan clan = clans.get(clanId);
        if (clan == null) return;
        if (!clan.members.containsKey(playerUUID)) return;
        clan.members.put(playerUUID, role);
        if (role == ClanRole.OWNER) {
            clan.ownerUUID = playerUUID;
        }
        save();
    }

    /**
     * Met a jour simultanement le rang affiche (rankId) et le role de permission (ClanRole) d'un membre.
     */
    public static void setMemberRankAndRole(UUID clanId, UUID playerUUID, String rankId, ClanRole role) {
        Clan clan = clans.get(clanId);
        if (clan == null) return;
        clan.memberRanks.put(playerUUID, rankId);
        clan.members.put(playerUUID, role);
        if (role == ClanRole.OWNER) {
            clan.ownerUUID = playerUUID;
        }
        save();
    }

    /**
     * Supprime un clan entier et retire tous ses membres de l'index.
     * Libere aussi tous les chunks claims par ce clan.
     */
    public static void removeClan(UUID clanId) {
        Clan clan = clans.remove(clanId);
        if (clan == null) return;
        // Liberer tous les chunks claims
        for (String key : clan.claimedChunks) {
            chunkOwnerIndex.remove(key);
        }
        for (UUID member : clan.members.keySet()) {
            playerClanIndex.remove(member);
        }
        save();
    }

    /**
     * Met a jour la banque d'un clan et sauvegarde.
     */
    public static void setBank(UUID clanId, long balance) {
        Clan clan = clans.get(clanId);
        if (clan == null) return;
        clan.bankBalance = balance;
        save();
    }

    /**
     * Incremente la contribution totale d'un joueur dans la banque du clan.
     */
    public static void addContribution(UUID clanId, UUID playerUUID, long amount) {
        Clan clan = clans.get(clanId);
        if (clan == null) return;
        long current = clan.memberContributions.getOrDefault(playerUUID, 0L);
        clan.memberContributions.put(playerUUID, current + amount);
        save();
    }

    /**
     * Enregistre le timestamp de derniere deconnexion d'un joueur.
     */
    public static void setLastSeen(UUID playerUUID, long timestamp) {
        memberLastSeen.put(playerUUID, timestamp);
        save();
    }

    /**
     * Met a jour la limite de retrait d'un rang et sauvegarde.
     * Accepte -1 pour illimite. Ne force pas de minimum a 0.
     */
    public static void setRankWithdrawLimit(UUID clanId, String rankId, long limit) {
        Clan clan = clans.get(clanId);
        if (clan == null) return;
        ClanRank rank = clan.getRankById(rankId);
        if (rank == null) return;
        // -1 = illimite ; toute autre valeur doit etre >= 0
        rank.withdrawLimit = (limit == -1L) ? -1L : Math.max(0L, limit);
        save();
    }

    /**
     * Ajoute un rang custom au clan et sauvegarde.
     * Retourne false si le clan a deja atteint MAX_RANKS.
     */
    public static boolean addRank(UUID clanId, String name, String colorCode) {
        Clan clan = clans.get(clanId);
        if (clan == null) return false;
        if (clan.ranks.size() >= ClanRank.MAX_RANKS) return false;
        int maxOrder = clan.ranks.stream().mapToInt(r -> r.sortOrder).max().orElse(2);
        String newId = java.util.UUID.randomUUID().toString().substring(0, 8);
        clan.ranks.add(new ClanRank(newId, name, colorCode, 5000L, maxOrder + 1));
        save();
        return true;
    }

    /**
     * Supprime un rang custom (non systeme) et sauvegarde.
     */
    public static boolean removeRank(UUID clanId, String rankId) {
        Clan clan = clans.get(clanId);
        if (clan == null) return false;
        ClanRank rank = clan.getRankById(rankId);
        if (rank == null || rank.isSystemRank()) return false;
        clan.ranks.removeIf(r -> r.id.equals(rankId));
        save();
        return true;
    }

    /**
     * Renomme un rang et sauvegarde.
     */
    public static void renameRank(UUID clanId, String rankId, String newName) {
        Clan clan = clans.get(clanId);
        if (clan == null) return;
        ClanRank rank = clan.getRankById(rankId);
        if (rank == null) return;
        rank.name = newName;
        save();
    }

    /**
     * Change la couleur d'un rang et sauvegarde.
     */
    public static void setRankColor(UUID clanId, String rankId, String colorCode) {
        Clan clan = clans.get(clanId);
        if (clan == null) return;
        ClanRank rank = clan.getRankById(rankId);
        if (rank == null) return;
        rank.colorCode = colorCode;
        save();
    }

    /**
     * Echange le sortOrder de deux rangs adjacents (monter ou descendre).
     * Rangs systeme (owner/officer/member) ne peuvent pas etre reordonnees.
     * Retourne false si l'operation n'est pas possible.
     */
    public static boolean reorderRank(UUID clanId, String rankId, boolean moveUp) {
        Clan clan = clans.get(clanId);
        if (clan == null) return false;

        // Trier les rangs par sortOrder
        List<ClanRank> sorted = new ArrayList<>(clan.ranks);
        sorted.sort(Comparator.comparingInt(r -> r.sortOrder));

        int idx = -1;
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).id.equals(rankId)) {
                idx = i;
                break;
            }
        }
        if (idx == -1) return false;

        int swapIdx = moveUp ? idx - 1 : idx + 1;
        if (swapIdx < 0 || swapIdx >= sorted.size()) return false;

        // Echanger les sortOrder
        int tmpOrder = sorted.get(idx).sortOrder;
        sorted.get(idx).sortOrder = sorted.get(swapIdx).sortOrder;
        sorted.get(swapIdx).sortOrder = tmpOrder;

        save();
        return true;
    }

    /**
     * Met a jour les permissions du clan et sauvegarde.
     * action : "kick" | "promote" | "demote"
     * requiredRole : "owner" | "officer" | "member"
     */
    public static void setPermission(UUID clanId, String action, String requiredRole) {
        Clan clan = clans.get(clanId);
        if (clan == null) return;
        clan.permissions.put(action, requiredRole);
        save();
    }

    /** Garantit que les 3 rangs systeme sont presents dans un clan (migration). */
    private static void ensureSystemRanks(Clan clan) {
        String[] sysIds    = {"owner",   "officer", "member"};
        String[] sysNames  = {"Proprietaire", "Officer", "Membre"};
        String[] sysColors = {"§c",      "§6",      "§7"};
        long[]   sysLimits = {-1L,        -1L,       5000L};
        int[]    sysOrders = {0,           1,         2};
        for (int i = 0; i < sysIds.length; i++) {
            final String sid = sysIds[i];
            if (clan.ranks.stream().noneMatch(r -> r.id.equals(sid))) {
                clan.ranks.add(new ClanRank(sid, sysNames[i], sysColors[i], sysLimits[i], sysOrders[i]));
            }
        }
        clan.ranks.sort(java.util.Comparator.comparingInt(r -> r.sortOrder));
    }

    /**
     * Met a jour le niveau et l'XP d'un clan et sauvegarde.
     */
    public static void setLevelAndXp(UUID clanId, int level, long xp) {
        Clan clan = clans.get(clanId);
        if (clan == null) return;
        clan.level = level;
        clan.xp = xp;
        save();
    }

    /**
     * Met a jour le clanLevel economique et sauvegarde.
     */
    public static void setClanLevel(UUID clanId, int clanLevel) {
        Clan clan = clans.get(clanId);
        if (clan == null) return;
        clan.clanLevel = clanLevel;
        save();
    }

    // -------------------------------------------------------------------------
    // Claims de territoire
    // -------------------------------------------------------------------------

    /**
     * Retourne le clan proprietaire d'un chunk, ou null.
     * Lookup O(1) via chunkOwnerIndex.
     */
    public static Clan getChunkOwner(RegistryKey<World> dim, ChunkPos pos) {
        UUID clanId = chunkOwnerIndex.get(chunkKey(dim, pos));
        return clanId != null ? clans.get(clanId) : null;
    }

    /**
     * Surcharge avec coordonnees chunk en int.
     */
    public static Clan getChunkOwner(RegistryKey<World> dim, int chunkX, int chunkZ) {
        UUID clanId = chunkOwnerIndex.get(chunkKey(dim, chunkX, chunkZ));
        return clanId != null ? clans.get(clanId) : null;
    }

    /**
     * Retourne true si le chunk est claim par un clan quelconque.
     */
    public static boolean isChunkClaimed(RegistryKey<World> dim, ChunkPos pos) {
        return chunkOwnerIndex.containsKey(chunkKey(dim, pos));
    }

    /**
     * Claim un chunk pour un clan.
     * Ne fait AUCUNE validation (appeler validateClaim() dans ClanClaimHandler avant).
     */
    public static void claimChunk(UUID clanId, RegistryKey<World> dim, ChunkPos pos) {
        String key = chunkKey(dim, pos);
        Clan clan = clans.get(clanId);
        if (clan == null) return;
        clan.claimedChunks.add(key);
        chunkOwnerIndex.put(key, clanId);
        save();
    }

    /**
     * Unclaim un chunk d'un clan.
     */
    public static void unclaimChunk(UUID clanId, RegistryKey<World> dim, ChunkPos pos) {
        String key = chunkKey(dim, pos);
        Clan clan = clans.get(clanId);
        if (clan == null) return;
        clan.claimedChunks.remove(key);
        chunkOwnerIndex.remove(key);
        save();
    }

    /**
     * Unclaim TOUS les chunks d'un clan (appele lors de disband).
     */
    public static void unclaimAll(UUID clanId) {
        Clan clan = clans.get(clanId);
        if (clan == null) return;
        for (String key : new ArrayList<>(clan.claimedChunks)) {
            chunkOwnerIndex.remove(key);
        }
        clan.claimedChunks.clear();
        save();
    }

    /**
     * Construit une cle de chunk depuis une dimension et un ChunkPos.
     * Format : "minecraft:overworld:X:Z"
     */
    public static String chunkKey(RegistryKey<World> dim, ChunkPos pos) {
        return dim.getValue().toString() + ":" + pos.x + ":" + pos.z;
    }

    /**
     * Construit une cle de chunk depuis une dimension et des coordonnees int.
     */
    public static String chunkKey(RegistryKey<World> dim, int chunkX, int chunkZ) {
        return dim.getValue().toString() + ":" + chunkX + ":" + chunkZ;
    }

    // -------------------------------------------------------------------------
    // Persistance
    // -------------------------------------------------------------------------

    public static void save() {
        if (server == null) return;
        try {
            File file = getSaveFile();
            file.getParentFile().mkdirs();

            JsonObject root = new JsonObject();
            JsonArray array = new JsonArray();

            for (Clan clan : clans.values()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", clan.id.toString());
                obj.addProperty("name", clan.name);
                obj.addProperty("tag", clan.tag);
                obj.addProperty("ownerUUID", clan.ownerUUID.toString());
                obj.addProperty("bankBalance", clan.bankBalance);
                obj.addProperty("level", clan.level);
                obj.addProperty("xp", clan.xp);
                obj.addProperty("createdAt", clan.createdAt);
                obj.addProperty("clanLevel", clan.clanLevel);

                // Rangs personnalises
                JsonArray ranksArr = new JsonArray();
                for (ClanRank rank : clan.ranks) {
                    JsonObject ro = new JsonObject();
                    ro.addProperty("id", rank.id);
                    ro.addProperty("name", rank.name);
                    ro.addProperty("colorCode", rank.colorCode);
                    ro.addProperty("withdrawLimit", rank.withdrawLimit);
                    ro.addProperty("sortOrder", rank.sortOrder);
                    ranksArr.add(ro);
                }
                obj.add("ranks", ranksArr);

                // Membres
                JsonObject membersObj = new JsonObject();
                for (Map.Entry<UUID, ClanRole> entry : clan.members.entrySet()) {
                    membersObj.addProperty(entry.getKey().toString(), entry.getValue().name());
                }
                obj.add("members", membersObj);

                // Contributions des membres
                JsonObject contribObj = new JsonObject();
                for (Map.Entry<UUID, Long> entry : clan.memberContributions.entrySet()) {
                    contribObj.addProperty(entry.getKey().toString(), entry.getValue());
                }
                obj.add("memberContributions", contribObj);

                // Permissions
                JsonObject permsObj = new JsonObject();
                for (Map.Entry<String, String> entry : clan.permissions.entrySet()) {
                    permsObj.addProperty(entry.getKey(), entry.getValue());
                }
                obj.add("permissions", permsObj);

                // Rangs des membres
                JsonObject memberRanksObj = new JsonObject();
                for (Map.Entry<UUID, String> entry : clan.memberRanks.entrySet()) {
                    memberRanksObj.addProperty(entry.getKey().toString(), entry.getValue());
                }
                obj.add("memberRanks", memberRanksObj);

                // Chunks claims
                JsonArray chunksArr = new JsonArray();
                for (String chunkKey : clan.claimedChunks) {
                    chunksArr.add(chunkKey);
                }
                obj.add("claimedChunks", chunksArr);

                array.add(obj);
            }

            root.add("clans", array);

            // Derniere connexion (global, pas par clan)
            JsonObject lastSeenObj = new JsonObject();
            for (Map.Entry<UUID, Long> entry : memberLastSeen.entrySet()) {
                lastSeenObj.addProperty(entry.getKey().toString(), entry.getValue());
            }
            root.add("memberLastSeen", lastSeenObj);

            try (FileWriter w = new FileWriter(file)) {
                w.write(GSON.toJson(root));
            }

        } catch (Exception e) {
            LOGGER.error("[Clan] Erreur sauvegarde : {}", e.getMessage());
        }
    }

    public static void load() {
        if (server == null) return;
        try {
            File file = getSaveFile();
            if (!file.exists()) {
                LOGGER.info("[Clan] Aucun fichier de sauvegarde trouve, demarrage a vide.");
                return;
            }

            clans.clear();
            playerClanIndex.clear();
            chunkOwnerIndex.clear();
            memberLastSeen.clear();

            JsonObject root = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
            for (JsonElement el : root.getAsJsonArray("clans")) {
                JsonObject obj = el.getAsJsonObject();

                Clan clan = new Clan();
                clan.id = UUID.fromString(obj.get("id").getAsString());
                clan.name = obj.get("name").getAsString();
                clan.tag = obj.get("tag").getAsString();
                clan.ownerUUID = UUID.fromString(obj.get("ownerUUID").getAsString());
                clan.bankBalance = obj.get("bankBalance").getAsLong();
                clan.level = obj.get("level").getAsInt();
                clan.xp = obj.get("xp").getAsLong();
                clan.createdAt = obj.get("createdAt").getAsLong();

                // Niveau economique (migration : defaut 1 si absent)
                clan.clanLevel = obj.has("clanLevel") ? obj.get("clanLevel").getAsInt() : 1;

                // Rangs : charger ou generer les defaults
                clan.ranks = new java.util.ArrayList<>();
                if (obj.has("ranks")) {
                    for (JsonElement re : obj.getAsJsonArray("ranks")) {
                        JsonObject ro = re.getAsJsonObject();
                        ClanRank rank = new ClanRank();
                        rank.id           = ro.get("id").getAsString();
                        rank.name         = ro.get("name").getAsString();
                        rank.colorCode    = ro.get("colorCode").getAsString();
                        rank.withdrawLimit = ro.get("withdrawLimit").getAsLong();
                        rank.sortOrder    = ro.get("sortOrder").getAsInt();
                        clan.ranks.add(rank);
                    }
                }
                // Garantir la presence des rangs systeme
                ensureSystemRanks(clan);

                JsonObject membersObj = obj.getAsJsonObject("members");
                for (Map.Entry<String, JsonElement> entry : membersObj.entrySet()) {
                    UUID memberUUID = UUID.fromString(entry.getKey());
                    ClanRole role = ClanRole.valueOf(entry.getValue().getAsString());
                    clan.members.put(memberUUID, role);
                    playerClanIndex.put(memberUUID, clan.id);
                }

                // Contributions
                clan.memberContributions = new HashMap<>();
                if (obj.has("memberContributions")) {
                    JsonObject contribObj = obj.getAsJsonObject("memberContributions");
                    for (Map.Entry<String, JsonElement> entry : contribObj.entrySet()) {
                        clan.memberContributions.put(
                                UUID.fromString(entry.getKey()),
                                entry.getValue().getAsLong()
                        );
                    }
                }

                // Permissions
                clan.permissions = Clan.defaultPermissions();
                if (obj.has("permissions")) {
                    JsonObject permsObj = obj.getAsJsonObject("permissions");
                    for (Map.Entry<String, JsonElement> entry : permsObj.entrySet()) {
                        clan.permissions.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }

                // Rangs des membres
                clan.memberRanks = new HashMap<>();
                if (obj.has("memberRanks")) {
                    JsonObject mro = obj.getAsJsonObject("memberRanks");
                    for (Map.Entry<String, JsonElement> entry : mro.entrySet()) {
                        clan.memberRanks.put(UUID.fromString(entry.getKey()), entry.getValue().getAsString());
                    }
                }
                // Backfill depuis ClanRole pour les membres sans entrée
                for (Map.Entry<UUID, ClanRole> entry : clan.members.entrySet()) {
                    clan.memberRanks.computeIfAbsent(entry.getKey(), k -> switch (entry.getValue()) {
                        case OWNER -> "owner"; case OFFICER -> "officer"; default -> "member";
                    });
                }

                // Chunks claims + reconstruction de chunkOwnerIndex
                clan.claimedChunks = new java.util.LinkedHashSet<>();
                if (obj.has("claimedChunks")) {
                    for (JsonElement ce : obj.getAsJsonArray("claimedChunks")) {
                        String key = ce.getAsString();
                        clan.claimedChunks.add(key);
                        chunkOwnerIndex.put(key, clan.id);
                    }
                }

                clans.put(clan.id, clan);
            }

            // Derniere connexion
            if (root.has("memberLastSeen")) {
                JsonObject lsObj = root.getAsJsonObject("memberLastSeen");
                for (Map.Entry<String, JsonElement> entry : lsObj.entrySet()) {
                    memberLastSeen.put(UUID.fromString(entry.getKey()), entry.getValue().getAsLong());
                }
            }

            LOGGER.info("[Clan] {} clan(s) charge(s).", clans.size());

        } catch (Exception e) {
            LOGGER.error("[Clan] Erreur chargement : {}", e.getMessage());
        }
    }

    private static File getSaveFile() {
        return new File(
                server.getSavePath(WorldSavePath.ROOT).toFile(),
                "lenemon_clans.json"
        );
    }
}
