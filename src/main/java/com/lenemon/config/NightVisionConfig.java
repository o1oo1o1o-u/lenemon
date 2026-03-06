package com.lenemon.config;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * The type Night vision config.
 */
public class NightVisionConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Integer> durations = new HashMap<>();
    private static File configFile;

    /**
     * Load.
     *
     * @param server the server
     */
    public static void load(MinecraftServer server) {
        configFile = server.getRunDirectory().resolve("config/custommenu_nv.json").toFile();
        configFile.getParentFile().mkdirs();

        if (!configFile.exists()) {
            // Valeurs par défaut
            durations.put("default", 300);
            save();
            return;
        }

        try (Reader reader = new InputStreamReader(
                new FileInputStream(configFile), StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            durations.clear();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                durations.put(entry.getKey(), entry.getValue().getAsInt());
            }
        } catch (Exception e) {
            System.err.println("[NV] Erreur chargement config : " + e.getMessage());
        }
    }

    /**
     * Save.
     */
    public static void save() {
        try {
            JsonObject json = new JsonObject();
            durations.forEach(json::addProperty);
            try (Writer writer = new OutputStreamWriter(
                    new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception e) {
            System.err.println("[NV] Erreur sauvegarde config : " + e.getMessage());
        }
    }

    /**
     * Set.
     *
     * @param grade   the grade
     * @param seconds the seconds
     */
    public static void set(String grade, int seconds) {
        durations.put(grade, seconds);
        save();
    }

    /**
     * Remove.
     *
     * @param grade the grade
     */
    public static void remove(String grade) {
        durations.remove(grade);
        save();
    }

    /**
     * Gets all.
     *
     * @return the all
     */
    public static Map<String, Integer> getAll() {
        return durations;
    }

    /**
     * Gets best duration.
     *
     * @param playerGrades the player grades
     * @return the best duration
     */
// Retourne la durée la plus longue parmi les grades du joueur
    // -1 = permanent
    public static int getBestDuration(java.util.Set<String> playerGrades) {
        int best = -2; // -2 = aucun grade trouvé
        for (String grade : playerGrades) {
            if (durations.containsKey(grade)) {
                int val = durations.get(grade);
                if (val == -1) return -1; // permanent → on prend direct
                if (val > best) best = val;
            }
        }
        // Vérifie aussi "default"
        if (best == -2 && durations.containsKey("default")) {
            best = durations.get("default");
        }
        return best;
    }
}