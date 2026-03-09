package com.lenemon.clan;

/**
 * Definit un rang personnalise dans un clan.
 * Chaque clan peut avoir jusqu'a MAX_RANKS rangs.
 * Les rangs "owner", "officer" et "member" sont systeme : non supprimables, toujours presents.
 * Les rangs custom ont un id UUID.
 */
public class ClanRank {

    public static final int MAX_RANKS = 20;

    /** ID : "owner" | "officer" | "member" pour les rangs systeme, UUID pour les rangs custom. */
    public String id;

    /** Nom d'affichage (ex: "Proprietaire", "Gardien"). */
    public String name;

    /** Code couleur Minecraft (ex: "§c", "§6", "§7"). */
    public String colorCode;

    /**
     * Limite de retrait journalier en coins.
     * -1 = illimite. 0 = aucun retrait autorise. Toute valeur >= 0 est une limite concrete.
     */
    public long withdrawLimit;

    /**
     * Ordre de tri : 0 = rang le plus haut (owner).
     * Plus le chiffre est grand, plus le rang est bas dans la hierarchie.
     */
    public int sortOrder;

    /**
     * Si true, ce rang custom accede aux outils de gestion reserves au proprietaire,
     * hors dissolution du clan et transfert de propriete.
     */
    public boolean ownerPrivileges;

    public ClanRank() {}

    public ClanRank(String id, String name, String colorCode, long withdrawLimit, int sortOrder) {
        this.id = id;
        this.name = name;
        this.colorCode = colorCode;
        this.withdrawLimit = withdrawLimit;
        this.sortOrder = sortOrder;
        this.ownerPrivileges = "owner".equals(id);
    }

    /** Vrai si ce rang est un rang systeme (non supprimable). */
    public boolean isSystemRank() {
        return "owner".equals(id) || "officer".equals(id) || "member".equals(id);
    }

    /** Couleurs preset disponibles (code Minecraft). */
    public static final String[] PRESET_COLORS = {
        "§c", "§6", "§e", "§a", "§b", "§9", "§d", "§f", "§7", "§8"
    };

    /** Noms des couleurs pour l'affichage dans le GUI. */
    public static final String[] PRESET_COLOR_NAMES = {
        "Rouge", "Orange", "Jaune", "Vert", "Cyan", "Bleu", "Violet", "Blanc", "Gris", "Gris fonce"
    };

    /** Retourne l'index de la prochaine couleur preset (cycle). */
    public static int nextColorIndex(int current) {
        return (current + 1) % PRESET_COLORS.length;
    }

    /** Retourne l'index de cette couleur dans PRESET_COLORS, ou 8 (gris) si inconnue. */
    public static int colorIndex(String colorCode) {
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            if (PRESET_COLORS[i].equals(colorCode)) return i;
        }
        return 8;
    }

    /** Cree les 3 rangs systeme par defaut pour un nouveau clan. */
    public static java.util.List<ClanRank> defaultRanks() {
        java.util.List<ClanRank> list = new java.util.ArrayList<>();
        list.add(new ClanRank("owner",   "Proprietaire", "§c", -1L,   0));
        list.add(new ClanRank("officer", "Officer",      "§6", -1L,   1));
        list.add(new ClanRank("member",  "Membre",       "§7", 5000L, 2));
        return list;
    }
}
