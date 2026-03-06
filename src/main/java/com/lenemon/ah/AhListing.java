package com.lenemon.ah;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Représente une mise en vente dans l'Hôtel des Ventes.
 */
public class AhListing {

    public UUID listingId;
    public UUID sellerUuid;
    public String sellerName;
    public String type; // "item" ou "pokemon"

    // Items
    public String itemId;
    public String itemDisplayName;
    public int itemCount;
    public String itemNbt; // peut être null

    // Pokémon
    public String pokemonNbt;         // NbtCompound.asString()
    public String pokemonSpecies;
    public Set<String> pokemonAspects = new HashSet<>();
    public String pokemonDisplayName;
    public boolean pokemonShiny;
    public int pokemonLevel;

    // Pokémon — stats supplémentaires (remplis à la création du listing)
    public String pokemonNature;
    public String pokemonAbility;
    public List<String> pokemonTypes  = new ArrayList<>();
    public String pokemonEvs;
    public List<String> pokemonMoves  = new ArrayList<>();
    public String pokemonBall;
    public boolean pokemonBreedable;
    public int pokemonFriendship;

    // Commun
    public long price;
    public long listedAt;
    public long expiresAt;
    public boolean sold;
    public boolean expired;
    public UUID buyerUuid;   // null si pas acheté
    public String buyerName;
}
