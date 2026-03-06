package com.lenemon.armor;

import com.cobblemon.mod.common.api.spawning.SpawnBucket;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition;
import com.lenemon.armor.config.EffectConfig;
import com.lenemon.armor.effects.ArmorEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import com.lenemon.armor.config.ArmorSetConfig;


import java.util.List;


/**
 * The interface Armor set.
 */
public interface ArmorSet {

    /**
     * Gets config.
     *
     * @return the config
     */
    default ArmorSetConfig getConfig() { return null; }

    /**
     * Recharge la config depuis le fichier JSON
     */
    default void reload() {}

    /**
     * Si false, le set est complètement ignoré au démarrage  @return  the boolean
     *
     * @return the boolean
     */
    default boolean isEnabled() { return true; }

    /**
     * Si true, force le spawn partout même hors biome naturel  @return  the boolean
     *
     * @return the boolean
     */
    default boolean ignoresBiome() { return false; }

    /**
     * Vérifie si le joueur porte ce set complet  @param player the player
     *
     * @param player the player
     * @return the boolean
     */
    boolean isWearing(ServerPlayerEntity player);

    /**
     * Message affiché quand le set est activé  @return  the activation message
     *
     * @return the activation message
     */
    Text getActivationMessage();

    /**
     * Message affiché quand le set est retiré  @return  the deactivation message
     *
     * @return the deactivation message
     */
    Text getDeactivationMessage();

    /**
     * IDs de Pokémon à booster (ex: "pikachu")  @return  the boosted pokemon
     *
     * @return the boosted pokemon
     */
    List<String> getBoostedPokemon();

    /**
     * Multiplicateur de poids pour les Pokémon boostés  @return  the weight multiplier
     *
     * @return the weight multiplier
     */
    default float getWeightMultiplier() { return 50f; }

    /**
     * Injecter des spawns custom (optionnel)  @param bucket the bucket
     *
     * @param bucket the bucket
     * @param pos    the pos
     * @return the injected spawns
     */
    default List<SpawnDetail> getInjectedSpawns(SpawnBucket bucket, SpawnablePosition pos) {
        return List.of();
    }

    /**
     * Gets effects.
     *
     * @return the effects
     */
    default List<ArmorEffect> getEffects() { return List.of(); }


    /**
     * Gets helmet item.
     *
     * @return the helmet item
     */
    net.minecraft.item.Item getHelmetItem();

    /**
     * Gets chestplate item.
     *
     * @return the chestplate item
     */
    net.minecraft.item.Item getChestplateItem();

    /**
     * Gets leggings item.
     *
     * @return the leggings item
     */
    net.minecraft.item.Item getLeggingsItem();

    /**
     * Gets boots item.
     *
     * @return the boots item
     */
    net.minecraft.item.Item getBootsItem();

    /**
     * Gets effect configs.
     *
     * @return the effect configs
     */
    default List<EffectConfig> getEffectConfigs() { return List.of(); }

    /**
     * Gets piece effects.
     *
     * @param player the player
     * @return the piece effects
     */
// Retourne les effets actifs selon les pièces portées
    default List<ArmorEffect> getPieceEffects(ServerPlayerEntity player) {
        return List.of();
    }

    /**
     * Gets set bonus effect.
     *
     * @return the set bonus effect
     */
// Retourne le bonus de set complet
    default ArmorEffect getSetBonusEffect() {
        return null;
    }

    /**
     * Gets all piece effects.
     *
     * @return the all piece effects
     */
    default List<ArmorEffect> getAllPieceEffects() { return List.of(); }

}