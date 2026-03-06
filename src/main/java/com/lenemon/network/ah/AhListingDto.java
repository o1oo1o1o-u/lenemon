package com.lenemon.network.ah;

import com.lenemon.ah.AhListing;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DTO sérialisable côté réseau représentant une mise en vente AH.
 */
public record AhListingDto(
        String listingId,
        String sellerName,
        String type,
        String itemId,
        String itemDisplayName,
        int itemCount,
        String pokemonSpecies,
        List<String> pokemonAspects,
        String pokemonDisplayName,
        boolean pokemonShiny,
        int pokemonLevel,
        // Stats supplémentaires Pokémon
        String pokemonNature,
        String pokemonAbility,
        List<String> pokemonTypes,
        String pokemonEvs,
        List<String> pokemonMoves,
        String pokemonBall,
        boolean pokemonBreedable,
        int pokemonFriendship,
        // Commun
        long price,
        long expiresAt,
        boolean isMine
) {
    /** Construit un DTO depuis un listing, avec protection NPE sur tous les String. */
    public static AhListingDto from(AhListing l, UUID viewerUuid) {
        return new AhListingDto(
                l.listingId != null ? l.listingId.toString() : "",
                l.sellerName != null ? l.sellerName : "",
                l.type != null ? l.type : "",
                l.itemId != null ? l.itemId : "",
                l.itemDisplayName != null ? l.itemDisplayName : "",
                l.itemCount,
                l.pokemonSpecies != null ? l.pokemonSpecies : "",
                l.pokemonAspects != null ? new ArrayList<>(l.pokemonAspects) : new ArrayList<>(),
                l.pokemonDisplayName != null ? l.pokemonDisplayName : "",
                l.pokemonShiny,
                l.pokemonLevel,
                l.pokemonNature != null ? l.pokemonNature : "",
                l.pokemonAbility != null ? l.pokemonAbility : "",
                l.pokemonTypes != null ? new ArrayList<>(l.pokemonTypes) : new ArrayList<>(),
                l.pokemonEvs != null ? l.pokemonEvs : "",
                l.pokemonMoves != null ? new ArrayList<>(l.pokemonMoves) : new ArrayList<>(),
                l.pokemonBall != null ? l.pokemonBall : "",
                l.pokemonBreedable,
                l.pokemonFriendship,
                l.price,
                l.expiresAt,
                l.sellerUuid != null && l.sellerUuid.equals(viewerUuid)
        );
    }
}
