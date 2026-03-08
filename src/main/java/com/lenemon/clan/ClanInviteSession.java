package com.lenemon.clan;

import java.util.*;

/**
 * Gestion des invitations de clan en memoire.
 * Les invitations ne sont pas persistees : elles expirent a la deconnexion
 * de l'invite ou de l'invitant, ou lors d'un redemarrage serveur.
 *
 * Toutes les methodes doivent etre appelees depuis le thread serveur.
 */
public class ClanInviteSession {

    /** Map : UUID de l'invite -> UUID du clan auquel il est invite. */
    private static final Map<UUID, UUID> pending = new HashMap<>();

    /** Empêche l'instanciation. */
    private ClanInviteSession() {}

    /**
     * Enregistre une invitation.
     * Ecrase une invitation precedente si elle existait deja.
     *
     * @param inviteeUUID UUID du joueur invite
     * @param clanId      UUID du clan
     */
    public static void add(UUID inviteeUUID, UUID clanId) {
        pending.put(inviteeUUID, clanId);
    }

    /**
     * Retourne l'ID du clan pour lequel ce joueur a une invitation en attente,
     * ou null s'il n'en a pas.
     */
    public static UUID getPendingClanId(UUID inviteeUUID) {
        return pending.get(inviteeUUID);
    }

    /**
     * Supprime l'invitation d'un joueur (apres acceptation, refus ou deconnexion).
     */
    public static void remove(UUID inviteeUUID) {
        pending.remove(inviteeUUID);
    }

    /**
     * Supprime toutes les invitations vers un clan donne.
     * Utile lors de la dissolution d'un clan.
     */
    public static void removeAllForClan(UUID clanId) {
        pending.entrySet().removeIf(e -> e.getValue().equals(clanId));
    }

    /** Retourne true si ce joueur a une invitation en attente. */
    public static boolean hasPending(UUID inviteeUUID) {
        return pending.containsKey(inviteeUUID);
    }
}
