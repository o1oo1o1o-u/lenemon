package com.lenemon.client.cobblemon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bridge Cobblemon -> (entity + infos affichables).
 * Tu vas devoir adapter 2-3 lignes selon ton API Cobblemon.
 */
public final class CobblemonPokemonView {

    /**
     * The type View.
     */
    public record View(LivingEntity entity, Text displayName, boolean shiny, String paletteKey) {}

    private static final Map<UUID, LivingEntity> ENTITY_CACHE = new HashMap<>();

    private CobblemonPokemonView() {}

    /**
     * Variante "je reçois un NBT complet d'un Pokemon" et j'obtiens une entity + infos.
     * NOTE: les appels Cobblemon (Pokemon.fromNBT, PokemonEntity...) sont à adapter.
     *
     * @param cacheKey   the cache key
     * @param pokemonNbt the pokemon nbt
     * @return the view
     */
    public static View fromPokemonNbt(UUID cacheKey, NbtCompound pokemonNbt) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return new View(null, Text.literal(""), false, "");

        LivingEntity entity = ENTITY_CACHE.get(cacheKey);
        if (entity == null) {
            // TODO Cobblemon: reconstruire un Pokemon depuis NBT
            // Exemple d'intention (à adapter):
            // Pokemon pokemon = Pokemon.Companion.loadFromNBT(pokemonNbt);
            // PokemonEntity pokeEntity = new PokemonEntity(client.world, pokemon);
            // entity = pokeEntity;

            // Tant que pas branché:
            return new View(null, Text.literal("Cobblemon: entity non branchée"), false, "");
        }

        // TODO Cobblemon: lire displayName, shiny, palette
        // Exemple d'intention (à adapter):
        // Pokemon pokemon = ((PokemonEntity) entity).getPokemon();
        // Text name = Text.literal(pokemon.getSpecies().getName()); ou pokemon.getDisplayName()
        // boolean shiny = pokemon.isShiny()
        // String palette = pokemon.getPalette().getName() (si existe) sinon "SHINY"/"NORMAL"

        return new View(entity, Text.literal("TODO name"), false, "");
    }

    /**
     * Variante "j'ai déjà une entity Cobblemon" (le plus simple si tu peux).
     *
     * @param cacheKey        the cache key
     * @param cobblemonEntity the cobblemon entity
     * @param displayName     the display name
     * @param shiny           the shiny
     * @param paletteKey      the palette key
     * @return the view
     */
    public static View fromEntity(UUID cacheKey, LivingEntity cobblemonEntity, Text displayName, boolean shiny, String paletteKey) {
        if (cobblemonEntity != null) {
            ENTITY_CACHE.put(cacheKey, cobblemonEntity);
        }
        return new View(cobblemonEntity, displayName, shiny, paletteKey);
    }

    /**
     * Clear.
     *
     * @param cacheKey the cache key
     */
    public static void clear(UUID cacheKey) {
        ENTITY_CACHE.remove(cacheKey);
    }

    /**
     * Clear all.
     */
    public static void clearAll() {
        ENTITY_CACHE.clear();
    }
}