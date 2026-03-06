package com.lenemon.hunter.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * The type Quest config loader.
 */
public class QuestConfigLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static List<Quest> quests = new ArrayList<>();

    /**
     * Load.
     */
    public static void load() {
        try {
            File file = new File(FabricLoader.getInstance().getConfigDir().toFile(), "lenemon/hunter_quests.json");
            if (!file.exists()) {
                createDefault(file);
            }

            JsonObject root = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
            JsonArray array = root.getAsJsonArray("quests");

            quests.clear();
            for (JsonElement el : array) {
                JsonObject obj = el.getAsJsonObject();
                Quest q = new Quest();
                q.id         = obj.get("id").getAsString();
                q.type       = QuestType.valueOf(obj.get("type").getAsString());
                q.difficulty = QuestDifficulty.valueOf(obj.get("difficulty").getAsString());
                q.target     = obj.get("target").getAsString();
                q.amount     = obj.get("amount").getAsInt();
                q.xpReward   = obj.get("xpReward").getAsLong();
                q.moneyReward = obj.get("moneyReward").getAsLong();
                q.itemRewards = new ArrayList<>();
                for (JsonElement item : obj.getAsJsonArray("itemRewards")) {
                    q.itemRewards.add(item.getAsString());
                }
                q.commands = new ArrayList<>();
                if (obj.has("commands")) {
                    for (JsonElement cmd : obj.getAsJsonArray("commands")) {
                        q.commands.add(cmd.getAsString());
                    }
                }
                q.commandsLabel = obj.has("commandsLabel") ? obj.get("commandsLabel").getAsString() : "";
                quests.add(q);
            }
            if (root.has("settings")) {
                JsonObject s = root.getAsJsonObject("settings");
                HunterSettings.xpPerKill           = s.get("xpPerKill").getAsLong();
                HunterSettings.xpPerCapture        = s.get("xpPerCapture").getAsLong();
                HunterSettings.shinyMultiplier     = s.get("shinyMultiplier").getAsDouble();
                HunterSettings.legendaryMultiplier = s.get("legendaryMultiplier").getAsDouble();
            }

        } catch (Exception e) {
            System.err.println("[Hunter] Erreur chargement hunter_quests.json : " + e.getMessage());
        }
    }

    /**
     * Gets by difficulty.
     *
     * @param difficulty the difficulty
     * @return the by difficulty
     */
    public static List<Quest> getByDifficulty(QuestDifficulty difficulty) {
        return quests.stream().filter(q -> q.difficulty == difficulty).toList();
    }

    /**
     * Gets all.
     *
     * @return the all
     */
    public static List<Quest> getAll() {
        return quests;
    }

    private static void createDefault(File file) throws Exception {
        file.getParentFile().mkdirs();
        String json = """
        {
        "settings": {
            "xpPerKill": 10,
            "xpPerCapture": 20,
            "shinyMultiplier": 3.0,
            "legendaryMultiplier": 5.0
          },
          "quests": [
            {"id":"kill_any_001","type":"KILL","difficulty":"EASY","target":"*","amount":10,"xpReward":50,"moneyReward":500,"itemRewards":[],"commands": ["/gift give Basic {player} 1"], "commandsLabel": "Bon cadeau BASIC"},
            {"id":"kill_any_002","type":"KILL","difficulty":"EASY","target":"*","amount":20,"xpReward":80,"moneyReward":700,"itemRewards":[],"commands": ["/gift give Basic {player} 1"], "commandsLabel": "Bon cadeau BASIC"},
            {"id":"capture_any_001","type":"CAPTURE","difficulty":"EASY","target":"*","amount":5,"xpReward":80,"moneyReward":800,"itemRewards":[],"commands": ["/gift give Basic {player} 1"], "commandsLabel": "Bon cadeau BASIC"},
            {"id":"capture_type_fire","type":"CAPTURE_TYPE","difficulty":"MEDIUM","target":"fire","amount":3,"xpReward":150,"moneyReward":1500,"itemRewards":["minecraft:golden_apple"],"commands": ["/gift give Epique {player} 1"], "commandsLabel": "Bon cadeau EPIQUE"},
            {"id":"capture_type_water","type":"CAPTURE_TYPE","difficulty":"MEDIUM","target":"water","amount":3,"xpReward":150,"moneyReward":1500,"itemRewards":["minecraft:golden_apple"],"commands": ["/gift give Epique {player} 1"], "commandsLabel": "Bon cadeau EPIQUE"},
            {"id":"kill_type_grass","type":"KILL_TYPE","difficulty":"MEDIUM","target":"grass","amount":5,"xpReward":120,"moneyReward":1200,"itemRewards":[],"commands": ["/gift give Epique {player} 1"], "commandsLabel": "Bon cadeau EPIQUE"},
            {"id":"capture_shiny_001","type":"CAPTURE_SHINY","difficulty":"HARD","target":"*","amount":1,"xpReward":500,"moneyReward":5000,"itemRewards":["minecraft:diamond"],"commands": ["/gift give Légendaire {player} 1"], "commandsLabel": "Bon cadeau LEGENDAIRE"},
            {"id":"capture_legendary_001","type":"CAPTURE_LEGENDARY","difficulty":"HARD","target":"*","amount":1,"xpReward":800,"moneyReward":8000,"itemRewards":["minecraft:nether_star"],"commands": ["/gift give Légendaire {player} 1"], "commandsLabel": "Bon cadeau LEGENDAIRE"}
          ]
        }
        """;
        try (FileWriter w = new FileWriter(file)) { w.write(json); }
    }
}