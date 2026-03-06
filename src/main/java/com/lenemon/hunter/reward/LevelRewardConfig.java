package com.lenemon.hunter.reward;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The type Level reward config.
 */
public class LevelRewardConfig {

    /**
     * The type Level reward.
     */
    public record LevelReward(int level, long money, List<String> items, List<String> commands, String message) {}

    private static final Map<Integer, LevelReward> rewards = new HashMap<>();

    /**
     * Load.
     */
    public static void load() {
        try {
            File file = new File(FabricLoader.getInstance().getConfigDir().toFile(), "lenemon/hunter_level_rewards.json");
            if (!file.exists()) createDefault(file);

            JsonObject root = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
            rewards.clear();

            for (JsonElement el : root.getAsJsonArray("levelRewards")) {
                JsonObject obj = el.getAsJsonObject();
                int level      = obj.get("level").getAsInt();
                long money     = obj.get("money").getAsLong();
                String message = obj.get("message").getAsString();
                List<String> items = new ArrayList<>();
                for (JsonElement item : obj.getAsJsonArray("items")) {
                    items.add(item.getAsString());
                }
                List<String> commands = new ArrayList<>();
                if (obj.has("commands")) {
                    for (JsonElement cmd : obj.getAsJsonArray("commands")) {
                        commands.add(cmd.getAsString());
                    }
                }
                rewards.put(level, new LevelReward(level, money, items, commands, message));
            }
        } catch (Exception e) {
            System.err.println("[Hunter] Erreur chargement hunter_level_rewards.json : " + e.getMessage());
        }
    }

    /**
     * Gets reward.
     *
     * @param level the level
     * @return the reward
     */
    public static LevelReward getReward(int level) {
        return rewards.get(level);
    }

    private static void createDefault(File file) throws Exception {
        file.getParentFile().mkdirs();
        String json = """
                {
                  "levelRewards": [
                    {
                      "level": 10,
                      "money": 5000,
                      "items": ["minecraft:golden_apple"],
                      "commands": [],
                      "message": "§6Niveau 10 atteint ! Félicitations !"
                    },
                    {
                      "level": 25,
                      "money": 15000,
                      "items": [],
                      "commands": [
                        "/gift give Basic {player} 1"
                      ],
                      "message": "§aNiveau 25 ! Tu progresses bien !"
                    },
                    {
                      "level": 50,
                      "money": 30000,
                      "items": ["minecraft:diamond"],
                      "commands": [
                        "/gift give Epique {player} 1",
                        "/say {player} vient d'atteindre le niveau 50 !"
                      ],
                      "message": "§bNiveau 50 ! Chasseur confirmé !"
                    }
                  ]
                }
        """;
        try (FileWriter w = new FileWriter(file)) { w.write(json); }
    }
}