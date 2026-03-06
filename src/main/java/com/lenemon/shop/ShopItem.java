package com.lenemon.shop;

/**
 * The type Shop item.
 */
public class ShopItem {
    /**
     * The Id.
     */
    public String id;
    /**
     * The Display name.
     */
    public String displayName;
    /**
     * The Buy price.
     */
    public double  buyPrice = -1;  // prix serveur vend au joueur (-1 = pas en vente)
    /**
     * The Sell price.
     */
    public double  sellPrice = -1; // prix serveur rachète au joueur (-1 = pas en achat)
}