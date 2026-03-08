package com.lenemon.clan;

/**
 * Roles possibles au sein d'un clan.
 * Ordre de priorite : OWNER > OFFICER > MEMBER.
 */
public enum ClanRole {
    OWNER,
    OFFICER,
    MEMBER;

    /**
     * Retourne true si ce role peut effectuer des actions de moderation
     * (kick, invite) sur des membres de role inferieur.
     */
    public boolean canModerate(ClanRole target) {
        return this.ordinal() < target.ordinal();
    }
}
