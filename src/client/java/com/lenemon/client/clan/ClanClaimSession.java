package com.lenemon.client.clan;

import com.lenemon.network.clan.ClanClaimMapPayload;
import net.minecraft.util.math.ChunkPos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Etat local du mode claim cote client.
 * Singleton. Mis a jour par les payloads S2C ClanClaimModePayload et ClanClaimMapPayload.
 */
public class ClanClaimSession {

    private static boolean active     = false;
    private static int     maxClaims  = 0;
    private static int     usedClaims = 0;

    /** Cles long (ChunkPos.toLong) des chunks du clan du joueur. */
    private static final Set<Long> ownChunks   = new HashSet<>();
    /** Cles long des chunks d'autres clans a proximite. */
    private static final Set<Long> otherChunks = new HashSet<>();

    private ClanClaimSession() {}

    // ── Activation / Desactivation ───────────────────────────────────────────

    public static void activate(int max, int used) {
        active     = true;
        maxClaims  = max;
        usedClaims = used;
    }

    public static void deactivate() {
        active = false;
        ownChunks.clear();
        otherChunks.clear();
        maxClaims  = 0;
        usedClaims = 0;
    }

    // ── Mise a jour des chunks ────────────────────────────────────────────────

    public static void setChunks(List<ClanClaimMapPayload.ChunkEntry> own,
                                  List<ClanClaimMapPayload.ChunkEntry> other,
                                  int max, int used) {
        ownChunks.clear();
        for (ClanClaimMapPayload.ChunkEntry e : own) {
            ownChunks.add(ChunkPos.toLong(e.chunkX(), e.chunkZ()));
        }
        otherChunks.clear();
        for (ClanClaimMapPayload.ChunkEntry e : other) {
            otherChunks.add(ChunkPos.toLong(e.chunkX(), e.chunkZ()));
        }
        maxClaims  = max;
        usedClaims = used;
    }

    /**
     * Appele apres un claim/unclaim reussi pour mettre a jour localement
     * sans attendre un nouvel envoi de la map complete.
     */
    public static void updateAfterResult(int chunkX, int chunkZ, boolean claimed,
                                          int newMax, int newUsed) {
        long key = ChunkPos.toLong(chunkX, chunkZ);
        if (claimed) {
            ownChunks.add(key);
        } else {
            ownChunks.remove(key);
        }
        maxClaims  = newMax;
        usedClaims = newUsed;
    }

    // ── Accesseurs ────────────────────────────────────────────────────────────

    public static boolean isActive()           { return active; }
    public static int     getMaxClaims()       { return maxClaims; }
    public static int     getUsedClaims()      { return usedClaims; }
    public static Set<Long> getOwnChunks()     { return ownChunks; }
    public static Set<Long> getOtherChunks()   { return otherChunks; }
}
