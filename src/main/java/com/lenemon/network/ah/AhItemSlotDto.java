package com.lenemon.network.ah;

/**
 * DTO représentant un slot d'inventaire proposé à la vente.
 */
public record AhItemSlotDto(
        int slot,
        String itemId,
        String displayName,
        int count,
        boolean canSell
) {}
