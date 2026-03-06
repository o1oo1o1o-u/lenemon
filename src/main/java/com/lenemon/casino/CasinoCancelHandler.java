package com.lenemon.casino;

import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.lenemon.block.CasinoBlockEntity;
import com.lenemon.block.CasinoState;
import com.lenemon.casino.holo.CasinoHolograms;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import com.lenemon.casino.CasinoWorldData;
import com.lenemon.casino.CasinoSpinHandler;
import net.minecraft.server.world.ServerWorld;

/**
 * The type Casino cancel handler.
 */
public class CasinoCancelHandler {

    /**
     * Cancel.
     *
     * @param player the player
     * @param entity the entity
     * @param pos    the pos
     */
    public static void cancel(ServerPlayerEntity player, CasinoBlockEntity entity, BlockPos pos) {
        if (entity.isLocked()) {
            player.sendMessage(Text.literal("§c[Casino] Impossible — un joueur est en train de jouer !"), false);
            return;
        }

        NbtCompound pokemonNbt = entity.getPokemonData();
        if (pokemonNbt == null) {
            entity.setState(CasinoState.UNCONFIGURED);
            entity.clearPokemon();
            player.sendMessage(Text.literal("§e[Casino] Casino réinitialisé."), false);
            return;
        }

        // Recrée le Pokémon depuis le NBT
        Pokemon pokemon = new Pokemon();
        pokemon.loadFromNBT(player.getServerWorld().getRegistryManager(), pokemonNbt);

        // Tente d'ajouter à la party
        PlayerPartyStore party = com.cobblemon.mod.common.Cobblemon.INSTANCE
                .getStorage().getParty(player);

        boolean addedToParty = false;
        int count = 0;
        for (var p : party) { if (p != null) count++; }
        player.sendMessage(Text.literal("§e[DEBUG] Party size avant add: " + count + " | isPartyFull: " + CasinoPokemonStorage.isPartyFull(party)), false);
        if (!CasinoPokemonStorage.isPartyFull(party)) {
            party.add(pokemon);
            addedToParty = true;
        }

        if (!addedToParty) {
            // Party pleine → envoi au PC
            PCStore pc = com.cobblemon.mod.common.Cobblemon.INSTANCE
                    .getStorage().getPC(player.getUuid(), player.getServerWorld().getRegistryManager());
            if (pc != null) {
                pc.add(pokemon);
                player.sendMessage(Text.literal("§e[Casino] Party pleine — Pokémon envoyé au PC."), false);
            } else {
                player.sendMessage(Text.literal("§c[Casino] Erreur : impossible de rendre le Pokémon !"), false);
                return;
            }
        } else {
            player.sendMessage(Text.literal("§a[Casino] Pokémon rendu à votre party !"), false);
        }

        entity.clearPokemon();
        entity.setState(CasinoState.CONFIGURED);

        player.sendMessage(Text.literal("§7Le casino est maintenant en attente d'un nouveau Pokémon."), false);
    }

    /**
     * Cancel from world data.
     *
     * @param player the player
     * @param casino the casino
     * @param data   the data
     * @param pos    the pos
     */
    public static void cancelFromWorldData(ServerPlayerEntity player,
                                           CasinoWorldData.CasinoData casino,
                                           CasinoWorldData data, BlockPos pos) {
        casino.pokemonSpecies = "";
        casino.pokemonAspects = new java.util.HashSet<>();
        if (casino.locked) {
            player.sendMessage(Text.literal("§c[Casino] Un spin est en cours !"), false);
            return;
        }

        if (!CasinoPokemonStorage.hasPokemon(player.getServer(), casino.casinoUUID)) {
            casino.state = CasinoState.UNCONFIGURED;
            casino.pokemonDisplayName = "";
            data.markDirty();
            player.sendMessage(Text.literal("§e[Casino] Casino réinitialisé."), false);
            return;
        }

        Pokemon pokemon = CasinoPokemonStorage.loadPokemon(
                player.getServer(),
                casino.casinoUUID,
                player.getServerWorld().getRegistryManager()
        );

        if (pokemon == null) {
            player.sendMessage(Text.literal("§c[Casino] Erreur chargement Pokémon !"), false);
            return;
        }

        PlayerPartyStore party = com.cobblemon.mod.common.Cobblemon.INSTANCE
                .getStorage().getParty(player);

        if (!CasinoPokemonStorage.isPartyFull(party)) {
            party.add(pokemon);
            player.sendMessage(Text.literal("§a[Casino] Pokémon rendu à votre party !"), false);
        } else {
            PCStore pc = com.cobblemon.mod.common.Cobblemon.INSTANCE
                    .getStorage().getPC(player.getUuid(), player.getServerWorld().getRegistryManager());
            if (pc != null) {
                pc.add(pokemon);
                player.sendMessage(Text.literal("§e[Casino] Party pleine — Pokémon envoyé au PC."), false);
            } else {
                player.sendMessage(Text.literal("§c[Casino] Impossible de rendre le Pokémon !"), false);
                return;
            }
        }

        CasinoPokemonStorage.deletePokemon(player.getServer(), casino.casinoUUID);
        casino.pokemonDisplayName = "";
        casino.state = CasinoState.CONFIGURED;
        data.markDirty();

        String ownerName = casino.ownerName != null && !casino.ownerName.isBlank()
                ? casino.ownerName
                : player.getName().getString();

        BlockPos bottomPos = CasinoHolograms.resolveCasinoPos(data, pos);
        CasinoHolograms.removeCasinoHologramAround(bottomPos);

        CasinoHolograms.recreateClosedCasinoHologram(
                player.getServerWorld(),
                bottomPos,
                ownerName,
                casino.entryPrice,
                casino.winChance / 100.0
        );

        player.sendMessage(Text.literal("§7Le casino attend un nouveau Pokémon."), false);
    }
}