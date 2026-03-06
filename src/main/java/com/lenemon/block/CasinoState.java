package com.lenemon.block;

/**
 * The enum Casino state.
 */
public enum CasinoState {
    /**
     * Unconfigured casino state.
     */
    UNCONFIGURED,   // posé, rien configuré, cassable par le proprio
    /**
     * Configured casino state.
     */
    CONFIGURED,     // prix + % définis, pas encore de Pokémon
    /**
     * Active casino state.
     */
    ACTIVE,         // Pokémon en jeu, jouable
    /**
     * Locked casino state.
     */
    LOCKED,         // spin en cours
    /**
     * The Empty.
     */
    EMPTY           // Pokémon gagné, doit être rechargé
}