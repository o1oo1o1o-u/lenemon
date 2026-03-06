package com.lenemon.hunter.quest;

/**
 * The enum Quest type.
 */
public enum QuestType {
    /**
     * Kill quest type.
     */
    KILL,           // tuer X pokémon
    /**
     * Capture quest type.
     */
    CAPTURE,        // capturer X pokémon
    /**
     * Capture type quest type.
     */
    CAPTURE_TYPE,   // capturer X pokémon d'un type
    /**
     * Capture shiny quest type.
     */
    CAPTURE_SHINY,  // capturer X pokémon shiny
    /**
     * Capture legendary quest type.
     */
    CAPTURE_LEGENDARY, // capturer X légendaires
    /**
     * Kill type quest type.
     */
    KILL_TYPE,      // tuer X pokémon d'un type
    /**
     * The Kill species.
     */
    KILL_SPECIES    // tuer X pokémon d'une espèce précise
}