package com.lenemon.casino;

import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * The type Casino pokemon storage.
 */
public class CasinoPokemonStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static File getFile(MinecraftServer server, UUID casinoUUID) {
        File dir = new File(
                server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).toFile(),
                "casino_pokemon"
        );
        dir.mkdirs();
        return new File(dir, casinoUUID + ".json");
    }

    /**
     * Save pokemon.
     *
     * @param server          the server
     * @param casinoUUID      the casino uuid
     * @param pokemon         the pokemon
     * @param registryManager the registry manager
     */
    public static void savePokemon(MinecraftServer server, UUID casinoUUID,
                                   Pokemon pokemon, DynamicRegistryManager registryManager) {
        try {
            // Cobblemon 1.7.x : saveToNBT retourne le NbtCompound
            NbtCompound nbt = pokemon.saveToNBT(registryManager, new NbtCompound());

            // Vérifie que le NBT n'est pas vide
            if (nbt.isEmpty()) {
                System.err.println("[Casino] Attention : NBT vide pour " + pokemon.getDisplayName(false).getString());
            }

            String nbtString = nbt.asString();

            JsonObject json = new JsonObject();
            json.addProperty("displayName", pokemon.getDisplayName(false).getString());
            json.addProperty("level", pokemon.getLevel());
            json.addProperty("shiny", pokemon.getShiny());
            json.addProperty("species", pokemon.getSpecies().getName());
            json.addProperty("nbt", nbtString);

            File file = getFile(server, casinoUUID);
            try (Writer writer = new OutputStreamWriter(
                    new FileOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }

            System.out.println("[Casino] Pokémon sauvegardé : " + pokemon.getDisplayName(false).getString()
                    + " | NBT size: " + nbt.getSize());

        } catch (Exception e) {
            System.err.println("[Casino] Erreur sauvegarde Pokémon : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load pokemon pokemon.
     *
     * @param server          the server
     * @param casinoUUID      the casino uuid
     * @param registryManager the registry manager
     * @return the pokemon
     */
    public static Pokemon loadPokemon(MinecraftServer server, UUID casinoUUID,
                                      DynamicRegistryManager registryManager) {
        try {
            File file = getFile(server, casinoUUID);
            if (!file.exists()) return null;

            JsonObject json;
            try (Reader reader = new InputStreamReader(
                    new FileInputStream(file), StandardCharsets.UTF_8)) {
                json = JsonParser.parseReader(reader).getAsJsonObject();
            }

            // Reconstruit le NBT depuis la string
            String nbtString = json.get("nbt").getAsString();
            NbtCompound nbt = StringNbtReader.parse(nbtString);

            // Charge le Pokémon depuis le NBT complet
            return Pokemon.Companion.loadFromNBT(registryManager, nbt);

        } catch (Exception e) {
            System.err.println("[Casino] Erreur chargement Pokémon : " + e.getMessage());
            return null;
        }
    }

    /**
     * Delete pokemon.
     *
     * @param server     the server
     * @param casinoUUID the casino uuid
     */
    public static void deletePokemon(MinecraftServer server, UUID casinoUUID) {
        File file = getFile(server, casinoUUID);
        if (file.exists()) file.delete();
    }

    /**
     * Has pokemon boolean.
     *
     * @param server     the server
     * @param casinoUUID the casino uuid
     * @return the boolean
     */
    public static boolean hasPokemon(MinecraftServer server, UUID casinoUUID) {
        return getFile(server, casinoUUID).exists();
    }

    /**
     * Is party full boolean.
     *
     * @param party the party
     * @return the boolean
     */
    public static boolean isPartyFull(PlayerPartyStore party) {
        if (party == null) return true;
        int count = 0;
        for (Pokemon p : party) {
            if (p != null) count++;
        }
        return count >= 6;
    }
}