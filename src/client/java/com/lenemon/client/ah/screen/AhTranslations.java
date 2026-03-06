package com.lenemon.client.ah.screen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Traductions françaises pour l'interface de l'Hôtel des Ventes.
 */
public final class AhTranslations {

    private AhTranslations() {}

    // ── Natures ───────────────────────────────────────────────────────────────

    private static final Map<String, String> NATURE_FR = new HashMap<>();

    static {
        NATURE_FR.put("cobblemon:lonely",  "Solo");
        NATURE_FR.put("cobblemon:brave",   "Brave");
        NATURE_FR.put("cobblemon:adamant", "Ferme");
        NATURE_FR.put("cobblemon:naughty", "Malin");
        NATURE_FR.put("cobblemon:bold",    "Audacieux");
        NATURE_FR.put("cobblemon:relaxed", "Relax");
        NATURE_FR.put("cobblemon:impish",  "Mauvais");
        NATURE_FR.put("cobblemon:lax",     "Lâche");
        NATURE_FR.put("cobblemon:timid",   "Timide");
        NATURE_FR.put("cobblemon:hasty",   "Pressé");
        NATURE_FR.put("cobblemon:jolly",   "Jovial");
        NATURE_FR.put("cobblemon:naive",   "Naïf");
        NATURE_FR.put("cobblemon:modest",  "Modeste");
        NATURE_FR.put("cobblemon:mild",    "Doux");
        NATURE_FR.put("cobblemon:quiet",   "Discret");
        NATURE_FR.put("cobblemon:rash",    "Fougueux");
        NATURE_FR.put("cobblemon:calm",    "Calme");
        NATURE_FR.put("cobblemon:gentle",  "Gentil");
        NATURE_FR.put("cobblemon:sassy",   "Assur");
        NATURE_FR.put("cobblemon:careful", "Prudent");
        NATURE_FR.put("cobblemon:quirky",  "Bizarre");
        NATURE_FR.put("cobblemon:serious", "Sérieux");
        NATURE_FR.put("cobblemon:docile",  "Docile");
        NATURE_FR.put("cobblemon:bashful", "Pudique");
        NATURE_FR.put("cobblemon:hardy",   "Endurant");
    }

    // ── Types ─────────────────────────────────────────────────────────────────

    private static final Map<String, String> TYPE_FR = new HashMap<>();

    static {
        TYPE_FR.put("normal",   "Normal");
        TYPE_FR.put("fire",     "Feu");
        TYPE_FR.put("water",    "Eau");
        TYPE_FR.put("grass",    "Plante");
        TYPE_FR.put("electric", "Électrik");
        TYPE_FR.put("ice",      "Glace");
        TYPE_FR.put("fighting", "Combat");
        TYPE_FR.put("poison",   "Poison");
        TYPE_FR.put("ground",   "Sol");
        TYPE_FR.put("flying",   "Vol");
        TYPE_FR.put("psychic",  "Psy");
        TYPE_FR.put("bug",      "Insecte");
        TYPE_FR.put("rock",     "Roche");
        TYPE_FR.put("ghost",    "Spectre");
        TYPE_FR.put("dragon",   "Dragon");
        TYPE_FR.put("dark",     "Ténèbres");
        TYPE_FR.put("steel",    "Acier");
        TYPE_FR.put("fairy",    "Fée");
    }

    // ── Balls ─────────────────────────────────────────────────────────────────

    private static final Map<String, String> BALL_FR = new HashMap<>();

    static {
        BALL_FR.put("poke_ball",    "Poké Ball");
        BALL_FR.put("great_ball",   "Super Ball");
        BALL_FR.put("ultra_ball",   "Hyper Ball");
        BALL_FR.put("master_ball",  "Master Ball");
        BALL_FR.put("premier_ball", "Premier Ball");
        BALL_FR.put("luxury_ball",  "Luxe Ball");
        BALL_FR.put("heal_ball",    "Soin Ball");
        BALL_FR.put("net_ball",     "Filet Ball");
        BALL_FR.put("dive_ball",    "Scuba Ball");
        BALL_FR.put("nest_ball",    "Nid Ball");
        BALL_FR.put("repeat_ball",  "Bis Ball");
        BALL_FR.put("timer_ball",   "Chrono Ball");
        BALL_FR.put("quick_ball",   "Rapide Ball");
        BALL_FR.put("dusk_ball",    "Sombre Ball");
        BALL_FR.put("heavy_ball",   "Masse Ball");
        BALL_FR.put("love_ball",    "Amour Ball");
        BALL_FR.put("friend_ball",  "Ami Ball");
        BALL_FR.put("fast_ball",    "Sprint Ball");
        BALL_FR.put("level_ball",   "Niveau Ball");
        BALL_FR.put("lure_ball",    "Mère Ball");
        BALL_FR.put("moon_ball",    "Lune Ball");
        BALL_FR.put("dream_ball",   "Rêve Ball");
        BALL_FR.put("beast_ball",   "Bête Ball");
        BALL_FR.put("safari_ball",  "Safari Ball");
        BALL_FR.put("sport_ball",   "Sport Ball");
        BALL_FR.put("cherish_ball", "Chérie Ball");
    }

    // ── API publique ──────────────────────────────────────────────────────────

    public static String nature(String raw) {
        if (raw == null || raw.isEmpty()) return "?";
        String result = NATURE_FR.get(raw.toLowerCase());
        if (result != null) return result;
        // Fallback : retirer le namespace
        int colon = raw.lastIndexOf(':');
        return colon >= 0 ? capitalize(raw.substring(colon + 1)) : capitalize(raw);
    }

    public static String type(String raw) {
        if (raw == null || raw.isEmpty()) return "?";
        return TYPE_FR.getOrDefault(raw.toLowerCase(), capitalize(raw));
    }

    public static String types(List<String> rawTypes) {
        if (rawTypes == null || rawTypes.isEmpty()) return "?";
        return rawTypes.stream().map(AhTranslations::type).collect(Collectors.joining("/"));
    }

    public static String ball(String raw) {
        if (raw == null || raw.isEmpty()) return "?";
        String result = BALL_FR.get(raw.toLowerCase());
        if (result != null) return result;
        // Fallback : remplacer underscores par espaces
        return capitalize(raw.replace('_', ' '));
    }

    public static String ability(String raw) {
        if (raw == null || raw.isEmpty()) return "?";
        // Les capacités spéciales n'ont pas de table, on formate le namespace
        int colon = raw.lastIndexOf(':');
        String name = colon >= 0 ? raw.substring(colon + 1) : raw;
        return capitalize(name.replace('_', ' '));
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
