package com.lenemon.pickaxe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

/**
 * The type Excaveon config loader.
 */
public class ExcaveonConfigLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ExcaveonConfig cached;

    // AJOUT
    private static volatile ExcaveonConfig serverSynced;

    /**
     * Sets server config.
     *
     * @param cfg the cfg
     */
// AJOUT
    public static void setServerConfig(ExcaveonConfig cfg) {
        serverSynced = cfg;
    }

    /**
     * Gets effective.
     *
     * @return the effective
     */
// AJOUT
    public static ExcaveonConfig getEffective() {
        return serverSynced != null ? serverSynced : get();
    }

    /**
     * Load excaveon config.
     *
     * @return the excaveon config
     */
    public static ExcaveonConfig load() {
        Path configPath = FabricLoader.getInstance()
                .getConfigDir().resolve("lenemon").resolve("excaveon.json");
        configPath.getParent().toFile().mkdirs();

        if (!configPath.toFile().exists()) {
            ExcaveonConfig defaults = new ExcaveonConfig();
            try (Writer w = new FileWriter(configPath.toFile())) {
                GSON.toJson(defaults, w);
            } catch (IOException e) {
                System.err.println("[LeNeMon] Erreur création excaveon.json : " + e.getMessage());
            }
            cached = defaults;
            return defaults;
        }

        try (Reader r = new FileReader(configPath.toFile())) {
            cached = GSON.fromJson(r, ExcaveonConfig.class);
            return cached;
        } catch (IOException e) {
            System.err.println("[LeNeMon] Erreur lecture excaveon.json : " + e.getMessage());
            cached = new ExcaveonConfig();
            return cached;
        }
    }

    /**
     * Get excaveon config.
     *
     * @return the excaveon config
     */
    public static ExcaveonConfig get() {
        if (cached == null) return load();
        return cached;
    }

    /**
     * Reload.
     */
    public static void reload() { cached = null; load(); }
}