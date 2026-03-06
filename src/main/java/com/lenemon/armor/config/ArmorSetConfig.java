package com.lenemon.armor.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The type Armor set config.
 */
public class ArmorSetConfig {
    /**
     * The Enabled.
     */
    public boolean enabled = true;
    /**
     * The Activation message.
     */
    public String activationMessage = "";
    /**
     * The Deactivation message.
     */
    public String deactivationMessage = "";
    /**
     * The Ignores biome.
     */
    public boolean ignoresBiome = false;
    /**
     * The Weight multiplier.
     */
    public float weightMultiplier = 10f;
    /**
     * The Boosted pokemon.
     */
    public List<String> boostedPokemon = new ArrayList<>();
    /**
     * The Effects.
     */
    public List<EffectConfig> effects = new ArrayList<>();

    /**
     * The Piece effects.
     */
    public Map<String, List<PieceEffectConfig>> pieceEffects = new HashMap<>();
    /**
     * The Set bonus effects.
     */
    public List<PieceEffectConfig> setBonusEffects = new ArrayList<>();

    /**
     * The Pokemon xp multiplier enabled.
     */
// Bonus spéciaux set complet
    public Boolean pokemonXpMultiplierEnabled = null;
    /**
     * The Pokemon xp multiplier.
     */
    public float pokemonXpMultiplier = 1.0f;

    /**
     * The Shiny multiplier enabled.
     */
    public Boolean shinyMultiplierEnabled = null;
    /**
     * The Shiny multiplier.
     */
    public float shinyMultiplier = 1.0f;

    /**
     * The Mining gift enabled.
     */
    public Boolean miningGiftEnabled = null;
    /**
     * The Mining gift chance.
     */
    public float miningGiftChance = 0.00005f;
    /**
     * The Mining gift commands.
     */
    public List<String> miningGiftCommands = new ArrayList<>();

    /**
     * Is pokemon xp enabled boolean.
     *
     * @return the boolean
     */
    public boolean isPokemonXpEnabled() { return Boolean.TRUE.equals(pokemonXpMultiplierEnabled); }

    /**
     * Is shiny enabled boolean.
     *
     * @return the boolean
     */
    public boolean isShinyEnabled()     { return Boolean.TRUE.equals(shinyMultiplierEnabled); }

    /**
     * Is mining gift enabled boolean.
     *
     * @return the boolean
     */
    public boolean isMiningGiftEnabled(){ return Boolean.TRUE.equals(miningGiftEnabled); }
}