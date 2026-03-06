package com.lenemon.armor.effects;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * The interface Armor effect.
 */
public interface ArmorEffect {
    /**
     * Appelé chaque tick serveur quand le set est porté  @param player the player
     *
     * @param player the player
     */
    void onTick(ServerPlayerEntity player);

    /**
     * Appelé quand le set est retiré pour nettoyer les effets  @param player the player
     *
     * @param player the player
     */
    void onRemove(ServerPlayerEntity player);

    /**
     * Gets effects.
     *
     * @return the effects
     */
    default List<ArmorEffect> getEffects() { return List.of(); }
}