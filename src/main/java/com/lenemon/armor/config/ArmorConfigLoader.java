package com.lenemon.armor.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The type Armor config loader.
 */
public class ArmorConfigLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Load armor set config.
     *
     * @param armorName the armor name
     * @param defaults  the defaults
     * @return the armor set config
     */
    public static ArmorSetConfig load(String armorName, ArmorSetConfig defaults) {
        Path configPath = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("lenemon")
                .resolve(armorName + ".json");

        configPath.getParent().toFile().mkdirs();

        // Fichier inexistant → créer avec defaults
        if (!configPath.toFile().exists()) {
            writeConfig(configPath, defaults);
            System.out.println("[LeNeMon] Config créée : " + armorName);
            return defaults;
        }

        // Fichier existant → charger et migrer
        try (Reader reader = new FileReader(configPath.toFile())) {
            ArmorSetConfig loaded = GSON.fromJson(reader, ArmorSetConfig.class);
            boolean migrated = migrate(loaded, defaults);

            if (migrated) {
                writeConfig(configPath, loaded);
                System.out.println("[LeNeMon] Config migrée : " + armorName);
            } else {
                System.out.println("[LeNeMon] Config chargée : " + armorName);
            }

            return loaded;
        } catch (IOException e) {
            System.err.println("[LeNeMon] Erreur lecture config : " + e.getMessage());
            return defaults;
        }
    }

    /** Ajoute les champs manquants depuis les defaults, retourne true si migration effectuée */
    private static boolean migrate(ArmorSetConfig loaded, ArmorSetConfig defaults) {
        boolean changed = false;

        if (loaded.boostedPokemon == null) {
            loaded.boostedPokemon = defaults.boostedPokemon;
            changed = true;
        }
        if (loaded.effects == null) {
            loaded.effects = new ArrayList<>();
            changed = true;
        }
        if (loaded.pieceEffects == null) {
            loaded.pieceEffects = defaults.pieceEffects;
            changed = true;
        } else {
            // Vérifier chaque slot individuellement
            for (String slot : List.of("helmet", "chestplate", "leggings", "boots")) {
                if (!loaded.pieceEffects.containsKey(slot)) {
                    loaded.pieceEffects.put(slot, defaults.pieceEffects.getOrDefault(slot, List.of()));
                    changed = true;
                }
            }
        }
        if (loaded.setBonusEffects == null) {
            loaded.setBonusEffects = defaults.setBonusEffects;
            changed = true;
        }
        if (loaded.activationMessage == null || loaded.activationMessage.isEmpty()) {
            loaded.activationMessage = defaults.activationMessage;
            changed = true;
        }
        if (loaded.deactivationMessage == null || loaded.deactivationMessage.isEmpty()) {
            loaded.deactivationMessage = defaults.deactivationMessage;
            changed = true;
        }
        if (loaded.pokemonXpMultiplier == 0) {
            loaded.pokemonXpMultiplier = defaults.pokemonXpMultiplier;
            changed = true;
        }
        if (loaded.shinyMultiplier == 0) {
            loaded.shinyMultiplier = defaults.shinyMultiplier;
            changed = true;
        }
        if (loaded.miningGiftCommands == null) {
            loaded.miningGiftCommands = defaults.miningGiftCommands;
            changed = true;
        }

        if (loaded.pokemonXpMultiplierEnabled == null) {
            loaded.pokemonXpMultiplierEnabled = false;
            loaded.pokemonXpMultiplier = defaults.pokemonXpMultiplier;
            changed = true;
        }
        if (loaded.pokemonXpMultiplier == 0) {
            loaded.pokemonXpMultiplier = defaults.pokemonXpMultiplier;
            changed = true;
        }
        if (loaded.shinyMultiplierEnabled == null) {
            loaded.shinyMultiplierEnabled = false;
            loaded.shinyMultiplier = defaults.shinyMultiplier;
            changed = true;
        }
        if (loaded.shinyMultiplier == 0) {
            loaded.shinyMultiplier = defaults.shinyMultiplier;
            changed = true;
        }
        if (loaded.miningGiftEnabled == null) {
            loaded.miningGiftEnabled = false;
            loaded.miningGiftChance = defaults.miningGiftChance;
            changed = true;
        }
        if (loaded.miningGiftCommands == null) {
            loaded.miningGiftCommands = defaults.miningGiftCommands;
            changed = true;
        }

        return changed;
    }

    private static void writeConfig(Path path, ArmorSetConfig config) {
        try (Writer writer = new FileWriter(path.toFile())) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            System.err.println("[LeNeMon] Erreur écriture config : " + e.getMessage());
        }
    }
}