package com.lenemon.armor;

import com.cobblemon.mod.common.api.spawning.CobblemonSpawnPools;
import com.cobblemon.mod.common.api.spawning.SpawnBucket;
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence;
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The type Base spawn influence.
 */
public class BaseSpawnInfluence implements SpawningInfluence {

    private final ServerPlayerEntity player;
    private final ArmorSet armorSet;
    // Cache par ArmorSet pour ne pas recréer les copies à chaque instance
    private static final Map<Class<?>, List<SpawnDetail>> detailsCache = new HashMap<>();

    /**
     * Instantiates a new Base spawn influence.
     *
     * @param player   the player
     * @param armorSet the armor set
     */
    public BaseSpawnInfluence(ServerPlayerEntity player, ArmorSet armorSet) {
        this.player = player;
        this.armorSet = armorSet;
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public List<SpawnDetail> injectSpawns(SpawnBucket bucket, SpawnablePosition pos) {
        if (!armorSet.isWearing(player)) return Collections.emptyList();
        if (!armorSet.ignoresBiome()) return Collections.emptyList();

        // Spawns custom définis par le set (optionnel)
        List<SpawnDetail> custom = armorSet.getInjectedSpawns(bucket, pos);
        if (!custom.isEmpty()) return custom;

        // Spawns boostés depuis le pool Cobblemon
        List<String> boosted = armorSet.getBoostedPokemon();
        if (boosted.isEmpty()) return Collections.emptyList();

        List<SpawnDetail> cached = detailsCache.computeIfAbsent(armorSet.getClass(), k -> {
            List<SpawnDetail> copies = CobblemonSpawnPools.WORLD_SPAWN_POOL.getDetails()
                    .stream()
                    .filter(d -> d instanceof PokemonSpawnDetail
                            && boosted.stream().anyMatch(name -> d.getId().toLowerCase().contains(name)))
                    .map(d -> (SpawnDetail) copyDetail((PokemonSpawnDetail) d,
                            d.getWeight() * armorSet.getWeightMultiplier()))
                    .collect(Collectors.toList());
            System.out.println("[LeNeMon] Cache créé pour " + armorSet.getClass().getSimpleName() + " : " + copies.size() + " spawns");
            return copies;
        });

        cached.forEach(d -> d.setBucket(bucket));
        return cached;
    }

    @Override
    public float affectWeight(SpawnDetail detail, SpawnablePosition pos, float weight) {
        if (!armorSet.isWearing(player)) return weight;

        boolean isBoosted = armorSet.getBoostedPokemon().stream()
                .anyMatch(name -> detail.getId().toLowerCase().contains(name));

        return isBoosted ? weight * armorSet.getWeightMultiplier() : weight;
    }

    @Override
    public boolean affectSpawnable(SpawnDetail detail, SpawnablePosition pos) {
        if (!armorSet.isWearing(player)) return SpawningInfluence.super.affectSpawnable(detail, pos);
        if (!armorSet.ignoresBiome()) return SpawningInfluence.super.affectSpawnable(detail, pos);

        boolean isBoosted = armorSet.getBoostedPokemon().stream()
                .anyMatch(name -> detail.getId().toLowerCase().contains(name));

        return isBoosted || SpawningInfluence.super.affectSpawnable(detail, pos);
    }

    private static PokemonSpawnDetail copyDetail(PokemonSpawnDetail original, float weight) {
        PokemonSpawnDetail copy = new PokemonSpawnDetail();
        copy.setId(original.getId() + "-lenemon-copy");
        copy.setWeight(weight);
        copy.setSpawnablePositionType(original.getSpawnablePositionType());
        copy.setBucket(original.getBucket());
        copy.setWidth(original.getWidth());
        copy.setHeight(original.getHeight());
        copy.setPercentage(original.getPercentage());
        copy.setPokemon(original.getPokemon());
        copy.setLevelRange(original.getLevelRange());
        return copy;
    }

    /**
     * Clear cache.
     */
    public static void clearCache() {
        detailsCache.clear();
        System.out.println("[LeNeMon] Cache spawn vidé.");
    }
}