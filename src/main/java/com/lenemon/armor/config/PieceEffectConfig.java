package com.lenemon.armor.config;

/**
 * The type Piece effect config.
 */
public class PieceEffectConfig {
    /**
     * The Effect.
     */
    public String effect = "";
    /**
     * The Level.
     */
    public int level = 1;

    /**
     * Instantiates a new Piece effect config.
     */
    public PieceEffectConfig() {}

    /**
     * Instantiates a new Piece effect config.
     *
     * @param effect the effect
     * @param level  the level
     */
    public PieceEffectConfig(String effect, int level) {
        this.effect = effect;
        this.level = level;
    }
}