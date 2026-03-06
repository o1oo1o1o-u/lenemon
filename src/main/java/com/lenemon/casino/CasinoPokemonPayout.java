package com.lenemon.casino;


import com.lenemon.block.CasinoBlockEntity;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.api.storage.pc.PCStore;

/**
 * The type Casino pokemon payout.
 */
public class CasinoPokemonPayout {

    /**
     * Give pokemon to player boolean.
     *
     * @param player the player
     * @param entity the entity
     * @return the boolean
     */
    public static boolean givePokemonToPlayer(ServerPlayerEntity player, CasinoBlockEntity entity) {
        NbtCompound pokemonNbt = entity.getPokemonData();
        if (pokemonNbt == null) return false;

        Pokemon pokemon = new Pokemon();
        pokemon.loadFromNBT(player.getServerWorld().getRegistryManager(), pokemonNbt);

        PlayerPartyStore party = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(player);
        if (!CasinoPokemonStorage.isPartyFull(party)) {
            party.add(pokemon);
            entity.clearPokemon();
            return true;
        }

        PCStore pc = com.cobblemon.mod.common.Cobblemon.INSTANCE
                .getStorage().getPC(player.getUuid(), player.getServerWorld().getRegistryManager());

        if (pc == null) return false;

        pc.add(pokemon);
        entity.clearPokemon();
        return true;
    }
}