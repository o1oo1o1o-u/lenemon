package com.lenemon.network.ah;

import java.util.List;

/**
 * DTO représentant un Pokémon de la party proposé à la vente.
 */
public record AhPartyPokemonDto(
        int partyIndex,
        String species,
        List<String> aspects,
        String displayName,
        boolean shiny,
        int level,
        String nature,
        String ivs,
        // Stats supplémentaires
        String ability,
        List<String> types,
        String evs,
        List<String> moves,
        String ball,
        boolean breedable,
        int friendship
) {}
