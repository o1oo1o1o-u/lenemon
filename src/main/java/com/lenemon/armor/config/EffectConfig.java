package com.lenemon.armor.config;

import com.google.gson.annotations.SerializedName;

/**
 * The type Effect config.
 */
public class EffectConfig {
    /**
     * The Type.
     */
    public String type = "";

    /**
     * The Particle.
     */
// Particules
    public String particle = "ELECTRIC_SPARK";
    /**
     * The Density.
     */
    public int density = 3;
    /**
     * The Radius.
     */
    public double radius = 1.0;

    /**
     * The Color.
     */
// Glowing
    public String color = "WHITE";

    /**
     * The Sound.
     */
// Son ambiant
    public String sound = "";
    /**
     * The Volume.
     */
    public float volume = 1.0f;
    /**
     * The Pitch.
     */
    public float pitch = 1.0f;
}