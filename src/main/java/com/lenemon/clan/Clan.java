package com.lenemon.clan;

import java.util.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;

/**
 * Modele de donnees d'un clan.
 * Mutable cote serveur uniquement. Toutes les mutations doivent
 * etre effectuees dans le thread serveur (server().execute()).
 */
public class Clan {

    /** Identifiant unique du clan. */
    public UUID id;

    /** Nom d'affichage du clan (max 32 caracteres). */
    public String name;

    /**
     * Tag court du clan (max 8 caracteres, majuscules).
     * Affiche dans le chat sous la forme [TAG].
     */
    public String tag;

    /** UUID du fondateur/proprietaire actuel. */
    public UUID ownerUUID;

    /**
     * Membres du clan avec leur role.
     * L'owner est toujours present avec ClanRole.OWNER.
     */
    public Map<UUID, ClanRole> members = new LinkedHashMap<>();

    /** Solde de la banque partagee du clan (en monnaie Impactor). */
    public long bankBalance = 0L;

    /** Niveau actuel du clan (1 a 10). */
    public int level = 1;

    /** XP totale accumulee par le clan. */
    public long xp = 0L;

    /** Timestamp de creation (System.currentTimeMillis()). */
    public long createdAt;

    /** Niveau economique du clan (achete avec argent de la banque). Demarre a 1. */
    public int clanLevel = 1;

    /**
     * Chunks claims par le clan.
     * Cle : "minecraft:overworld:X:Z" (dimension:chunkX:chunkZ)
     * Transient car gere manuellement dans ClanWorldData save/load.
     */
    public transient Set<String> claimedChunks = new LinkedHashSet<>();

    /**
     * Rangs personnalises du clan (max ClanRank.MAX_RANKS).
     * Toujours au moins 3 rangs systeme : owner, officer, member.
     * Non serialise directement par Gson (gestion manuelle dans ClanWorldData).
     */
    public transient List<ClanRank> ranks = new ArrayList<>();

    /** Cache des noms de joueurs pour l'affichage (non persiste, reconstruit a la connexion). */
    public transient Map<UUID, String> memberNames = new HashMap<>();

    /**
     * Total cumule des coins deposes par chaque membre dans la banque du clan.
     * Persiste en JSON via ClanWorldData.
     * Non transient : gestion manuelle dans save/load.
     */
    public transient Map<UUID, Long> memberContributions = new HashMap<>();

    /**
     * Rang affiche de chaque membre (rankId, systeme ou custom).
     * Persiste en JSON via ClanWorldData.
     */
    public transient Map<UUID, String> memberRanks = new HashMap<>();

    /**
     * Permissions du clan : quelle ClanRole minimum est requise pour chaque action.
     * Cles : "kick", "promote", "demote"
     * Valeurs : "owner", "officer", "member"
     * Non transient : gestion manuelle dans save/load.
     */
    public transient Map<String, String> permissions = new HashMap<>();

    public Clan() {
        this.ranks = ClanRank.defaultRanks();
        this.permissions = defaultPermissions();
    }

    public Clan(UUID id, String name, String tag, UUID ownerUUID) {
        this.id = id;
        this.name = name;
        this.tag = tag.toUpperCase();
        this.ownerUUID = ownerUUID;
        this.createdAt = System.currentTimeMillis();
        this.members.put(ownerUUID, ClanRole.OWNER);
        this.ranks = ClanRank.defaultRanks();
        this.permissions = defaultPermissions();
        this.memberRanks.put(ownerUUID, "owner");
    }

    /** Retourne les permissions par defaut du clan. */
    public static Map<String, String> defaultPermissions() {
        Map<String, String> p = new HashMap<>();
        p.put("kick",      "officer");
        p.put("promote",   "owner");
        p.put("demote",    "owner");
        p.put("claim",     "officer");
        p.put("buy_level", "owner");
        return p;
    }

    /** Retourne le role d'un membre, ou null s'il n'est pas dans le clan. */
    public ClanRole getRole(UUID uuid) {
        return members.get(uuid);
    }

    /** Retourne true si l'UUID est membre du clan (quel que soit le role). */
    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    /** Retourne le nombre total de membres. */
    public int size() {
        return members.size();
    }

    /** Retourne le rang associe a un ClanRole systeme, ou null si absent. */
    public ClanRank getRankForRole(ClanRole role) {
        String sysId = switch (role) {
            case OWNER   -> "owner";
            case OFFICER -> "officer";
            case MEMBER  -> "member";
        };
        return getRankById(sysId);
    }

    /** Retourne un rang par son id, ou null. */
    public ClanRank getRankById(String rankId) {
        for (ClanRank r : ranks) {
            if (r.id.equals(rankId)) return r;
        }
        return null;
    }

    /**
     * Retourne l'XP requise pour atteindre le niveau suivant.
     * Formule : 500 * niveau^2 (override possible via ClanConfig).
     */
    public static long xpForLevel(int level) {
        return 500L * level * level;
    }

    /**
     * Calcule le nombre maximum de chunks claimables pour le clanLevel actuel.
     * Level 1 : baseChunks (defaut 10)
     * Level N>=2 : baseChunks + chunksPerLevelMultiplier * (N*(N+1)/2 - 1)
     *
     * Exemples (base=10, mult=5):
     *   L1=10, L2=20, L3=35, L4=55, L5=80, L10=280
     */
    public int maxClaims() {
        ClanConfig cfg = ClanConfig.get();
        if (clanLevel <= 1) return cfg.baseChunks;
        return cfg.baseChunks + cfg.chunksPerLevelMultiplier
               * (clanLevel * (clanLevel + 1) / 2 - 1);
    }

    /**
     * Retourne le nombre de claims restants disponibles.
     */
    public int remainingClaims() {
        return maxClaims() - claimedChunks.size();
    }
}
