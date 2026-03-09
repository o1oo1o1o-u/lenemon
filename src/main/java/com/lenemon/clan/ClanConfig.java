package com.lenemon.clan;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Configuration du systeme de clan.
 * Chargee depuis config/lenemon/clan.json au demarrage du serveur.
 * Les valeurs par defaut sont generees automatiquement si le fichier est absent.
 */
public class ClanConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("lenemon/clan");

    // Singleton charge
    private static ClanConfig instance = new ClanConfig();

    // --- Limites ---
    /** Nombre maximum de membres par clan (niveau 1). Augmente par palier de niveau. */
    public int maxMembersBase = 10;
    /** Membres supplementaires par niveau de clan (ex: +5 par niveau). */
    public int maxMembersPerLevel = 5;

    // --- XP par action ---
    public long xpPerHunterQuest = 50L;
    public long xpPerPokemonCatch = 5L;
    public long xpPerShinyCatch = 50L;

    // --- Paliers de niveau ---
    /** XP requise pour passer au niveau N : xpBase * N^2. */
    public long xpLevelBase = 500L;

    /** Niveau maximum atteignable. */
    public int maxLevel = 10;

    // --- Bank ---
    /** Retrait maximum par membre et par jour (0 = illimite pour les officers). */
    public long bankWithdrawDailyLimitMember = 5000L;
    /** Les officers n'ont pas de limite par defaut (valeur 0 = illimite). */
    public long bankWithdrawDailyLimitOfficer = 0L;

    // --- Tag ---
    /** Longueur maximale du tag de clan. */
    public int maxTagLength = 8;

    /** Longueur maximale du nom de clan. */
    public int maxNameLength = 24;

    // --- Claims ---
    /** Nombre de chunks de base claimables (niveau economique 1). */
    public int baseChunks = 10;
    /** Multiplicateur de chunks par niveau (formule triangulaire). */
    public int chunksPerLevelMultiplier = 5;
    /** Distance buffer minimum entre deux territoires de clans differents (en chunks, Chebyshev). */
    public int claimBufferDistance = 10;
    /** Dimension autorisee pour les claims. */
    public String claimAllowedDimension = "minecraft:overworld";
    /** Cooldown anti-spam des messages d'entree/sortie de territoire (ms). */
    public long territoryMessageCooldownMs = 3000L;

    // --- Level economique ---
    /** Prix de base pour acheter le level 1 → 2 (en coins de banque du clan). */
    public long baseLevelPrice = 1_000_000L;
    /** Multiplicateur de prix par niveau (le prix double a chaque niveau par defaut). */
    public double levelPriceMultiplier = 1.35;
    /** Niveau economique maximum atteignable. */
    public int maxClanLevel = 20;

    public static ClanConfig get() {
        return instance;
    }

    /**
     * Charge la config depuis config/lenemon/clan.json.
     * Cree le fichier avec les valeurs par defaut s'il est absent.
     */
    public static void load() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("lenemon");
        File file = configDir.resolve("clan.json").toFile();

        if (!file.exists()) {
            createDefault(file);
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            instance = gson.fromJson(reader, ClanConfig.class);
            // Securite : si le fichier est mal forme, on garde les defaults
            if (instance == null) instance = new ClanConfig();
            LOGGER.info("[Clan] Config chargee depuis {}", file.getPath());
        } catch (Exception e) {
            LOGGER.error("[Clan] Erreur chargement config, defaults utilises : {}", e.getMessage());
            instance = new ClanConfig();
        }
    }

    private static void createDefault(File file) {
        try {
            file.getParentFile().mkdirs();
            ClanConfig defaults = new ClanConfig();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter w = new FileWriter(file, StandardCharsets.UTF_8)) {
                w.write(gson.toJson(defaults));
            }
            LOGGER.info("[Clan] Config par defaut creee : {}", file.getPath());
        } catch (Exception e) {
            LOGGER.error("[Clan] Impossible de creer la config par defaut : {}", e.getMessage());
        }
    }

    /** Calcule l'XP requise pour atteindre un niveau donne. */
    public long xpRequiredForLevel(int level) {
        return xpLevelBase * level * level;
    }

    /** Calcule le nombre maximum de membres pour un niveau de clan donne. */
    public int maxMembersForLevel(int level) {
        return maxMembersBase + maxMembersPerLevel * (level - 1);
    }

    /**
     * Calcule le prix pour acheter le prochain level economique.
     * Formule : baseLevelPrice * levelPriceMultiplier^(currentLevel - 1)
     * Exemples (base=1_000_000, mult=2.0) :
     *   L1-&gt;L2 = 1 000 000, L2-&gt;L3 = 2 000 000, L3-&gt;L4 = 4 000 000
     */
    public long levelUpPrice(int currentLevel) {
        return (long)(baseLevelPrice * Math.pow(levelPriceMultiplier, currentLevel - 1));
    }

    /**
     * Calcule le nombre maximum de chunks claimables pour un niveau economique donne.
     * Identique a Clan.maxClaims() mais utilisable sans instance de Clan.
     */
    public int maxClaims(int clanLevel) {
        if (clanLevel <= 1) return baseChunks;
        return baseChunks + chunksPerLevelMultiplier * (clanLevel * (clanLevel + 1) / 2 - 1);
    }
}
