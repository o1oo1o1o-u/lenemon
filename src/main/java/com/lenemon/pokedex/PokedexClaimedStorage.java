package com.lenemon.pokedex;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Stocke les récompenses Pokédex déjà récupérées par chaque joueur.
 * Fichier : config/lenemon/pokedex_claimed.json
 * Format : { "uuid": ["kanto_caught_50", "national_seen_25", ...] }
 */
public class PokedexClaimedStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, Set<String>> claimed = new HashMap<>();
    private static boolean loaded = false;

    public static void load() {
        claimed.clear();
        File file = getFile();
        if (!file.exists()) { loaded = true; return; }
        try (Reader r = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                UUID uuid = UUID.fromString(e.getKey());
                Set<String> keys = new LinkedHashSet<>();
                for (JsonElement el : e.getValue().getAsJsonArray()) keys.add(el.getAsString());
                claimed.put(uuid, keys);
            }
        } catch (Exception e) {
            System.err.println("[Pokedex] Erreur chargement pokedex_claimed.json : " + e.getMessage());
        }
        loaded = true;
    }

    public static boolean isClaimed(UUID uuid, String regionId, String type, int threshold) {
        if (!loaded) load();
        return claimed.getOrDefault(uuid, Set.of()).contains(key(regionId, type, threshold));
    }

    public static void markClaimed(UUID uuid, String regionId, String type, int threshold) {
        if (!loaded) load();
        claimed.computeIfAbsent(uuid, k -> new LinkedHashSet<>()).add(key(regionId, type, threshold));
        save();
    }

    /** Remet à zéro toutes les récompenses d'une région pour un joueur. Retourne le nombre supprimé. */
    public static int resetRegion(UUID uuid, String regionId) {
        if (!loaded) load();
        Set<String> keys = claimed.get(uuid);
        if (keys == null || keys.isEmpty()) return 0;
        String prefix = regionId + "_";
        int before = keys.size();
        keys.removeIf(k -> k.startsWith(prefix));
        int removed = before - keys.size();
        if (removed > 0) save();
        return removed;
    }

    /** Remet à zéro toutes les récompenses Pokédex d'un joueur. Retourne le nombre supprimé. */
    public static int resetAll(UUID uuid) {
        if (!loaded) load();
        Set<String> keys = claimed.remove(uuid);
        int removed = keys != null ? keys.size() : 0;
        if (removed > 0) save();
        return removed;
    }

    private static String key(String regionId, String type, int threshold) {
        return regionId + "_" + type + "_" + threshold;
    }

    private static void save() {
        try {
            File file = getFile();
            file.getParentFile().mkdirs();
            JsonObject root = new JsonObject();
            for (Map.Entry<UUID, Set<String>> e : claimed.entrySet()) {
                JsonArray arr = new JsonArray();
                e.getValue().forEach(arr::add);
                root.add(e.getKey().toString(), arr);
            }
            try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
        } catch (Exception e) {
            System.err.println("[Pokedex] Erreur sauvegarde pokedex_claimed.json : " + e.getMessage());
        }
    }

    private static File getFile() {
        return new File(FabricLoader.getInstance().getConfigDir().toFile(), "lenemon/pokedex_claimed.json");
    }
}
