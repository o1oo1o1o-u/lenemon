package com.lenemon.pokedex;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.util.*;

/**
 * Charge le fichier lenemon/pokedex_rewards.json.
 * Format :
 * {
 *   "national": {
 *     "caught": { "10": { "money": 5000, "items": [...], "commands": [...], "description": "..." } },
 *     "seen":   { "50": { ... } }
 *   },
 *   "kanto": { ... }
 * }
 */
public class PokedexRewardConfig {

    public record RewardEntry(long money, List<String> items, List<String> commands, String description) {}

    /** regionId → type ("caught"/"seen") → threshold (%) → reward */
    private static final Map<String, Map<String, Map<Integer, RewardEntry>>> rewards = new HashMap<>();

    public static void load() {
        rewards.clear();
        try {
            File file = new File(FabricLoader.getInstance().getConfigDir().toFile(), "lenemon/pokedex_rewards.json");
            if (!file.exists()) {
                createDefault(file);
            }
            JsonObject root = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> regionEntry : root.entrySet()) {
                String regionId = regionEntry.getKey();
                JsonObject regionObj = regionEntry.getValue().getAsJsonObject();
                Map<String, Map<Integer, RewardEntry>> byType = new HashMap<>();
                for (Map.Entry<String, JsonElement> typeEntry : regionObj.entrySet()) {
                    String type = typeEntry.getKey(); // "caught" or "seen"
                    JsonObject thresholds = typeEntry.getValue().getAsJsonObject();
                    Map<Integer, RewardEntry> byThreshold = new TreeMap<>();
                    for (Map.Entry<String, JsonElement> tEntry : thresholds.entrySet()) {
                        int threshold = Integer.parseInt(tEntry.getKey());
                        JsonObject r = tEntry.getValue().getAsJsonObject();
                        long money = r.has("money") ? r.get("money").getAsLong() : 0L;
                        List<String> items = new ArrayList<>();
                        if (r.has("items")) for (JsonElement i : r.getAsJsonArray("items")) items.add(i.getAsString());
                        List<String> commands = new ArrayList<>();
                        if (r.has("commands")) for (JsonElement c : r.getAsJsonArray("commands")) commands.add(c.getAsString());
                        String desc = r.has("description") ? r.get("description").getAsString() : buildDefaultDesc(money, items, commands);
                        byThreshold.put(threshold, new RewardEntry(money, items, commands, desc));
                    }
                    byType.put(type, byThreshold);
                }
                rewards.put(regionId, byType);
            }
        } catch (Exception e) {
            System.err.println("[Pokedex] Erreur chargement pokedex_rewards.json : " + e.getMessage());
        }
    }

    private static String buildDefaultDesc(long money, List<String> items, List<String> commands) {
        List<String> parts = new ArrayList<>();
        if (money > 0) parts.add(money + " $");
        parts.addAll(items);
        if (!commands.isEmpty()) parts.add(commands.size() + " commande(s)");
        return String.join(", ", parts);
    }

    /** Retourne les paliers pour une région et un type ("caught"/"seen"), ou vide si absent. */
    public static Map<Integer, RewardEntry> getTiers(String regionId, String type) {
        return rewards.getOrDefault(regionId, Map.of()).getOrDefault(type, Map.of());
    }

    /** Retourne true si la config contient au moins un palier pour cette région. */
    public static boolean hasRegion(String regionId) {
        return rewards.containsKey(regionId);
    }

    public static Set<String> getRegions() {
        return rewards.keySet();
    }

    private static void createDefault(File file) throws Exception {
        file.getParentFile().mkdirs();
        String json = """
                {
                  "national": {
                    "caught": {
                      "10": { "money": 2000,  "items": [], "commands": [], "description": "2000 $" },
                      "25": { "money": 5000,  "items": ["minecraft:golden_apple"], "commands": [], "description": "5000 $ + Pomme dorée" },
                      "50": { "money": 15000, "items": [], "commands": ["/gift give Basic {player} 1"], "description": "15000 $ + Coffre Basic" },
                      "75": { "money": 30000, "items": [], "commands": ["/gift give Rare {player} 1"], "description": "30000 $ + Coffre Rare" },
                      "100": { "money": 50000, "items": ["minecraft:nether_star"], "commands": ["/gift give Legendaire {player} 1"], "description": "50000 $ + Etoile + Coffre Légendaire" }
                    },
                    "seen": {
                      "25": { "money": 1000, "items": [], "commands": [], "description": "1000 $" },
                      "50": { "money": 3000, "items": [], "commands": [], "description": "3000 $" },
                      "100": { "money": 10000, "items": [], "commands": [], "description": "10000 $" }
                    }
                  },
                  "kanto": {
                    "caught": {
                      "50":  { "money": 5000, "items": [], "commands": [], "description": "5000 $" },
                      "100": { "money": 20000, "items": ["minecraft:diamond"], "commands": [], "description": "20000 $ + Diamant" }
                    }
                  },
                  "johto": {
                    "caught": {
                      "50":  { "money": 5000, "items": [], "commands": [], "description": "5000 $" },
                      "100": { "money": 20000, "items": ["minecraft:diamond"], "commands": [], "description": "20000 $ + Diamant" }
                    }
                  },
                  "hoenn": {
                    "caught": {
                      "50":  { "money": 5000, "items": [], "commands": [], "description": "5000 $" },
                      "100": { "money": 20000, "items": ["minecraft:diamond"], "commands": [], "description": "20000 $ + Diamant" }
                    }
                  },
                  "sinnoh": {
                    "caught": {
                      "50":  { "money": 5000, "items": [], "commands": [], "description": "5000 $" },
                      "100": { "money": 20000, "items": ["minecraft:diamond"], "commands": [], "description": "20000 $ + Diamant" }
                    }
                  },
                  "unova": {
                    "caught": {
                      "50":  { "money": 5000, "items": [], "commands": [], "description": "5000 $" },
                      "100": { "money": 20000, "items": ["minecraft:diamond"], "commands": [], "description": "20000 $ + Diamant" }
                    }
                  },
                  "kalos": {
                    "caught": {
                      "50":  { "money": 5000, "items": [], "commands": [], "description": "5000 $" },
                      "100": { "money": 20000, "items": ["minecraft:diamond"], "commands": [], "description": "20000 $ + Diamant" }
                    }
                  },
                  "alola": {
                    "caught": {
                      "50":  { "money": 5000, "items": [], "commands": [], "description": "5000 $" },
                      "100": { "money": 20000, "items": ["minecraft:diamond"], "commands": [], "description": "20000 $ + Diamant" }
                    }
                  },
                  "galar": {
                    "caught": {
                      "50":  { "money": 5000, "items": [], "commands": [], "description": "5000 $" },
                      "100": { "money": 20000, "items": ["minecraft:diamond"], "commands": [], "description": "20000 $ + Diamant" }
                    }
                  },
                  "hisui": {
                    "caught": {
                      "50":  { "money": 5000, "items": [], "commands": [], "description": "5000 $" },
                      "100": { "money": 20000, "items": ["minecraft:diamond"], "commands": [], "description": "20000 $ + Diamant" }
                    }
                  },
                  "paldea": {
                    "caught": {
                      "50":  { "money": 5000, "items": [], "commands": [], "description": "5000 $" },
                      "100": { "money": 20000, "items": ["minecraft:diamond"], "commands": [], "description": "20000 $ + Diamant" }
                    }
                  }
                }
                """;
        try (FileWriter w = new FileWriter(file)) { w.write(json); }
    }
}
