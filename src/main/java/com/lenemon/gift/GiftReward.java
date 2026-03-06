package com.lenemon.gift;

/**
 * The type Gift reward.
 */
public class GiftReward {
    /**
     * The Type.
     */
    public String type; // "item" ou "command"
    /**
     * The Data.
     */
    public String data; // item id ou commande sans /
    /**
     * The Count.
     */
    public int count;   // quantité (items uniquement)
    /**
     * The Chance.
     */
    public double chance; // % de 0.01 à 100
    /**
     * The Display name.
     */
    public String displayName; // nom affiché dans le GUI

    /**
     * Instantiates a new Gift reward.
     */
    public GiftReward() {}

    /**
     * Instantiates a new Gift reward.
     *
     * @param type        the type
     * @param data        the data
     * @param count       the count
     * @param chance      the chance
     * @param displayName the display name
     */
    public GiftReward(String type, String data, int count, double chance, String displayName) {
        this.type = type;
        this.data = data;
        this.count = count;
        this.chance = chance;
        this.displayName = displayName;
    }
}